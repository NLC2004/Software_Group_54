package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.*;
import com.bupt.tarecruit.service.DataService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AdminHandler extends BaseHandler {

    public AdminHandler(DataService ds) { super(ds); }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        if (!"ADMIN".equals(user.role)) { sendError(ex, 403, "Admin only"); return; }

        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        try {
            if (path.contains("/users")) handleUsers(ex, path, method);
            else if (path.endsWith("/workload")) getWorkload(ex);
            else if (path.endsWith("/stats")) getStats(ex);
            else if (path.contains("/settings")) handleSettings(ex, method);
            else sendError(ex, 404, "Not found");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Internal error");
        }
    }

    private void handleUsers(HttpExchange ex, String path, String method) throws IOException {
        String[] parts = path.split("/");
        if (parts.length == 4 && "GET".equals(method)) {
            String search = getQueryParam(ex, "search");
            List<User> users = ds.getAllUsers();
            if (search != null && !search.isEmpty()) {
                String q = search.toLowerCase();
                users = users.stream().filter(u ->
                        (u.username != null && u.username.toLowerCase().contains(q)) ||
                        (u.fullName != null && u.fullName.toLowerCase().contains(q)) ||
                        (u.email != null && u.email.toLowerCase().contains(q))
                ).collect(Collectors.toList());
            }
            List<Map<String, Object>> result = users.stream().map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", u.id); m.put("username", u.username); m.put("role", u.role);
                m.put("fullName", u.fullName); m.put("email", u.email);
                m.put("active", u.active); m.put("createdAt", u.createdAt);
                return m;
            }).collect(Collectors.toList());
            sendJson(ex, 200, result);
        } else if (parts.length == 5 && "PUT".equals(method)) {
            String userId = parts[4];
            User target = ds.getUserById(userId);
            if (target == null) { sendError(ex, 404, "User not found"); return; }
            JsonObject body = parseJson(readBody(ex));
            if (body.has("active")) target.active = body.get("active").getAsBoolean();
            if (body.has("role")) target.role = body.get("role").getAsString();
            if (body.has("password")) target.password = body.get("password").getAsString();
            ds.updateUser(target);
            sendJson(ex, 200, Map.of("message", "User updated"));
        } else if (parts.length == 5 && "DELETE".equals(method)) {
            ds.deleteUser(parts[4]);
            sendJson(ex, 200, Map.of("message", "User deleted"));
        } else {
            sendError(ex, 404, "Not found");
        }
    }

    private void getWorkload(HttpExchange ex) throws IOException {
        List<User> tas = ds.getAllUsers().stream().filter(u -> "TA".equals(u.role)).collect(Collectors.toList());
        List<Application> allApps = ds.getAllApplications();
        List<Job> allJobs = ds.getAllJobs();

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
            m.put("totalWeeklyHours", totalHours);
            m.put("approvedPositions", approvedCount);
            m.put("jobTitles", jobTitles);
            m.put("overloaded", totalHours > maxHours);
            m.put("maxHours", maxHours);
            result.add(m);
        }
        sendJson(ex, 200, result);
    }

    private void getStats(HttpExchange ex) throws IOException {
        List<User> users = ds.getAllUsers();
        List<Job> jobs = ds.getAllJobs();
        List<Application> apps = ds.getAllApplications();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", users.size());
        stats.put("totalTAs", users.stream().filter(u -> "TA".equals(u.role)).count());
        stats.put("totalMOs", users.stream().filter(u -> "MO".equals(u.role)).count());
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
        sendJson(ex, 200, stats);
    }

    private void handleSettings(HttpExchange ex, String method) throws IOException {
        if ("GET".equals(method)) {
            sendJson(ex, 200, ds.getSettings());
        } else if ("PUT".equals(method)) {
            JsonObject body = parseJson(readBody(ex));
            Map<String, String> settings = ds.getSettings();
            body.entrySet().forEach(e -> settings.put(e.getKey(), e.getValue().getAsString()));
            ds.updateSettings(settings);
            sendJson(ex, 200, settings);
        } else {
            sendError(ex, 405, "Method not allowed");
        }
    }
}
