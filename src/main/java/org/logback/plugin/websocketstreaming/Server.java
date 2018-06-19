package org.logback.plugin.websocketstreaming;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;

import ch.qos.logback.core.spi.LifeCycle;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author raymonddu 15/6/2018
 */
class Server implements LifeCycle {

  private final Undertow server;

  private final List<WebSocketChannel> connections = new CopyOnWriteArrayList<>();

  private boolean isStarted = false;

  Server(int port) {
    server = Undertow.builder().addHttpListener(port, "0.0.0.0")
        .setHandler(path()
            .addExactPath("/ws", websocket((exchange, channel) -> {
              connections.add(channel);
              channel.addCloseTask((task) -> {
                connections.remove(channel);
              });
            }))
            .addPrefixPath("/", new StaticFileHandler())
        ).build();
  }

  public void broadcast(String text) {
    connections.forEach(
        webSocketChannel -> WebSockets.sendText(text, webSocketChannel, null));
  }

  @Override
  public void start() {
    if (isStarted()) {
      return;
    }
    isStarted = true;
    server.start();
  }

  @Override
  public void stop() {
    server.stop();
    isStarted = false;
  }

  @Override
  public boolean isStarted() {
    return isStarted;
  }

  static class StaticFileHandler implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange httpExchange) throws Exception {
      String targetRes = "index.html";
      String path = httpExchange.getRequestPath();
      if (path.length() > 1 && !path.equals("/index")) {
        targetRes = path.substring(1);
      }
      InputStream in = getClass().getClassLoader().getResourceAsStream(targetRes);
      if (in == null) {
        httpExchange.setStatusCode(404);
        httpExchange.endExchange();
        return;
      }

      byte[] fileContent = readBytes(in);
      httpExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
      httpExchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, fileContent.length);
      httpExchange.setStatusCode(200);
      httpExchange.getResponseSender().send(ByteBuffer.wrap(fileContent));
    }

    private byte[] readBytes(InputStream stream) throws IOException {
      byte[] buffer = new byte[1024];
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      int numRead;
      while ((numRead = stream.read(buffer)) > -1) {
        output.write(buffer, 0, numRead);
      }
      output.flush();
      return output.toByteArray();
    }
  }

}
