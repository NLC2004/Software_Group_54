package com.bupt.tarecruit.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.*;

public class StaticHandler implements HttpHandler {
    private final String baseDir;

    public StaticHandler(String baseDir) { this.baseDir = baseDir; }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        if ("/".equals(path)) {
            // Use MO login as the shared entry page for MO/Admin.
            ex.getResponseHeaders().set("Location", "/MO/index.html");
            ex.sendResponseHeaders(302, -1);
            return;
        }

        Path filePath = Paths.get(baseDir, path);
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            if (isAllowedPath(path)) {
                serve(ex, Files.readAllBytes(filePath), contentType(path));
                return;
            }
        }

        String msg = "Not Found";
        ex.getResponseHeaders().set("Content-Type", "text/plain");
        ex.sendResponseHeaders(404, msg.length());
        try (var os = ex.getResponseBody()) { os.write(msg.getBytes()); }
    }

    private boolean isAllowedPath(String path) {
        return path.startsWith("/TA/") || path.startsWith("/MO/") || path.startsWith("/admin/")
            || path.startsWith("/js/") || path.startsWith("/css/");
    }

    private void serve(HttpExchange ex, byte[] data, String contentType) throws IOException {
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, data.length);
        try (var os = ex.getResponseBody()) { os.write(data); }
    }

    private String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg"))  return "image/jpeg";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        if (path.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }
}
