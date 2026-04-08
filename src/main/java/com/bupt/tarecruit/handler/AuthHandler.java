package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.Notification;
import com.bupt.tarecruit.model.PasswordResetRequest;
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
            else if (path.endsWith("/password-reset") && "POST".equals(method)) requestPasswordReset(ex);
            else sendError(ex, 404, "Not found");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Internal error");
        }
    }

    private void login(HttpExchange ex) throws IOException {
        JsonObject body = parseJson(readBody(ex));
        String identifier = body.has("identifier") ? body.get("identifier").getAsString() : "";
        String password = body.get("password").getAsString();
        String portalRole = body.has("portalRole") ? body.get("portalRole").getAsString() : "";

        User user = ds.getUserByStudentIdOrEmail(identifier);
        // Allow username login as fallback (needed for admin account "admin" on MO portal).
        if (user == null) user = ds.getUserByUsername(identifier);
        if (user == null || !user.password.equals(password)) {
            sendError(ex, 401, "Invalid student ID, email, or password"); return;
        }
        if (portalRole != null && !portalRole.trim().isEmpty()) {
            String pr = portalRole.trim().toUpperCase();
            // Keep MO portal backward compatible: admin can still login from MO window.
            boolean moPortalAllowAdmin = "MO".equals(pr) && "ADMIN".equals(user.role);
            if (!pr.equals(user.role) && !moPortalAllowAdmin) {
                sendError(ex, 403, "This portal is for " + pr + " accounts only");
                return;
            }
        }
        if (!user.active) {
            sendError(ex, 403, "Account is deactivated, please contact admin"); return;
        }

        String token = ds.createSession(user.id);
        ds.addAuditLog(user.id, user.username, "LOGIN", "User logged in");

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
        if ("ADMIN".equalsIgnoreCase(user.role)) {
            sendError(ex, 403, "Admin accounts cannot be registered");
            return;
        }
        user.fullName = body.has("fullName") ? body.get("fullName").getAsString() : "";
        user.email = body.has("email") ? body.get("email").getAsString() : "";
        user.studentId = body.has("studentId") ? body.get("studentId").getAsString() : "";
        if (user.fullName.trim().isEmpty() || user.email.trim().isEmpty() || user.studentId.trim().isEmpty()) {
            sendError(ex, 400, "Please fill in all required fields");
            return;
        }
        user = ds.addUser(user);

        ds.addAuditLog(user.id, user.username, "REGISTER", "New " + user.role + " registered");

        String token = ds.createSession(user.id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("user", sanitize(user));
        sendJson(ex, 201, resp);
    }

    private void logout(HttpExchange ex) throws IOException {
        User user = authenticate(ex);
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) ds.removeSession(auth.substring(7));
        if (user != null) ds.addAuditLog(user.id, user.username, "LOGOUT", "User logged out");
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
        if (body.has("studentId")) user.studentId = body.get("studentId").getAsString();
        if (body.has("school")) user.school = body.get("school").getAsString();
        if (body.has("supervisor")) user.supervisor = body.get("supervisor").getAsString();
        if (body.has("degree")) user.degree = body.get("degree").getAsString();
        if (body.has("yearOfStudy")) user.yearOfStudy = body.get("yearOfStudy").getAsString();
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
        ds.addAuditLog(user.id, user.username, "PASSWORD_CHANGE", "User changed password");
        sendJson(ex, 200, Map.of("message", "Password updated"));
    }

    private void requestPasswordReset(HttpExchange ex) throws IOException {
        JsonObject body = parseJson(readBody(ex));
        PasswordResetRequest req = new PasswordResetRequest();
        req.studentId = body.has("studentId") ? body.get("studentId").getAsString() : "";
        req.fullName = body.has("fullName") ? body.get("fullName").getAsString() : "";
        req.email = body.has("email") ? body.get("email").getAsString() : "";
        req.phone = body.has("phone") ? body.get("phone").getAsString() : "";
        req.notes = body.has("notes") ? body.get("notes").getAsString() : "";
        req = ds.addPasswordReset(req);

        Notification n = new Notification();
        n.userId = "admin001";
        n.title = "New Password Reset Request";
        n.content = "User " + req.fullName + " (ID: " + req.studentId + ") requested a password reset.";
        n.type = "PASSWORD_RESET";
        ds.addNotification(n);

        ds.addAuditLog("", req.studentId, "PASSWORD_RESET_REQUEST", "Reset request from " + req.fullName);
        sendJson(ex, 201, Map.of("message", "Password reset request submitted. Please wait for admin approval."));
    }

    private Map<String, Object> sanitize(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.id); m.put("username", u.username); m.put("role", u.role);
        m.put("fullName", u.fullName); m.put("email", u.email);
        m.put("phone", u.phone); m.put("gender", u.gender);
        m.put("studentId", u.studentId); m.put("school", u.school);
        m.put("supervisor", u.supervisor); m.put("degree", u.degree);
        m.put("yearOfStudy", u.yearOfStudy);
        m.put("active", u.active); m.put("createdAt", u.createdAt);
        return m;
    }
}
