package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class HTTPStreamFactoryTest extends BaseTest {

    private static final Identifier PRESENT_READABLE_IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private WebServer server;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        server = new WebServer();
        server.setHTTP1Enabled(true);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    private HTTPStreamFactory newInstance() {
        return newInstance(true);
    }

    private HTTPStreamFactory newInstance(boolean serverAcceptsRanges) {
        Map<String,String> headers = new HashMap<>();
        headers.put("X-Custom", "yes");
        HttpSource.RequestInfo requestInfo = new HttpSource.RequestInfo(
                server.getHTTPURI().resolve("/" + PRESENT_READABLE_IDENTIFIER).toString(),
                null, null, headers);

        return new HTTPStreamFactory(
                HttpSource.getHTTPClient(requestInfo),
                requestInfo,
                5439,
                serverAcceptsRanges);
    }

    @Test
    public void testNewImageInputStreamWhenServerAcceptsRanges()
            throws Exception {
        server.start();
        try (ImageInputStream is = newInstance(true).newImageInputStream()) {
            assertTrue(is instanceof HTTPImageInputStream);
        }
    }

    @Test
    public void testNewImageInputStreamWhenServerDoesNotAcceptRanges()
            throws Exception {
        server.setAcceptingRanges(false);
        server.start();
        try (ImageInputStream is = newInstance(false).newImageInputStream()) {
            assertFalse(is instanceof HTTPImageInputStream);
        }
    }

    @Test
    public void testNewImageInputStreamSendsCustomHeaders() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                assertEquals("yes", request.getHeader("X-Custom"));
                baseRequest.setHandled(true);
            }
        });
        server.start();

        try (ImageInputStream is = newInstance().newImageInputStream()) {}
    }

    @Test
    public void testNewImageInputStreamReturnsContent() throws Exception {
        server.start();

        int length = 0;
        try (ImageInputStream is = newInstance().newImageInputStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(5439, length);
    }

    @Test
    public void testNewInputStreamSendsCustomHeaders() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                assertEquals("yes", request.getHeader("X-Custom"));
                baseRequest.setHandled(true);
            }
        });
        server.start();

        try (InputStream is = newInstance().newInputStream()) {}
    }

    @Test
    public void testNewInputStreamReturnsContent() throws Exception {
        server.start();

        int length = 0;
        try (InputStream is = newInstance().newInputStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(5439, length);
    }

}