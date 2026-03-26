package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class BaseHandler implements HttpHandler {
    protected final DataService ds;
    protected final Gson gson = new Gson();

    public BaseHandler(DataService ds) { this.ds = ds; }

    protected String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    protected JsonObject parseJson(String body) {
        return gson.fromJson(body, JsonObject.class);
    }

    protected void sendJson(HttpExchange ex, int code, Object data) throws IOException {
        byte[] bytes = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        addCorsHeaders(ex);
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) { os.write(bytes); }
    }

    protected void sendError(HttpExchange ex, int code, String msg) throws IOException {
        sendJson(ex, code, Map.of("error", msg));
    }

    protected User authenticate(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return ds.getSessionUser(auth.substring(7));
    }

    protected String getQueryParam(HttpExchange ex, String key) {
        String query = ex.getRequestURI().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv[0].equals(key)) return kv.length > 1 ? java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
        }
        return null;
    }

    protected void addCorsHeaders(HttpExchange ex) {
        var h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    protected boolean handleCors(HttpExchange ex) throws IOException {
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            addCorsHeaders(ex);
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }
}
