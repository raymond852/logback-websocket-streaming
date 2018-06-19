import ch.qos.logback.classic.LoggerContext;
import io.undertow.server.XnioByteBufferPool;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.Xnio;
import org.xnio.XnioWorker;


/**
 * @author raymonddu 14/6/2018
 */
public class WebSocketServerAppenderTest {

  private Logger logger;

  @BeforeClass
  public void setup() throws InterruptedException {
    logger = LoggerFactory.getLogger(this.getClass());
    Thread.sleep(1000);
  }

  @AfterClass
  public void teardown() {
    if (LoggerFactory.getILoggerFactory() instanceof LoggerContext) {
      ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
    }
  }

  @Test
  public void testCanAppend()
      throws InterruptedException, ExecutionException, TimeoutException, IOException, URISyntaxException {
    XnioWorker worker = Xnio.getInstance().createWorker(OptionMap.builder()
        .set(Options.WORKER_IO_THREADS, 1)
        .set(Options.WORKER_TASK_CORE_THREADS, 1)
        .set(Options.WORKER_TASK_MAX_THREADS, 2)
        .set(Options.TCP_NODELAY, true)
        .getMap());
    Pool<ByteBuffer> buffers = new ByteBufferSlicePool(BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR,
        1024, 1024);

    WebSocketChannel channel = WebSocketClient
        .connectionBuilder(worker, new XnioByteBufferPool(buffers),
            new URI("http://localhost:3450/ws")).connect().get();
    CompletableFuture<String> messageFuture = new CompletableFuture<>();
    channel.getReceiveSetter().set(new AbstractReceiveListener() {

      @Override
      protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
        final String messageData = message.getData();
        messageFuture.complete(messageData);
      }
    });
    channel.resumeReceives();
    String logMsg = UUID.randomUUID().toString();
    logger.debug(logMsg);
    String actualResult = messageFuture.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(actualResult.contains(logMsg));
  }

  @Test(dataProvider = "file_url_provider")
  public void testGetFile(String url) throws IOException {
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = new OkHttpClient().newCall(request).execute();
    byte[] fileBytes = readBytes(getClass().getClassLoader().getResourceAsStream("index.html"));
    Assert.assertNotNull(fileBytes);
    Assert.assertTrue(Arrays.equals(response.body().bytes(), fileBytes));
  }

  @Test
  public void testGetFileNotFound() throws IOException {
    String url = "http://localhost:3450/notfound";
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = new OkHttpClient().newCall(request).execute();
    Assert.assertEquals(response.code(), 404);
  }

  @DataProvider(name = "file_url_provider")
  public Object[][] provideFileUrl() {
    return new Object[][]{
        {"http://localhost:3450/"},
        {"http://localhost:3450/index"},
        {"http://localhost:3450/index.html"}
    };
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
