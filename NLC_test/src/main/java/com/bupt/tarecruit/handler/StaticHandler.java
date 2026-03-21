package com.bupt.tarecruit.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public class StaticHandler implements HttpHandler {
    private final String staticDir;

    public StaticHandler(String staticDir) { this.staticDir = staticDir; }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if ("/".equals(path)) path = "/login.html";

        // Try filesystem first
        Path filePath = Paths.get(staticDir, path);
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            serve(ex, Files.readAllBytes(filePath), contentType(path));
            return;
        }

        // Try classpath
        InputStream is = getClass().getResourceAsStream("/static" + path);
        if (is != null) {
            byte[] data = is.readAllBytes();
            is.close();
            serve(ex, data, contentType(path));
            return;
        }

        String msg = "Not Found";
        ex.sendResponseHeaders(404, msg.length());
        try (var os = ex.getResponseBody()) { os.write(msg.getBytes()); }
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
        if (path.endsWith(".svg"))  return "image/svg+xml";
        return "application/octet-stream";
    }
}
