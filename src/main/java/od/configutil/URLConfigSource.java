package od.configutil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 20-Nov-2010
 * Time: 09:43:06
 *
 * Load a config from a URL which is determined from the context URL provided
 * and the file name, or if no context is provided the filename is expected to be a complete url
 */
public class URLConfigSource extends AbstractConfigSource {

    private URL context;
    private int timeout = -1;
    private Executor timeoutExecutor;

    public URLConfigSource(String encoding) {
        this(null, encoding);
    }

    public URLConfigSource() {
        this(null, "UTF-8");
    }

    public URLConfigSource(int timeoutMillis) {
        this(null, "UTF-8");
        this.timeout = timeoutMillis;
    }

    public URLConfigSource(URL context) {
        this(context, "UTF-8");
    }
    
    public URLConfigSource(URL context, String encoding) {
        super(encoding);
        this.context = context;
    }

    /**
     * Set a timeout in milliseconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    protected void loadStarting(String configName) throws ConfigManagerException {
        LogMethods.log.info("Loading config " + configName + " from context");
    }

    @Override
    protected InputStream getInputStream(String configFileName) throws Exception {
        final URL url = new URL(context, configFileName);

        InputStream result;
        if ( timeout != -1 ) {
            result = openInputStreamWithTimeout(url);
        } else {
            result = url.openStream();
        }

        if ( result == null ) {
            throw new ConfigManagerException("Could not get an input stream from config source URL " + url);
        }
        return result;
    }

    //a mechanism to guarantee we only wait a certain amount of time, even if
    //the network layer takes longer
    private InputStream openInputStreamWithTimeout(final URL url) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        class InputStreamRunnable implements Runnable {
            volatile InputStream stream;

            public void run() {
                try {
                    stream = url.openStream();
                } catch (IOException e) {
                    LogMethods.log.error("Error opening input stream from URL " + url, e);
                }
                latch.countDown();
            }
        }

        InputStreamRunnable isr = new InputStreamRunnable();
        getTimeoutExecutor().execute(isr);
        latch.await(timeout, TimeUnit.MILLISECONDS);
        return isr.stream;
    }

    private Executor getTimeoutExecutor() {
        if ( timeoutExecutor == null ) {
            timeoutExecutor = Executors.newSingleThreadExecutor();
        }
        return timeoutExecutor;
    }

    @Override
    protected String getConfigFileName(String configName, long version) {
        return getUrlSpec(configName);
    }

    protected String getUrlSpec(String configName) {
        return configName;
    }
}
