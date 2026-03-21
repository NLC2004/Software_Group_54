package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthHandler extends BaseHandler {

    public AuthHandler(DataService ds) { super(ds); }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        try {
            if (path.endsWith("/login") && "POST".equals(method)) login(ex);
            else if (path.endsWith("/register") && "POST".equals(method)) register(ex);
            else if (path.endsWith("/logout") && "POST".equals(method)) logout(ex);
            else if (path.endsWith("/me") && "GET".equals(method)) me(ex);
            else if (path.endsWith("/profile") && "PUT".equals(method)) updateProfile(ex);
            else if (path.endsWith("/password") && "PUT".equals(method)) changePassword(ex);
            else sendError(ex, 404, "Not found");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Internal error");
        }
    }

    private void login(HttpExchange ex) throws IOException {
        JsonObject body = parseJson(readBody(ex));
        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();

        User user = ds.getUserByUsername(username);
        if (user == null || !user.password.equals(password)) {
            sendError(ex, 401, "Invalid username or password"); return;
        }
        if (!user.active) {
            sendError(ex, 403, "Account is deactivated, please contact admin"); return;
        }

        String token = ds.createSession(user.id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("user", sanitize(user));
        sendJson(ex, 200, resp);
    }

    private void register(HttpExchange ex) throws IOException {
        JsonObject body = parseJson(readBody(ex));
        String username = body.get("username").getAsString();

        if (ds.getUserByUsername(username) != null) {
            sendError(ex, 409, "Username already exists"); return;
        }

        User user = new User();
        user.username = username;
        user.password = body.get("password").getAsString();
        user.role = body.has("role") ? body.get("role").getAsString() : "TA";
        user.fullName = body.has("fullName") ? body.get("fullName").getAsString() : "";
        user.email = body.has("email") ? body.get("email").getAsString() : "";
        user = ds.addUser(user);

        String token = ds.createSession(user.id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("user", sanitize(user));
        sendJson(ex, 201, resp);
    }

    private void logout(HttpExchange ex) throws IOException {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) ds.removeSession(auth.substring(7));
        sendJson(ex, 200, Map.of("message", "Logged out"));
    }

    private void me(HttpExchange ex) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        sendJson(ex, 200, sanitize(user));
    }

    private void updateProfile(HttpExchange ex) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        JsonObject body = parseJson(readBody(ex));
        if (body.has("fullName")) user.fullName = body.get("fullName").getAsString();
        if (body.has("email")) user.email = body.get("email").getAsString();
        if (body.has("phone")) user.phone = body.get("phone").getAsString();
        if (body.has("gender")) user.gender = body.get("gender").getAsString();
        ds.updateUser(user);
        sendJson(ex, 200, sanitize(user));
    }

    private void changePassword(HttpExchange ex) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        JsonObject body = parseJson(readBody(ex));
        if (!user.password.equals(body.get("oldPassword").getAsString())) {
            sendError(ex, 400, "Current password is incorrect"); return;
        }
        user.password = body.get("newPassword").getAsString();
        ds.updateUser(user);
        sendJson(ex, 200, Map.of("message", "Password updated"));
    }

    private Map<String, Object> sanitize(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.id); m.put("username", u.username); m.put("role", u.role);
        m.put("fullName", u.fullName); m.put("email", u.email);
        m.put("phone", u.phone); m.put("gender", u.gender);
        m.put("active", u.active); m.put("createdAt", u.createdAt);
        return m;
    }
}
