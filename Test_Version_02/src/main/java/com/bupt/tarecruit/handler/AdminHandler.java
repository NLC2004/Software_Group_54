package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.*;
import com.bupt.tarecruit.service.DataService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class AdminHandler extends BaseHandler {

    public AdminHandler(DataService ds) { super(ds); }

    private boolean isSuperAdmin(User user) {
        return user != null
                && "ADMIN".equals(user.role)
                && user.username != null
                && "admin".equalsIgnoreCase(user.username.trim());
    }

    private boolean ensureSuperAdminForWrite(HttpExchange ex, User user, String action) throws IOException {
        if (isSuperAdmin(user)) return true;
        sendError(ex, 403, "Only super admin (admin) can " + action);
        return false;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        if (!"ADMIN".equals(user.role)) { sendError(ex, 403, "Admin only"); return; }

        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        try {
            if (path.contains("/password-resets")) handlePasswordResets(ex, path, method, user);
            else if (path.contains("/bulk-notifications")) handleBulkNotifications(ex, method, user);
            else if (path.contains("/export-tasks")) handleExportTasks(ex, path, method, user);
            else if (path.contains("/role-templates")) handleRoleTemplates(ex, path, method, user);
            else if (path.contains("/users")) handleUsers(ex, path, method, user);
            else if (path.endsWith("/workload")) getWorkload(ex);
            else if (path.endsWith("/stats")) getStats(ex);
            else if (path.contains("/settings")) handleSettings(ex, method, user);
            else if (path.endsWith("/audit-logs")) getAuditLogs(ex);
            else if (path.endsWith("/export")) exportData(ex);
            else sendError(ex, 404, "Not found");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Internal error");
        }
    }

    private void handleUsers(HttpExchange ex, String path, String method, User admin) throws IOException {
        String[] parts = path.split("/");
        if (parts.length == 5 && "GET".equals(method)) {
            String userId = parts[4];
            User target = ds.getUserById(userId);
            if (target == null) { sendError(ex, 404, "User not found"); return; }
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("id", target.id); one.put("username", target.username); one.put("role", target.role);
            one.put("fullName", target.fullName); one.put("email", target.email);
            one.put("phone", target.phone); one.put("studentId", target.studentId);
            one.put("active", target.active); one.put("createdAt", target.createdAt);
            one.put("adminRoleTemplateId", target.adminRoleTemplateId != null ? target.adminRoleTemplateId : "");
            sendJson(ex, 200, one);
        } else if (parts.length == 4 && "GET".equals(method)) {
            String search = getQueryParam(ex, "search");
            String role = getQueryParam(ex, "role");
            List<User> users = ds.getAllUsers();
            if (search != null && !search.isEmpty()) {
                String q = search.toLowerCase();
                users = users.stream().filter(u ->
                        (u.username != null && u.username.toLowerCase().contains(q)) ||
                        (u.fullName != null && u.fullName.toLowerCase().contains(q)) ||
                        (u.email != null && u.email.toLowerCase().contains(q)) ||
                        (u.studentId != null && u.studentId.toLowerCase().contains(q))
                ).collect(Collectors.toList());
            }
            if (role != null && !role.isEmpty()) {
                users = users.stream().filter(u -> u.role.equals(role)).collect(Collectors.toList());
            }
            List<Map<String, Object>> result = users.stream().map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", u.id); m.put("username", u.username); m.put("role", u.role);
                m.put("fullName", u.fullName); m.put("email", u.email);
                m.put("phone", u.phone); m.put("studentId", u.studentId);
                m.put("active", u.active); m.put("createdAt", u.createdAt);
                m.put("adminRoleTemplateId", u.adminRoleTemplateId != null ? u.adminRoleTemplateId : "");
                return m;
            }).collect(Collectors.toList());
            sendJson(ex, 200, result);
        } else if (parts.length == 4 && "POST".equals(method)) {
            if (!ensureSuperAdminForWrite(ex, admin, "create users")) return;
            JsonObject body = parseJson(readBody(ex));
            String username = body.has("username") ? body.get("username").getAsString().trim() : "";
            String password = body.has("password") ? body.get("password").getAsString() : "";
            String role = body.has("role") ? body.get("role").getAsString().trim().toUpperCase(Locale.ROOT) : "TA";
            String fullName = body.has("fullName") ? body.get("fullName").getAsString().trim() : "";
            String email = body.has("email") ? body.get("email").getAsString().trim() : "";
            String studentId = body.has("studentId") ? body.get("studentId").getAsString().trim() : "";

            if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
                sendError(ex, 400, "Username, password, fullName and email are required");
                return;
            }
            if (!"TA".equals(role) && !"MO".equals(role) && !"ADMIN".equals(role)) {
                sendError(ex, 400, "Invalid role");
                return;
            }
            if (ds.getUserByUsername(username) != null) {
                sendError(ex, 409, "Username already exists");
                return;
            }

            User created = new User();
            created.username = username;
            created.password = password;
            created.role = role;
            created.fullName = fullName;
            created.email = email;
            created.studentId = studentId;
            created.active = true;
            if ("ADMIN".equals(role)) {
                String tid = body.has("adminRoleTemplateId") ? body.get("adminRoleTemplateId").getAsString() : "";
                if (!tid.isEmpty() && ds.getAdminRoleTemplateById(tid) == null) {
                    sendError(ex, 400, "Invalid admin role template id");
                    return;
                }
                created.adminRoleTemplateId = tid;
            }

            created = ds.addUser(created);
            ds.addAuditLog(admin.id, admin.username, "USER_CREATE", "Created user: " + created.username + " (" + created.role + ")");
            sendJson(ex, 201, Map.of("id", created.id, "message", "User created"));
        } else if (parts.length == 5 && "PUT".equals(method)) {
            if (!ensureSuperAdminForWrite(ex, admin, "update user permissions")) return;
            String userId = parts[4];
            User target = ds.getUserById(userId);
            if (target == null) { sendError(ex, 404, "User not found"); return; }
            JsonObject body = parseJson(readBody(ex));
            if (body.has("password")) {
                String targetRole = target.role == null ? "" : target.role.toUpperCase(Locale.ROOT);
                if (("TA".equals(targetRole) || "MO".equals(targetRole)) && !isSuperAdmin(admin)) {
                    sendError(ex, 403, "Only super admin (admin) can reset TA/MO passwords");
                    return;
                }
            }
            if (body.has("active")) target.active = body.get("active").getAsBoolean();
            if (body.has("role")) target.role = body.get("role").getAsString();
            if (body.has("password")) target.password = body.get("password").getAsString();
            if (body.has("adminRoleTemplateId")) {
                if (body.get("adminRoleTemplateId").isJsonNull()) {
                    target.adminRoleTemplateId = "";
                } else {
                    String tid = body.get("adminRoleTemplateId").getAsString();
                    if (tid.isEmpty()) target.adminRoleTemplateId = "";
                    else if (ds.getAdminRoleTemplateById(tid) == null) {
                        sendError(ex, 400, "Invalid admin role template id"); return;
                    } else target.adminRoleTemplateId = tid;
                }
            }
            ds.updateUser(target);
            ds.addAuditLog(admin.id, admin.username, "USER_UPDATE", "Updated user: " + target.username);
            sendJson(ex, 200, Map.of("message", "User updated"));
        } else if (parts.length == 6 && "POST".equals(method) && "notify-password-reset".equals(parts[5])) {
            String userId = parts[4];
            User target = ds.getUserById(userId);
            if (target == null) { sendError(ex, 404, "User not found"); return; }
            if (!"TA".equalsIgnoreCase(target.role) && !"MO".equalsIgnoreCase(target.role)) {
                sendError(ex, 400, "Only TA/MO accounts support password reset escalation");
                return;
            }
            User superAdmin = ds.getUserByUsername("admin");
            if (superAdmin == null) {
                sendError(ex, 500, "Super admin account not found");
                return;
            }
            Notification n = new Notification();
            n.userId = superAdmin.id;
            n.title = "Password Reset Escalation";
            n.content = "Admin " + admin.username + " requested super-admin password reset for user: "
                    + (target.fullName != null && !target.fullName.isEmpty() ? target.fullName : target.username)
                    + " (" + target.role + ", id=" + target.id + ").";
            n.type = "PASSWORD_RESET";
            ds.addNotification(n);
            ds.addAuditLog(admin.id, admin.username, "PASSWORD_RESET_ESCALATE",
                    "Escalated password reset for user: " + target.username + " (" + target.role + ")");
            sendJson(ex, 200, Map.of("message", "Escalation sent to super admin"));
        } else if (parts.length == 5 && "DELETE".equals(method)) {
            if (!ensureSuperAdminForWrite(ex, admin, "delete users")) return;
            User target = ds.getUserById(parts[4]);
            ds.deleteUser(parts[4]);
            ds.addAuditLog(admin.id, admin.username, "USER_DELETE", "Deleted user: " + (target != null ? target.username : parts[4]));
            sendJson(ex, 200, Map.of("message", "User deleted"));
        } else {
            sendError(ex, 404, "Not found");
        }
    }

    private void handlePasswordResets(HttpExchange ex, String path, String method, User admin) throws IOException {
        String[] parts = path.split("/");
        if ("GET".equals(method) && parts.length == 4) {
            List<PasswordResetRequest> reqs = ds.getAllPasswordResets();
            String status = getQueryParam(ex, "status");
            if (status != null && !status.isEmpty()) {
                reqs = reqs.stream().filter(r -> r.status.equals(status)).collect(Collectors.toList());
            }
            reqs.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
            sendJson(ex, 200, reqs);
        } else if ("PUT".equals(method) && parts.length == 5) {
            if (!ensureSuperAdminForWrite(ex, admin, "process password reset requests")) return;
            String reqId = parts[4];
            PasswordResetRequest req = ds.getPasswordResetById(reqId);
            if (req == null) { sendError(ex, 404, "Request not found"); return; }
            JsonObject body = parseJson(readBody(ex));
            String action = body.get("action").getAsString();

            User target = null;
            if (req.studentId != null && !req.studentId.isEmpty()) {
                target = ds.getAllUsers().stream()
                        .filter(u -> req.studentId.equals(u.studentId) || req.studentId.equals(u.username))
                        .findFirst().orElse(null);
            }
            if (target != null) {
                String targetRole = target.role == null ? "" : target.role.toUpperCase(Locale.ROOT);
                if (("TA".equals(targetRole) || "MO".equals(targetRole)) && !isSuperAdmin(admin)) {
                    sendError(ex, 403, "Only super admin (admin) can process TA/MO password reset requests");
                    return;
                }
            }

            if ("APPROVE".equals(action)) {
                req.status = "APPROVED";
                req.processedAt = System.currentTimeMillis();
                ds.updatePasswordReset(req);
                if (target != null) {
                    target.password = "123456";
                    ds.updateUser(target);

                    Notification n = new Notification();
                    n.userId = target.id;
                    n.title = "Password Reset Approved";
                    n.content = "Your password has been reset to the initial password. Please change it after login.";
                    n.type = "PASSWORD_RESET";
                    ds.addNotification(n);
                }
                ds.addAuditLog(admin.id, admin.username, "PASSWORD_RESET_APPROVE", "Approved reset for: " + req.fullName);
                sendJson(ex, 200, Map.of("message", "Password reset approved"));
            } else if ("REJECT".equals(action)) {
                req.status = "REJECTED";
                req.reason = body.has("reason") ? body.get("reason").getAsString() : "";
                req.processedAt = System.currentTimeMillis();
                ds.updatePasswordReset(req);
                ds.addAuditLog(admin.id, admin.username, "PASSWORD_RESET_REJECT", "Rejected reset for: " + req.fullName);
                sendJson(ex, 200, Map.of("message", "Password reset rejected"));
            } else {
                sendError(ex, 400, "Invalid action");
            }
        } else if ("POST".equals(method) && parts.length == 6 && "escalate".equals(parts[5])) {
            String reqId = parts[4];
            PasswordResetRequest req = ds.getPasswordResetById(reqId);
            if (req == null) { sendError(ex, 404, "Request not found"); return; }
            User superAdmin = ds.getUserByUsername("admin");
            if (superAdmin == null) {
                sendError(ex, 500, "Super admin account not found");
                return;
            }
            Notification n = new Notification();
            n.userId = superAdmin.id;
            n.title = "Password Reset Request Escalation";
            n.content = "Admin " + admin.username + " escalated reset request: "
                    + (req.fullName != null ? req.fullName : "")
                    + " (studentId=" + (req.studentId != null ? req.studentId : "") + ").";
            n.type = "PASSWORD_RESET";
            ds.addNotification(n);
            ds.addAuditLog(admin.id, admin.username, "PASSWORD_RESET_REQUEST_ESCALATE",
                    "Escalated reset request id=" + req.id + " for " + (req.fullName != null ? req.fullName : req.studentId));
            sendJson(ex, 200, Map.of("message", "Escalation sent to super admin"));
        } else {
            sendError(ex, 404, "Not found");
        }
    }

    private void getWorkload(HttpExchange ex) throws IOException {
        List<User> tas = ds.getAllUsers().stream().filter(u -> "TA".equals(u.role)).collect(Collectors.toList());
        List<Application> allApps = ds.getAllApplications();
        List<Job> allJobs = ds.getAllJobs();
        String faculty = Optional.ofNullable(getQueryParam(ex, "faculty")).orElse("").trim().toLowerCase(Locale.ROOT);
        String status = Optional.ofNullable(getQueryParam(ex, "status")).orElse("").trim().toLowerCase(Locale.ROOT);
        if (!faculty.isEmpty() && !"all".equals(faculty)) {
            tas = tas.stream().filter(ta -> {
                String sch = ta.school == null ? "" : ta.school.toLowerCase(Locale.ROOT);
                return sch.contains(faculty);
            }).collect(Collectors.toList());
        }

        Map<String, String> settings = ds.getSettings();
        double maxHours = 20;
        try { if (settings.containsKey("maxWeeklyHours")) maxHours = Double.parseDouble(settings.get("maxWeeklyHours")); }
        catch (NumberFormatException ignored) {}

        List<Map<String, Object>> result = new ArrayList<>();
        for (User ta : tas) {
            double totalHours = 0;
            int approvedCount = 0;
            List<String> jobTitles = new ArrayList<>();

            List<Application> taApps = allApps.stream()
                    .filter(a -> a.applicantId.equals(ta.id) && "APPROVED".equals(a.status))
                    .collect(Collectors.toList());
            for (Application app : taApps) {
                Job job = allJobs.stream().filter(j -> j.id.equals(app.jobId)).findFirst().orElse(null);
                if (job != null) {
                    totalHours += job.weeklyHours;
                    approvedCount++;
                    jobTitles.add(job.title);
                }
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", ta.id);
            m.put("username", ta.username);
            m.put("fullName", ta.fullName != null && !ta.fullName.isEmpty() ? ta.fullName : ta.username);
            m.put("email", ta.email);
            m.put("faculty", ta.school != null && !ta.school.isEmpty() ? ta.school : "N/A");
            m.put("totalWeeklyHours", totalHours);
            m.put("approvedPositions", approvedCount);
            m.put("jobTitles", jobTitles);
            m.put("overloaded", totalHours > maxHours);
            m.put("warning", totalHours > (maxHours * 0.8) && totalHours <= maxHours);
            m.put("maxHours", maxHours);
            result.add(m);
        }
        if (!status.isEmpty() && !"all".equals(status)) {
            result = result.stream().filter(row -> {
                double h = row.get("totalWeeklyHours") instanceof Number ? ((Number) row.get("totalWeeklyHours")).doubleValue() : 0;
                double max = row.get("maxHours") instanceof Number ? ((Number) row.get("maxHours")).doubleValue() : 20;
                if ("overload".equals(status)) return h > max;
                if ("warning".equals(status)) return h > (max * 0.8) && h <= max;
                if ("normal".equals(status)) return h <= (max * 0.8);
                return true;
            }).collect(Collectors.toList());
        }
        sendJson(ex, 200, result);
    }

    private void getStats(HttpExchange ex) throws IOException {
        Long startMs = parseDateStart(getQueryParam(ex, "startDate"));
        Long endMs = parseDateEnd(getQueryParam(ex, "endDate"));
        List<User> users = ds.getAllUsers().stream().filter(u -> inRange(u.createdAt, startMs, endMs)).collect(Collectors.toList());
        List<Job> jobs = ds.getAllJobs().stream().filter(j -> inRange(j.createdAt, startMs, endMs)).collect(Collectors.toList());
        List<Application> apps = ds.getAllApplications().stream().filter(a -> inRange(a.createdAt, startMs, endMs)).collect(Collectors.toList());
        List<Application> activeApps = apps.stream()
                .filter(a -> !"WITHDRAWN".equals(a.status) && !"REJECTED".equals(a.status))
                .collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", users.size());
        stats.put("totalTAs", users.stream().filter(u -> "TA".equals(u.role)).count());
        stats.put("totalMOs", users.stream().filter(u -> "MO".equals(u.role)).count());
        stats.put("totalAdmins", users.stream().filter(u -> "ADMIN".equals(u.role)).count());
        stats.put("activeUsers", users.stream().filter(u -> u.active).count());
        stats.put("totalJobs", jobs.size());
        stats.put("openJobs", jobs.stream().filter(j -> "OPEN".equals(j.status)).count());
        stats.put("closedJobs", jobs.stream().filter(j -> "CLOSED".equals(j.status)).count());
        stats.put("totalApplications", apps.size());
        stats.put("pendingApplications", apps.stream().filter(a -> "PENDING".equals(a.status)).count());
        stats.put("approvedApplications", apps.stream().filter(a -> "APPROVED".equals(a.status)).count());
        stats.put("rejectedApplications", apps.stream().filter(a -> "REJECTED".equals(a.status)).count());
        stats.put("withdrawnApplications", apps.stream().filter(a -> "WITHDRAWN".equals(a.status)).count());

        long overloaded = 0;
        Map<String, String> settings = ds.getSettings();
        double maxH = 20;
        try { if (settings.containsKey("maxWeeklyHours")) maxH = Double.parseDouble(settings.get("maxWeeklyHours")); }
        catch (NumberFormatException ignored) {}
        for (User ta : users.stream().filter(u -> "TA".equals(u.role)).collect(Collectors.toList())) {
            double h = 0;
            for (Application a : apps.stream().filter(a -> a.applicantId.equals(ta.id) && "APPROVED".equals(a.status)).collect(Collectors.toList())) {
                Job j = jobs.stream().filter(jj -> jj.id.equals(a.jobId)).findFirst().orElse(null);
                if (j != null) h += j.weeklyHours;
            }
            if (h > maxH) overloaded++;
        }
        stats.put("overloadedTAs", overloaded);

        long totalQuota = jobs.stream().mapToLong(j -> j.quota).sum();
        stats.put("totalQuota", totalQuota);

        long pendingResets = ds.getAllPasswordResets().stream().filter(r -> "PENDING".equals(r.status)).count();
        stats.put("pendingPasswordResets", pendingResets);

        Map<String, Object> activePreferenceUsage = new LinkedHashMap<>();
        activePreferenceUsage.put("priority1", activeApps.stream().filter(a -> a.priority == 1).count());
        activePreferenceUsage.put("priority2", activeApps.stream().filter(a -> a.priority == 2).count());
        activePreferenceUsage.put("priority3", activeApps.stream().filter(a -> a.priority == 3).count());
        activePreferenceUsage.put("withoutPriority", activeApps.stream().filter(a -> a.priority <= 0).count());
        activePreferenceUsage.put("totalActivePreferences", activeApps.size());
        stats.put("activePreferenceUsage", activePreferenceUsage);

        Map<String, Object> taPriorityDistribution = new LinkedHashMap<>();
        List<User> taUsers = users.stream().filter(u -> "TA".equals(u.role)).collect(Collectors.toList());
        taPriorityDistribution.put("tasWithPriority1", countDistinctApplicantsByPriority(activeApps, 1));
        taPriorityDistribution.put("tasWithPriority2", countDistinctApplicantsByPriority(activeApps, 2));
        taPriorityDistribution.put("tasWithPriority3", countDistinctApplicantsByPriority(activeApps, 3));
        taPriorityDistribution.put("tasWithoutActivePreference", Math.max(0, taUsers.size() - countDistinctApplicants(activeApps)));
        taPriorityDistribution.put("totalTAsInRange", taUsers.size());
        stats.put("taPriorityDistribution", taPriorityDistribution);

        sendJson(ex, 200, stats);
    }

    private long countDistinctApplicantsByPriority(List<Application> apps, int priority) {
        return apps.stream()
                .filter(a -> a.priority == priority)
                .map(a -> a.applicantId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private long countDistinctApplicants(List<Application> apps) {
        return apps.stream()
                .map(a -> a.applicantId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private void handleSettings(HttpExchange ex, String method, User admin) throws IOException {
        if ("GET".equals(method)) {
            sendJson(ex, 200, ds.getSettings());
        } else if ("PUT".equals(method)) {
            if (!ensureSuperAdminForWrite(ex, admin, "update system settings")) return;
            JsonObject body = parseJson(readBody(ex));
            Map<String, String> settings = ds.getSettings();
            body.entrySet().forEach(e -> settings.put(e.getKey(), e.getValue().getAsString()));
            ds.updateSettings(settings);
            ds.addAuditLog(admin.id, admin.username, "SETTINGS_UPDATE", "Updated system settings");
            sendJson(ex, 200, settings);
        } else {
            sendError(ex, 405, "Method not allowed");
        }
    }

    private void getAuditLogs(HttpExchange ex) throws IOException {
        List<AuditLog> logs = ds.getAllAuditLogs();
        String action = getQueryParam(ex, "action");
        String search = getQueryParam(ex, "search");
        if (action != null && !action.isEmpty()) {
            logs = logs.stream().filter(l -> l.action.equals(action)).collect(Collectors.toList());
        }
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            logs = logs.stream().filter(l ->
                (l.username != null && l.username.toLowerCase().contains(q)) ||
                (l.detail != null && l.detail.toLowerCase().contains(q)) ||
                (l.action != null && l.action.toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }
        logs.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
        sendJson(ex, 200, logs);
    }

    private void handleRoleTemplates(HttpExchange ex, String path, String method, User admin) throws IOException {
        String[] parts = path.split("/");
        if (parts.length == 5 && "GET".equals(method)) {
            String id = parts[4];
            AdminRoleTemplate t = ds.getAdminRoleTemplateById(id);
            if (t == null) { sendError(ex, 404, "Not found"); return; }
            long cnt = ds.getAllUsers().stream()
                    .filter(u -> "ADMIN".equals(u.role) && t.id != null && t.id.equals(u.adminRoleTemplateId))
                    .count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.id);
            m.put("name", t.name);
            m.put("description", t.description);
            m.put("tags", t.tags != null ? t.tags : new ArrayList<String>());
            m.put("createdAt", t.createdAt);
            m.put("assignedCount", cnt);
            sendJson(ex, 200, m);
            return;
        }
        if (parts.length == 4 && "GET".equals(method)) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (AdminRoleTemplate t : ds.getAllAdminRoleTemplates()) {
                long cnt = ds.getAllUsers().stream()
                        .filter(u -> "ADMIN".equals(u.role) && t.id != null && t.id.equals(u.adminRoleTemplateId))
                        .count();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.id);
                m.put("name", t.name);
                m.put("description", t.description);
                m.put("tags", t.tags != null ? t.tags : new ArrayList<String>());
                m.put("createdAt", t.createdAt);
                m.put("assignedCount", cnt);
                out.add(m);
            }
            sendJson(ex, 200, out);
        } else if (parts.length == 4 && "POST".equals(method)) {
            if (!ensureSuperAdminForWrite(ex, admin, "create role templates")) return;
            JsonObject body = parseJson(readBody(ex));
            AdminRoleTemplate t = new AdminRoleTemplate();
            t.name = body.has("name") ? body.get("name").getAsString().trim() : "Unnamed";
            if (t.name.isEmpty()) { sendError(ex, 400, "Name is required"); return; }
            t.description = body.has("description") ? body.get("description").getAsString() : "";
            t.tags = new ArrayList<>();
            if (body.has("tags") && body.get("tags").isJsonArray()) {
                for (int i = 0; i < body.get("tags").getAsJsonArray().size(); i++) {
                    t.tags.add(body.get("tags").getAsJsonArray().get(i).getAsString());
                }
            }
            ds.addAdminRoleTemplate(t);
            ds.addAuditLog(admin.id, admin.username, "ADMIN_ROLE_TEMPLATE_CREATE", "Created template: " + t.name);
            sendJson(ex, 201, Map.of("id", t.id, "message", "Created"));
        } else if (parts.length == 5 && "PUT".equals(method)) {
            if (!ensureSuperAdminForWrite(ex, admin, "update role templates")) return;
            String id = parts[4];
            AdminRoleTemplate existing = ds.getAdminRoleTemplateById(id);
            if (existing == null) { sendError(ex, 404, "Not found"); return; }
            JsonObject body = parseJson(readBody(ex));
            if (body.has("name")) {
                String nm = body.get("name").getAsString().trim();
                if (nm.isEmpty()) { sendError(ex, 400, "Name cannot be empty"); return; }
                existing.name = nm;
            }
            if (body.has("description")) existing.description = body.get("description").getAsString();
            if (body.has("tags") && body.get("tags").isJsonArray()) {
                existing.tags = new ArrayList<>();
                for (int i = 0; i < body.get("tags").getAsJsonArray().size(); i++) {
                    existing.tags.add(body.get("tags").getAsJsonArray().get(i).getAsString());
                }
            }
            ds.updateAdminRoleTemplate(existing);
            ds.addAuditLog(admin.id, admin.username, "ADMIN_ROLE_TEMPLATE_UPDATE", "Updated template: " + existing.name);
            sendJson(ex, 200, Map.of("message", "Updated"));
        } else if (parts.length == 5 && "DELETE".equals(method)) {
            if (!ensureSuperAdminForWrite(ex, admin, "delete role templates")) return;
            String id = parts[4];
            if (ds.getAdminRoleTemplateById(id) == null) { sendError(ex, 404, "Not found"); return; }
            ds.deleteAdminRoleTemplate(id);
            ds.addAuditLog(admin.id, admin.username, "ADMIN_ROLE_TEMPLATE_DELETE", "Deleted template id: " + id);
            sendJson(ex, 200, Map.of("message", "Deleted"));
        } else {
            sendError(ex, 404, "Not found");
        }
    }

    private void exportData(HttpExchange ex) throws IOException {
        User admin = authenticate(ex);
        if (admin == null || !"ADMIN".equals(admin.role)) { sendError(ex, 401, "Unauthorized"); return; }

        Long startMs = parseDateStart(getQueryParam(ex, "startDate"));
        Long endMs = parseDateEnd(getQueryParam(ex, "endDate"));
        String csv = ds.exportAllDataCsv(startMs, endMs);
        byte[] data = csv.getBytes(StandardCharsets.UTF_8);
        String fileName = ds.saveUpload("export_" + System.currentTimeMillis() + ".csv", data);

        ExportTask task = new ExportTask();
        task.dataSubject = "Core Data Export";
        task.dateRange = Optional.ofNullable(getQueryParam(ex, "dateRange")).orElse("All time");
        task.format = "CSV";
        task.status = "COMPLETED";
        task.generatorId = admin.id;
        task.generatorName = (admin.fullName != null && !admin.fullName.isEmpty()) ? admin.fullName : admin.username;
        task.fileName = fileName;
        ds.addExportTask(task);
        ds.addAuditLog(admin.id, admin.username, "EXPORT_CORE_DATA", "Exported core data CSV task=" + task.id);

        ex.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
        ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"ta_recruit_export.csv\"");
        addCorsHeaders(ex);
        ex.sendResponseHeaders(200, data.length);
        try (var os = ex.getResponseBody()) { os.write(data); }
    }

    private void handleExportTasks(HttpExchange ex, String path, String method, User admin) throws IOException {
        String[] parts = path.split("/");
        if ("GET".equals(method) && parts.length == 4) {
            String status = getQueryParam(ex, "status");
            String search = getQueryParam(ex, "search");
            Long startMs = parseDateStart(getQueryParam(ex, "startDate"));
            Long endMs = parseDateEnd(getQueryParam(ex, "endDate"));
            int page = parseIntOrDefault(getQueryParam(ex, "page"), 1);
            int pageSize = parseIntOrDefault(getQueryParam(ex, "pageSize"), 10);
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 10;
            if (pageSize > 100) pageSize = 100;

            List<ExportTask> all = ds.getAllExportTasks();
            if (status != null && !status.isEmpty()) {
                String s = status.toUpperCase(Locale.ROOT);
                all = all.stream().filter(t -> s.equals(String.valueOf(t.status).toUpperCase(Locale.ROOT))).collect(Collectors.toList());
            }
            if (search != null && !search.isEmpty()) {
                String q = search.toLowerCase(Locale.ROOT);
                all = all.stream().filter(t ->
                        (t.id != null && t.id.toLowerCase(Locale.ROOT).contains(q)) ||
                        (t.dataSubject != null && t.dataSubject.toLowerCase(Locale.ROOT).contains(q)) ||
                        (t.generatorName != null && t.generatorName.toLowerCase(Locale.ROOT).contains(q))
                ).collect(Collectors.toList());
            }
            if (startMs != null || endMs != null) {
                all = all.stream().filter(t -> inRange(t.createdAt, startMs, endMs)).collect(Collectors.toList());
            }
            all.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
            int total = all.size();
            int from = Math.min((page - 1) * pageSize, total);
            int to = Math.min(from + pageSize, total);
            List<ExportTask> items = all.subList(from, to);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("items", items);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            sendJson(ex, 200, result);
            return;
        }
        if ("GET".equals(method) && parts.length == 6 && "download".equals(parts[5])) {
            ExportTask task = ds.getExportTaskById(parts[4]);
            if (task == null) { sendError(ex, 404, "Export task not found"); return; }
            if (!"COMPLETED".equals(task.status) || task.fileName == null || task.fileName.isEmpty()) {
                sendError(ex, 400, "Export file is not ready"); return;
            }
            Path p = ds.getUploadsDir().resolve(task.fileName);
            if (!Files.exists(p)) { sendError(ex, 404, "Export file not found"); return; }
            byte[] data = Files.readAllBytes(p);
            ex.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"export_" + task.id + ".csv\"");
            addCorsHeaders(ex);
            ex.sendResponseHeaders(200, data.length);
            try (var os = ex.getResponseBody()) { os.write(data); }
            return;
        }
        if ("POST".equals(method) && parts.length == 6 && "retry".equals(parts[5])) {
            ExportTask task = ds.getExportTaskById(parts[4]);
            if (task == null) { sendError(ex, 404, "Export task not found"); return; }

            task.status = "PROCESSING";
            task.errorMessage = "";
            ds.updateExportTask(task);
            try {
                String csv = ds.exportAllDataCsv();
                byte[] data = csv.getBytes(StandardCharsets.UTF_8);
                task.fileName = ds.saveUpload("retry_export_" + System.currentTimeMillis() + ".csv", data);
                task.status = "COMPLETED";
                ds.updateExportTask(task);
                ds.addAuditLog(admin.id, admin.username, "EXPORT_RETRY", "Retried export task=" + task.id);
                sendJson(ex, 200, Map.of("message", "Retry completed"));
            } catch (Exception e) {
                task.status = "FAILED";
                task.errorMessage = e.getMessage() != null ? e.getMessage() : "Retry failed";
                ds.updateExportTask(task);
                sendError(ex, 500, "Retry failed");
            }
            return;
        }
        sendError(ex, 404, "Not found");
    }

    private void handleBulkNotifications(HttpExchange ex, String method, User admin) throws IOException {
        if (!"POST".equals(method)) {
            sendError(ex, 405, "Method not allowed");
            return;
        }
        if (!ensureSuperAdminForWrite(ex, admin, "send bulk notifications")) return;
        JsonObject body = parseJson(readBody(ex));
        String title = body.has("title") ? body.get("title").getAsString().trim() : "";
        String message = body.has("message") ? body.get("message").getAsString().trim() : "";
        if (title.isEmpty() || message.isEmpty()) {
            sendError(ex, 400, "Title and message are required");
            return;
        }

        Set<String> roleSet = new HashSet<>();
        if (body.has("roles") && body.get("roles").isJsonArray()) {
            for (int i = 0; i < body.get("roles").getAsJsonArray().size(); i++) {
                String r = body.get("roles").getAsJsonArray().get(i).getAsString().toUpperCase(Locale.ROOT);
                if ("TA".equals(r) || "MO".equals(r) || "ADMIN".equals(r)) roleSet.add(r);
            }
        }
        if (roleSet.isEmpty()) {
            roleSet.add("TA");
            roleSet.add("MO");
        }

        List<User> recipients = ds.getAllUsers().stream()
                .filter(u -> u.active && roleSet.contains(u.role))
                .collect(Collectors.toList());
        for (User u : recipients) {
            Notification n = new Notification();
            n.userId = u.id;
            n.title = title;
            n.content = message;
            n.type = "BULK_ANNOUNCEMENT";
            ds.addNotification(n);
        }

        StringBuilder txt = new StringBuilder();
        txt.append("Bulk Notification\n");
        txt.append("GeneratedAt: ").append(System.currentTimeMillis()).append("\n");
        txt.append("Sender: ").append(admin.username).append(" (").append(admin.id).append(")\n");
        txt.append("TargetRoles: ").append(String.join(",", roleSet)).append("\n");
        txt.append("RecipientCount: ").append(recipients.size()).append("\n\n");
        txt.append("Title: ").append(title).append("\n");
        txt.append("Message:\n").append(message).append("\n\n");
        txt.append("Recipients:\n");
        for (User u : recipients) {
            txt.append("- ").append(u.id).append(",").append(u.username).append(",").append(u.role).append("\n");
        }
        String fileName = ds.saveUpload("bulk_notification_" + System.currentTimeMillis() + ".txt",
                txt.toString().getBytes(StandardCharsets.UTF_8));

        ds.addAuditLog(admin.id, admin.username, "BULK_NOTIFY",
                "Bulk notification sent roles=" + String.join(",", roleSet) + ", recipients=" + recipients.size());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message", "Bulk notification sent");
        res.put("recipientCount", recipients.size());
        res.put("fileName", fileName);
        sendJson(ex, 200, res);
    }

    private int parseIntOrDefault(String s, int dft) {
        try { return s == null ? dft : Integer.parseInt(s); }
        catch (NumberFormatException e) { return dft; }
    }

    private Long parseDateStart(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseDateEnd(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return LocalDate.parse(date).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean inRange(long ts, Long startMs, Long endMs) {
        if (startMs != null && ts < startMs) return false;
        if (endMs != null && ts > endMs) return false;
        return true;
    }
}
