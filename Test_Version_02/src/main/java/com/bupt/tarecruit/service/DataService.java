package com.bupt.tarecruit.service;

import com.bupt.tarecruit.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DataService {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataDir;
    private final Path uploadsDir;
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public DataService(String baseDir) throws IOException {
        this.dataDir = Paths.get(baseDir, "data");
        this.uploadsDir = Paths.get(baseDir, "uploads");
        Files.createDirectories(dataDir);
        Files.createDirectories(uploadsDir);
        initDefaultData();
    }

    // ==================== Sessions ====================

    public String createSession(String userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, userId);
        return token;
    }

    public void removeSession(String token) { sessions.remove(token); }

    public User getSessionUser(String token) {
        String userId = sessions.get(token);
        return userId == null ? null : getUserById(userId);
    }

    // ==================== Users ====================

    public synchronized List<User> getAllUsers() {
        return readList("users.json", new TypeToken<List<User>>(){}.getType());
    }

    public synchronized User getUserById(String id) {
        return getAllUsers().stream().filter(u -> u.id.equals(id)).findFirst().orElse(null);
    }

    public synchronized User getUserByUsername(String username) {
        return getAllUsers().stream().filter(u -> u.username.equals(username)).findFirst().orElse(null);
    }

    public synchronized User getUserByStudentIdOrEmail(String identifier) {
        String normalized = identifier == null ? "" : identifier.trim();
        if (normalized.isEmpty()) return null;
        return getAllUsers().stream()
                .filter(u -> normalized.equalsIgnoreCase(u.studentId == null ? "" : u.studentId.trim())
                        || normalized.equalsIgnoreCase(u.email == null ? "" : u.email.trim()))
                .findFirst()
                .orElse(null);
    }

    public synchronized User addUser(User user) {
        List<User> users = getAllUsers();
        user.id = UUID.randomUUID().toString().substring(0, 8);
        user.createdAt = System.currentTimeMillis();
        users.add(user);
        writeList("users.json", users);
        return user;
    }

    public synchronized void updateUser(User user) {
        List<User> users = getAllUsers();
        users.removeIf(u -> u.id.equals(user.id));
        users.add(user);
        writeList("users.json", users);
    }

    public synchronized void deleteUser(String id) {
        List<User> users = getAllUsers();
        users.removeIf(u -> u.id.equals(id));
        writeList("users.json", users);
    }

    // ==================== Jobs ====================

    public synchronized List<Job> getAllJobs() {
        return readList("jobs.json", new TypeToken<List<Job>>(){}.getType());
    }

    public synchronized Job getJobById(String id) {
        return getAllJobs().stream().filter(j -> j.id.equals(id)).findFirst().orElse(null);
    }

    public synchronized Job addJob(Job job) {
        List<Job> jobs = getAllJobs();
        job.id = UUID.randomUUID().toString().substring(0, 8);
        job.createdAt = System.currentTimeMillis();
        job.status = "OPEN";
        jobs.add(job);
        writeList("jobs.json", jobs);
        return job;
    }

    public synchronized void updateJob(Job job) {
        List<Job> jobs = getAllJobs();
        jobs.removeIf(j -> j.id.equals(job.id));
        jobs.add(job);
        writeList("jobs.json", jobs);
    }

    public synchronized void deleteJob(String id) {
        List<Job> jobs = getAllJobs();
        jobs.removeIf(j -> j.id.equals(id));
        writeList("jobs.json", jobs);
    }

    // ==================== Applications ====================

    public synchronized List<Application> getAllApplications() {
        return readList("applications.json", new TypeToken<List<Application>>(){}.getType());
    }

    public synchronized Application getApplicationById(String id) {
        return getAllApplications().stream().filter(a -> a.id.equals(id)).findFirst().orElse(null);
    }

    public synchronized List<Application> getApplicationsByJob(String jobId) {
        return getAllApplications().stream().filter(a -> a.jobId.equals(jobId)).collect(Collectors.toList());
    }

    public synchronized List<Application> getApplicationsByApplicant(String applicantId) {
        return getAllApplications().stream().filter(a -> a.applicantId.equals(applicantId)).collect(Collectors.toList());
    }

    public synchronized Application addApplication(Application app) {
        List<Application> apps = getAllApplications();
        app.id = UUID.randomUUID().toString().substring(0, 8);
        app.createdAt = System.currentTimeMillis();
        app.updatedAt = app.createdAt;
        app.status = "PENDING";
        apps.add(app);
        writeList("applications.json", apps);
        return app;
    }

    public synchronized void updateApplication(Application app) {
        List<Application> apps = getAllApplications();
        app.updatedAt = System.currentTimeMillis();
        apps.removeIf(a -> a.id.equals(app.id));
        apps.add(app);
        writeList("applications.json", apps);
    }

    // ==================== Notifications ====================

    public synchronized List<Notification> getAllNotifications() {
        return readList("notifications.json", new TypeToken<List<Notification>>(){}.getType());
    }

    public synchronized List<Notification> getNotificationsByUser(String userId) {
        return getAllNotifications().stream().filter(n -> n.userId.equals(userId)).collect(Collectors.toList());
    }

    public synchronized Notification getNotificationById(String id) {
        return getAllNotifications().stream().filter(n -> n.id.equals(id)).findFirst().orElse(null);
    }

    public synchronized Notification addNotification(Notification n) {
        List<Notification> list = getAllNotifications();
        n.id = UUID.randomUUID().toString().substring(0, 8);
        n.createdAt = System.currentTimeMillis();
        list.add(n);
        writeList("notifications.json", list);
        return n;
    }

    public synchronized void updateNotification(Notification n) {
        List<Notification> list = getAllNotifications();
        list.removeIf(x -> x.id.equals(n.id));
        list.add(n);
        writeList("notifications.json", list);
    }

    // ==================== Audit Logs ====================

    public synchronized List<AuditLog> getAllAuditLogs() {
        return readList("audit_logs.json", new TypeToken<List<AuditLog>>(){}.getType());
    }

    public synchronized void addAuditLog(String userId, String username, String action, String detail) {
        List<AuditLog> logs = getAllAuditLogs();
        AuditLog log = new AuditLog();
        log.id = UUID.randomUUID().toString().substring(0, 8);
        log.userId = userId;
        log.username = username;
        log.action = action;
        log.detail = detail;
        log.createdAt = System.currentTimeMillis();
        logs.add(log);
        writeList("audit_logs.json", logs);
    }

    // ==================== Password Reset Requests ====================

    public synchronized List<PasswordResetRequest> getAllPasswordResets() {
        return readList("password_resets.json", new TypeToken<List<PasswordResetRequest>>(){}.getType());
    }

    public synchronized PasswordResetRequest getPasswordResetById(String id) {
        return getAllPasswordResets().stream().filter(r -> r.id.equals(id)).findFirst().orElse(null);
    }

    public synchronized PasswordResetRequest addPasswordReset(PasswordResetRequest req) {
        List<PasswordResetRequest> list = getAllPasswordResets();
        req.id = UUID.randomUUID().toString().substring(0, 8);
        req.createdAt = System.currentTimeMillis();
        req.status = "PENDING";
        list.add(req);
        writeList("password_resets.json", list);
        return req;
    }

    public synchronized void updatePasswordReset(PasswordResetRequest req) {
        List<PasswordResetRequest> list = getAllPasswordResets();
        list.removeIf(r -> r.id.equals(req.id));
        list.add(req);
        writeList("password_resets.json", list);
    }

    // ==================== Settings ====================

    public synchronized Map<String, String> getSettings() {
        Path file = dataDir.resolve("settings.json");
        if (!Files.exists(file)) return new HashMap<>();
        try {
            String json = Files.readString(file);
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> result = gson.fromJson(json, type);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) { return new HashMap<>(); }
    }

    public synchronized void updateSettings(Map<String, String> settings) {
        try { Files.writeString(dataDir.resolve("settings.json"), gson.toJson(settings)); }
        catch (IOException e) { e.printStackTrace(); }
    }

    // ==================== File Uploads ====================

    public String saveUpload(String fileName, byte[] data) throws IOException {
        String safeName = System.currentTimeMillis() + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Files.write(uploadsDir.resolve(safeName), data);
        return safeName;
    }

    public byte[] getUpload(String fileName) throws IOException {
        return Files.readAllBytes(uploadsDir.resolve(fileName));
    }

    public Path getUploadsDir() { return uploadsDir; }

    // ==================== Export ====================

    public String exportAllDataCsv() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== USERS ===\n");
        sb.append("ID,Username,Role,FullName,Email,Phone,Active,CreatedAt\n");
        for (User u : getAllUsers()) {
            sb.append(String.format("%s,%s,%s,%s,%s,%s,%s,%d\n",
                    u.id, u.username, u.role, safe(u.fullName), safe(u.email), safe(u.phone), u.active, u.createdAt));
        }

        sb.append("\n=== JOBS ===\n");
        sb.append("ID,Title,Type,CourseName,PostedBy,Quota,WeeklyHours,Status,CreatedAt\n");
        for (Job j : getAllJobs()) {
            sb.append(String.format("%s,%s,%s,%s,%s,%d,%.1f,%s,%d\n",
                    j.id, safe(j.title), j.type, safe(j.courseName), j.postedBy, j.quota, j.weeklyHours, j.status, j.createdAt));
        }

        sb.append("\n=== APPLICATIONS ===\n");
        sb.append("ID,JobId,ApplicantId,Status,Priority,CvFile,CreatedAt,UpdatedAt\n");
        for (Application a : getAllApplications()) {
            sb.append(String.format("%s,%s,%s,%s,%d,%s,%d,%d\n",
                    a.id, a.jobId, a.applicantId, a.status, a.priority, safe(a.cvFileName), a.createdAt, a.updatedAt));
        }

        return sb.toString();
    }

    private String safe(String s) { return s == null ? "" : s.replace(",", ";"); }

    // ==================== Internal ====================

    private <T> List<T> readList(String filename, Type type) {
        Path file = dataDir.resolve(filename);
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            String json = Files.readString(file);
            List<T> result = gson.fromJson(json, type);
            return result != null ? new ArrayList<>(result) : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private <T> void writeList(String filename, List<T> data) {
        try { Files.writeString(dataDir.resolve(filename), gson.toJson(data)); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private void initDefaultData() {
        if (!Files.exists(dataDir.resolve("users.json"))) {
            User admin = new User("admin001", "admin", "admin123", "ADMIN");
            admin.fullName = "System Administrator";
            admin.email = "admin@bupt.edu.cn";
            writeList("users.json", List.of(admin));
        }
        String[] emptyLists = {"jobs.json", "applications.json", "notifications.json", "audit_logs.json", "password_resets.json"};
        for (String f : emptyLists) {
            if (!Files.exists(dataDir.resolve(f))) writeList(f, new ArrayList<>());
        }
    }
}
