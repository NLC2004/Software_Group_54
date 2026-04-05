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
        // Ensure seeded data exists even if JSON files already exist.
        // We do incremental upsert by ID to avoid deleting existing data.
        boolean usersChanged = false;
        List<User> users = getAllUsers();

        java.util.function.Function<String, User> findUserById = (uid) -> {
            for (User u : users) {
                if (u != null && u.id != null && u.id.equals(uid)) return u;
            }
            return null;
        };

        java.util.function.BiFunction<String, String, User> findUserByRoleAndStudentId = (role, sid) -> {
            for (User u : users) {
                if (u == null) continue;
                if (u.role != null && u.role.equals(role) && u.studentId != null && u.studentId.equals(sid)) return u;
            }
            return null;
        };

        java.util.function.BiFunction<String, String, User> findUserByRoleAndEmail = (role, email) -> {
            for (User u : users) {
                if (u == null) continue;
                if (u.role != null && u.role.equals(role) && u.email != null && u.email.equalsIgnoreCase(email)) return u;
            }
            return null;
        };

        java.util.function.Function<User, Boolean> upsertUser = (seed) -> {
            User existing = findUserById.apply(seed.id);
            if (existing == null && seed.studentId != null && !seed.studentId.isBlank()) {
                existing = findUserByRoleAndStudentId.apply(seed.role, seed.studentId);
            }
            if (existing == null && seed.email != null && !seed.email.isBlank()) {
                existing = findUserByRoleAndEmail.apply(seed.role, seed.email);
            }

            if (existing == null) {
                users.add(seed);
                return true;
            }

            boolean changed = false;
            // Do not overwrite password for existing accounts.
            if ((existing.username == null || existing.username.isBlank()) && seed.username != null) {
                existing.username = seed.username;
                changed = true;
            }
            if ((existing.fullName == null || existing.fullName.isBlank()) && seed.fullName != null) {
                existing.fullName = seed.fullName;
                changed = true;
            }
            if ((existing.email == null || existing.email.isBlank()) && seed.email != null) {
                existing.email = seed.email;
                changed = true;
            }
            if ((existing.studentId == null || existing.studentId.isBlank()) && seed.studentId != null) {
                existing.studentId = seed.studentId;
                changed = true;
            }
            if (existing.role == null || existing.role.isBlank()) {
                existing.role = seed.role;
                changed = true;
            }
            if (!existing.active) {
                existing.active = true;
                changed = true;
            }
            if (existing.createdAt == 0) {
                existing.createdAt = seed.createdAt;
                changed = true;
            }
            return changed;
        };

        User superAdmin = new User("admin001", "admin", "admin123", "ADMIN");
        superAdmin.fullName = "System Administrator";
        superAdmin.email = "admin@bupt.edu.cn";
        superAdmin.studentId = "admin";
        usersChanged |= upsertUser.apply(superAdmin);

        for (int i = 1; i <= 3; i++) {
            String adminId = "admin" + i;
            User subAdmin = new User("admin00" + (i + 1), adminId, "admin123", "ADMIN");
            subAdmin.fullName = "Administrator " + i;
            subAdmin.email = adminId + "@bupt.edu.cn";
            subAdmin.studentId = adminId;
            usersChanged |= upsertUser.apply(subAdmin);
        }

        String[][] taSeed = new String[][]{
            {"231225731", "Zijie Zhang"},
            {"231225270", "Zijun Song"},
            {"231225672", "Siying Li"},
            {"231225557", "Lingxiang Mei"},
            {"231225339", "Lechen Ning"},
            {"231225649", "Zhenkun Li"}
        };
        for (int i = 0; i < taSeed.length; i++) {
            String qm = taSeed[i][0];
            String fullName = taSeed[i][1];
            User ta = new User(String.format("ta%03d", i + 1), qm, "123456", "TA");
            ta.fullName = fullName;
            ta.studentId = qm;
            ta.email = qm + "@bupt.edu.cn";
            usersChanged |= upsertUser.apply(ta);
        }

        String[] moLastNames = new String[]{"Zhang", "Song", "Li", "Mei", "Ning", "Li"};
        for (int i = 0; i < 6; i++) {
            String teacherId = String.format("teacher%02d", i + 1);
            User mo = new User(String.format("mo%03d", i + 1), teacherId, "123456", "MO");
            mo.fullName = moLastNames[i];
            mo.studentId = teacherId;
            mo.email = teacherId + "@bupt.edu.cn";
            usersChanged |= upsertUser.apply(mo);
        }

        if (usersChanged) writeList("users.json", users);

        boolean jobsChanged = false;
        List<Job> jobs = getAllJobs();
        java.util.function.Function<String, Job> findJobById = (jid) -> {
            for (Job j : jobs) {
                if (j != null && j.id != null && j.id.equals(jid)) return j;
            }
            return null;
        };

        java.util.function.Function<Job, Boolean> upsertJob = (seed) -> {
            Job existing = findJobById.apply(seed.id);
            if (existing == null) {
                jobs.add(seed);
                return true;
            }
            return false;
        };

        if (findJobById.apply("job001") == null) {

            Job j1 = new Job();
            j1.id = "job001";
            j1.postedBy = "mo001";
            j1.title = "TA Needed: Programming Fundamentals";
            j1.type = "COURSE";
            j1.courseName = "Programming Fundamentals";
            j1.description = "Support tutorials, grading and office hours.";
            j1.requirements = List.of("Strong Java basics", "Good communication", "Available weekly");
            j1.quota = 2;
            j1.schedule = "Mon 18:00-20:00";
            j1.weeklyHours = 6;
            j1.deadline = "2026-05-01";
            j1.status = "OPEN";
            j1.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 5;
            jobsChanged |= upsertJob.apply(j1);

            Job j2 = new Job();
            j2.id = "job002";
            j2.postedBy = "mo002";
            j2.title = "TA Needed: Data Structures";
            j2.type = "COURSE";
            j2.courseName = "Data Structures";
            j2.description = "Help with labs and assignments.";
            j2.requirements = List.of("Solid DS/Algo", "Responsible", "Teamwork");
            j2.quota = 1;
            j2.schedule = "Wed 14:00-16:00";
            j2.weeklyHours = 5;
            j2.deadline = "2026-05-05";
            j2.status = "OPEN";
            j2.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 4;
            jobsChanged |= upsertJob.apply(j2);

            Job j3 = new Job();
            j3.id = "job003";
            j3.postedBy = "mo003";
            j3.title = "TA Needed: Database Systems";
            j3.type = "COURSE";
            j3.courseName = "Database Systems";
            j3.description = "Assist with SQL labs and project support.";
            j3.requirements = List.of("SQL proficiency", "Patience", "Clear explanation");
            j3.quota = 2;
            j3.schedule = "Thu 10:00-12:00";
            j3.weeklyHours = 4;
            j3.deadline = "2026-05-10";
            j3.status = "OPEN";
            j3.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 3;
            jobsChanged |= upsertJob.apply(j3);

            Job j4 = new Job();
            j4.id = "job004";
            j4.postedBy = "mo004";
            j4.title = "TA Needed: Software Engineering Project";
            j4.type = "ACTIVITY";
            j4.courseName = "SE Project";
            j4.description = "Mentor team projects and demos.";
            j4.requirements = List.of("Project experience", "Mentoring", "Time management");
            j4.quota = 1;
            j4.schedule = "Fri 16:00-18:00";
            j4.weeklyHours = 3;
            j4.deadline = "2026-04-25";
            j4.status = "CLOSED";
            j4.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 10;
            jobsChanged |= upsertJob.apply(j4);

            Job j5 = new Job();
            j5.id = "job005";
            j5.postedBy = "mo005";
            j5.title = "TA Needed: Computer Networks";
            j5.type = "COURSE";
            j5.courseName = "Computer Networks";
            j5.description = "Assist with network labs and Q&A.";
            j5.requirements = List.of("Basic networking", "Hands-on", "Reliable");
            j5.quota = 1;
            j5.schedule = "Tue 08:00-10:00";
            j5.weeklyHours = 4;
            j5.deadline = "2026-05-15";
            j5.status = "OPEN";
            j5.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 2;
            jobsChanged |= upsertJob.apply(j5);

            Job j6 = new Job();
            j6.id = "job006";
            j6.postedBy = "mo006";
            j6.title = "TA Needed: AI Basics";
            j6.type = "COURSE";
            j6.courseName = "AI Basics";
            j6.description = "Help with homework review and tutorials.";
            j6.requirements = List.of("ML fundamentals", "Python basics", "Communication");
            j6.quota = 2;
            j6.schedule = "Sat 09:00-11:00";
            j6.weeklyHours = 5;
            j6.deadline = "2026-05-20";
            j6.status = "OPEN";
            j6.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24;
            jobsChanged |= upsertJob.apply(j6);
        }

        if (jobsChanged) writeList("jobs.json", jobs);

        boolean appsChanged = false;
        List<Application> apps = getAllApplications();
        java.util.function.Function<String, Application> findAppById = (aid) -> {
            for (Application a : apps) {
                if (a != null && a.id != null && a.id.equals(aid)) return a;
            }
            return null;
        };

        java.util.function.Function<Application, Boolean> upsertApp = (seed) -> {
            Application existing = findAppById.apply(seed.id);
            if (existing == null) {
                apps.add(seed);
                return true;
            }
            return false;
        };

        if (findAppById.apply("app001") == null) {

            Application a1 = new Application();
            a1.id = "app001";
            a1.jobId = "job001";
            a1.applicantId = "ta001";
            a1.priority = 1;
            a1.cvFileName = "cv_ta001.pdf";
            a1.coverLetter = "Interested in teaching and helping peers.";
            a1.status = "PENDING";
            a1.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 12;
            a1.updatedAt = a1.createdAt;
            appsChanged |= upsertApp.apply(a1);

            Application a2 = new Application();
            a2.id = "app002";
            a2.jobId = "job001";
            a2.applicantId = "ta002";
            a2.priority = 2;
            a2.cvFileName = "cv_ta002.pdf";
            a2.coverLetter = "Have experience as course helper.";
            a2.status = "APPROVED";
            a2.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 30;
            a2.updatedAt = System.currentTimeMillis() - 1000L * 60 * 60 * 20;
            appsChanged |= upsertApp.apply(a2);

            Application a3 = new Application();
            a3.id = "app003";
            a3.jobId = "job002";
            a3.applicantId = "ta003";
            a3.priority = 1;
            a3.cvFileName = "cv_ta003.pdf";
            a3.coverLetter = "Strong DS/Algo, can assist labs.";
            a3.status = "REJECTED";
            a3.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 50;
            a3.updatedAt = System.currentTimeMillis() - 1000L * 60 * 60 * 40;
            appsChanged |= upsertApp.apply(a3);

            Application a4 = new Application();
            a4.id = "app004";
            a4.jobId = "job003";
            a4.applicantId = "ta004";
            a4.priority = 3;
            a4.cvFileName = "cv_ta004.pdf";
            a4.coverLetter = "I like database and can help with SQL labs.";
            a4.status = "WITHDRAWN";
            a4.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 36;
            a4.updatedAt = System.currentTimeMillis() - 1000L * 60 * 60 * 10;
            appsChanged |= upsertApp.apply(a4);

            Application a5 = new Application();
            a5.id = "app005";
            a5.jobId = "job006";
            a5.applicantId = "ta005";
            a5.priority = 1;
            a5.cvFileName = "cv_ta005.pdf";
            a5.coverLetter = "Familiar with Python and ML basics.";
            a5.status = "PENDING";
            a5.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 6;
            a5.updatedAt = a5.createdAt;
            appsChanged |= upsertApp.apply(a5);

            Application a6 = new Application();
            a6.id = "app006";
            a6.jobId = "job005";
            a6.applicantId = "ta006";
            a6.priority = 2;
            a6.cvFileName = "cv_ta006.pdf";
            a6.coverLetter = "Hands-on labs and networking basics.";
            a6.status = "APPROVED";
            a6.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 70;
            a6.updatedAt = System.currentTimeMillis() - 1000L * 60 * 60 * 60;
            appsChanged |= upsertApp.apply(a6);
        }

        if (appsChanged) writeList("applications.json", apps);

        boolean notificationsChanged = false;
        List<Notification> ns = getAllNotifications();
        java.util.function.Function<String, Notification> findNotificationById = (nid) -> {
            for (Notification n : ns) {
                if (n != null && n.id != null && n.id.equals(nid)) return n;
            }
            return null;
        };

        java.util.function.Function<Notification, Boolean> upsertNotification = (seed) -> {
            Notification existing = findNotificationById.apply(seed.id);
            if (existing == null) {
                ns.add(seed);
                return true;
            }
            return false;
        };

        if (findNotificationById.apply("noti001") == null) {

            Notification n1 = new Notification();
            n1.id = "noti001";
            n1.userId = "mo001";
            n1.title = "New Application";
            n1.content = "Zijie Zhang applied for TA Needed: Programming Fundamentals";
            n1.type = "APPLICATION";
            n1.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 12;
            notificationsChanged |= upsertNotification.apply(n1);

            Notification n2 = new Notification();
            n2.id = "noti002";
            n2.userId = "ta002";
            n2.title = "Application Approved";
            n2.content = "Your application for TA Needed: Programming Fundamentals has been approved!";
            n2.type = "APPLICATION";
            n2.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 20;
            notificationsChanged |= upsertNotification.apply(n2);

            Notification n3 = new Notification();
            n3.id = "noti003";
            n3.userId = "ta003";
            n3.title = "Application Rejected";
            n3.content = "Your application for TA Needed: Data Structures has been rejected.";
            n3.type = "APPLICATION";
            n3.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 40;
            notificationsChanged |= upsertNotification.apply(n3);

            Notification n4 = new Notification();
            n4.id = "noti004";
            n4.userId = "ta004";
            n4.title = "Application Withdrawn";
            n4.content = "You withdrew your application for TA Needed: Database Systems.";
            n4.type = "APPLICATION";
            n4.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 10;
            notificationsChanged |= upsertNotification.apply(n4);
        }

        if (notificationsChanged) writeList("notifications.json", ns);

        String[] emptyLists = {"audit_logs.json", "password_resets.json"};
        for (String f : emptyLists) {
            if (!Files.exists(dataDir.resolve(f))) writeList(f, new ArrayList<>());
        }
    }
}
