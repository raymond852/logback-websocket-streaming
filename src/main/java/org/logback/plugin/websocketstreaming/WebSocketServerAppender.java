package org.logback.plugin.websocketstreaming;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.layout.EchoLayout;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author raymonddu 14/6/2018
 */
public class WebSocketServerAppender<E> extends UnsynchronizedAppenderBase<E> implements Runnable {

  public static final int DEFAULT_PORT = 3450;

  private final Executor logWorker = Executors.newSingleThreadExecutor();

  private final LinkedBlockingDeque<E> logEventQueue = new LinkedBlockingDeque<>();

  private Layout<E> layout = new EchoLayout<>();

  private int port = DEFAULT_PORT;

  private Server server;

  @Override
  protected void append(E event) {
    if (!isStarted()) {
      return;
    }
    logEventQueue.offer(event);
    logWorker.execute(this);
  }

  @Override
  public void stop() {
    if (server != null) {
      server.stop();
    }
    super.stop();
  }

  @Override
  public void start() {
    if (isStarted()) {
      return;
    }
    server = new Server(port);
    server.start();
    super.start();
  }

  @Override
  public void run() {
    E event = logEventQueue.poll();
    if (event instanceof DeferredProcessingAware) {
      ((DeferredProcessingAware) event).prepareForDeferredProcessing();
    }

    String logTxt = layout.doLayout(event);
    server.broadcast(logTxt);
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setLayout(Layout<E> layout) {
    this.layout = layout;
  }
}
