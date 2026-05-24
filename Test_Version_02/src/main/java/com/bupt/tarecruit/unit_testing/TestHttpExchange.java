package com.bupt.tarecruit.unit_testing;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * In-memory {@link HttpExchange} implementation used by handler unit tests.
 *
 * <p>The production application receives requests from Java's HTTP server. The
 * test suite instead constructs this exchange with a method, URI, query string
 * and JSON body, invokes a real handler, and then inspects the captured status,
 * headers and response bytes. This keeps tests fast and isolated while still
 * exercising the application's HTTP-facing behavior.</p>
 */
public class TestHttpExchange extends HttpExchange {
    private final Headers requestHeaders = new Headers();
    private final Headers responseHeaders = new Headers();
    private final String method;
    private final URI uri;
    private final InputStream requestBody;
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private int responseCode = -1;
    private long responseLength = -1;

    /**
     * Creates a request exchange whose input body is held in memory.
     *
     * @param method HTTP method delivered to the handler, such as GET or POST
     * @param path absolute endpoint path used by handler route matching
     * @param query optional query text without a leading question mark
     * @param requestBody optional UTF-8 request body, normally JSON
     */
    public TestHttpExchange(String method, String path, String query, String requestBody) {
        this.method = method;
        String full = path + (query == null || query.isBlank() ? "" : "?" + query);
        this.uri = URI.create(full);
        this.requestBody = new ByteArrayInputStream((requestBody == null ? "" : requestBody).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Adds the authorization header used by authenticated endpoint tests.
     *
     * @param token session token previously issued or constructed by DataService
     */
    public void setBearerToken(String token) {
        requestHeaders.set("Authorization", "Bearer " + token);
    }

    /**
     * Returns the bytes written by the handler decoded as UTF-8 text.
     *
     * @return captured textual response body for JSON and message assertions
     */
    public String getResponseBodyAsString() {
        return responseBody.toString(StandardCharsets.UTF_8);
    }

    /**
     * Returns the response length passed to {@link #sendResponseHeaders(int, long)}.
     *
     * @return recorded length, or {@code -1} before a response is sent
     */
    public long getRecordedResponseLength() {
        return responseLength;
    }

    /**
     * Provides request headers configured by the test, including bearer tokens.
     */
    @Override
    public Headers getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * Provides response headers populated by the handler under test.
     */
    @Override
    public Headers getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Provides the complete request URI, including any query parameters.
     */
    @Override
    public URI getRequestURI() {
        return uri;
    }

    /**
     * Provides the HTTP verb specified when the simulated exchange was built.
     */
    @Override
    public String getRequestMethod() {
        return method;
    }

    /**
     * No server context is required for direct handler invocation in tests.
     */
    @Override
    public HttpContext getHttpContext() {
        return null;
    }

    /**
     * Releases the in-memory request and response streams after a test request.
     */
    @Override
    public void close() {
        try {
            requestBody.close();
            responseBody.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Supplies the handler with the prepared UTF-8 request-body stream.
     */
    @Override
    public InputStream getRequestBody() {
        return requestBody;
    }

    /**
     * Captures bytes written by a handler so the test can assert its response.
     */
    @Override
    public OutputStream getResponseBody() {
        return responseBody;
    }

    /**
     * Records status and declared length rather than transmitting a response.
     */
    @Override
    public void sendResponseHeaders(int rCode, long responseLength) {
        this.responseCode = rCode;
        this.responseLength = responseLength;
    }

    /**
     * Returns a neutral remote address because authorization does not depend
     * on a live socket in these unit tests.
     */
    @Override
    public InetSocketAddress getRemoteAddress() {
        return new InetSocketAddress(0);
    }

    /**
     * Returns the response status recorded when the handler sent its headers.
     */
    @Override
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Returns a neutral local address because no HTTP server is opened.
     */
    @Override
    public InetSocketAddress getLocalAddress() {
        return new InetSocketAddress(0);
    }

    /**
     * Models the protocol version expected by the simple application server.
     */
    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    /**
     * Context attributes are unused by the application's tested handlers.
     */
    @Override
    public Object getAttribute(String name) {
        return null;
    }

    /**
     * Ignores context attributes because handlers do not consult them in unit
     * scenarios.
     */
    @Override
    public void setAttribute(String name, Object value) {
    }

    /**
     * Keeps the constructor-provided in-memory streams; external stream
     * replacement is not required for handler tests.
     */
    @Override
    public void setStreams(InputStream i, OutputStream o) {
    }

    /**
     * Returns no server principal because authentication is represented by the
     * application's bearer-session header instead.
     */
    @Override
    public HttpPrincipal getPrincipal() {
        return null;
    }
}
