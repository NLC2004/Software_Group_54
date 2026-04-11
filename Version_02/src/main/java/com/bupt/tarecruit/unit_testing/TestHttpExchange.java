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

public class TestHttpExchange extends HttpExchange {
    private final Headers requestHeaders = new Headers();
    private final Headers responseHeaders = new Headers();
    private final String method;
    private final URI uri;
    private final InputStream requestBody;
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private int responseCode = -1;
    private long responseLength = -1;

    public TestHttpExchange(String method, String path, String query, String requestBody) {
        this.method = method;
        String full = path + (query == null || query.isBlank() ? "" : "?" + query);
        this.uri = URI.create(full);
        this.requestBody = new ByteArrayInputStream((requestBody == null ? "" : requestBody).getBytes(StandardCharsets.UTF_8));
    }

    public void setBearerToken(String token) {
        requestHeaders.set("Authorization", "Bearer " + token);
    }

    public String getResponseBodyAsString() {
        return responseBody.toString(StandardCharsets.UTF_8);
    }

    public long getRecordedResponseLength() {
        return responseLength;
    }

    @Override
    public Headers getRequestHeaders() {
        return requestHeaders;
    }

    @Override
    public Headers getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public URI getRequestURI() {
        return uri;
    }

    @Override
    public String getRequestMethod() {
        return method;
    }

    @Override
    public HttpContext getHttpContext() {
        return null;
    }

    @Override
    public void close() {
        try {
            requestBody.close();
            responseBody.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public InputStream getRequestBody() {
        return requestBody;
    }

    @Override
    public OutputStream getResponseBody() {
        return responseBody;
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength) {
        this.responseCode = rCode;
        this.responseLength = responseLength;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return new InetSocketAddress(0);
    }

    @Override
    public int getResponseCode() {
        return responseCode;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return new InetSocketAddress(0);
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public void setAttribute(String name, Object value) {
    }

    @Override
    public void setStreams(InputStream i, OutputStream o) {
    }

    @Override
    public HttpPrincipal getPrincipal() {
        return null;
    }
}
