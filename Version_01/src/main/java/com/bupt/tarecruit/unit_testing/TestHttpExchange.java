package com.bupt.tarecruit.unit_testing;

import com.sun.net.httpserver.Headers;
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

class TestHttpExchange extends HttpExchange {
    private final String method;
    private final URI uri;
    private final Headers requestHeaders = new Headers();
    private final Headers responseHeaders = new Headers();
    private final InputStream requestBody;
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private int responseCode;
    private long responseLength;

    TestHttpExchange(String method, String path, String query, String body) {
        this.method = method;
        String full = query == null || query.isEmpty() ? path : path + "?" + query;
        this.uri = URI.create(full);
        this.requestBody = new ByteArrayInputStream(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
    }

    void setBearerToken(String token) {
        requestHeaders.set("Authorization", "Bearer " + token);
    }

    String getResponseBodyAsString() {
        return responseBody.toString(StandardCharsets.UTF_8);
    }

    long getRecordedResponseLength() {
        return responseLength;
    }

    @Override public Headers getRequestHeaders() { return requestHeaders; }
    @Override public Headers getResponseHeaders() { return responseHeaders; }
    @Override public URI getRequestURI() { return uri; }
    @Override public String getRequestMethod() { return method; }
    @Override public com.sun.net.httpserver.HttpContext getHttpContext() { return null; }
    @Override public void close() {}
    @Override public InputStream getRequestBody() { return requestBody; }
    @Override public OutputStream getResponseBody() { return responseBody; }
    @Override public void sendResponseHeaders(int rCode, long responseLength) {
        this.responseCode = rCode;
        this.responseLength = responseLength;
    }
    @Override public InetSocketAddress getRemoteAddress() { return null; }
    @Override public int getResponseCode() { return responseCode; }
    @Override public InetSocketAddress getLocalAddress() { return null; }
    @Override public String getProtocol() { return "HTTP/1.1"; }
    @Override public Object getAttribute(String name) { return null; }
    @Override public void setAttribute(String name, Object value) {}
    @Override public void setStreams(InputStream i, OutputStream o) {}
    @Override public HttpPrincipal getPrincipal() { return null; }
}
