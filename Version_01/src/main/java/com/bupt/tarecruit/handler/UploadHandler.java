package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

public class UploadHandler extends BaseHandler {

    public UploadHandler(DataService ds) { super(ds); }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");

        if ("POST".equals(method)) {
            upload(ex);
        } else if ("GET".equals(method) && parts.length >= 4) {
            download(ex, parts[3]);
        } else {
            sendError(ex, 405, "Method not allowed");
        }
    }

    private void upload(HttpExchange ex) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        try {
            JsonObject body = parseJson(readBody(ex));
            String fileName = body.get("fileName").getAsString();
            String base64Data = body.get("data").getAsString();
            byte[] data = Base64.getDecoder().decode(base64Data);
            String savedName = ds.saveUpload(fileName, data);
            sendJson(ex, 200, Map.of("fileName", savedName));
        } catch (Exception e) {
            sendError(ex, 400, "Upload failed: " + e.getMessage());
        }
    }

    private void download(HttpExchange ex, String fileName) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        try {
            byte[] data = ds.getUpload(fileName);
            String ct = fileName.endsWith(".pdf") ? "application/pdf" : "application/octet-stream";
            ex.getResponseHeaders().set("Content-Type", ct);
            ex.getResponseHeaders().set("Content-Disposition", "inline; filename=\"" + fileName + "\"");
            addCorsHeaders(ex);
            ex.sendResponseHeaders(200, data.length);
            try (var os = ex.getResponseBody()) { os.write(data); }
        } catch (Exception e) {
            sendError(ex, 404, "File not found");
        }
    }
}
