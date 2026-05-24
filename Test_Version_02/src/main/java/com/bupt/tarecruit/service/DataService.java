package com.bupt.tarecruit.service;

import com.bupt.tarecruit.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents the data service component of the TA recruitment system.
 */
public class DataService {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataDir;
    private final Path uploadsDir;
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final MailService mailService;

    /**
     * Creates a new data service instance.
     */
    public DataService(String baseDir) throws IOException {
        this.dataDir = Paths.get(baseDir, "data");
        this.uploadsDir = Paths.get(baseDir, "uploads");
        Files.createDirectories(dataDir);
        Files.createDirectories(uploadsDir);
        this.mailService = new MailService(getSettings());
        initDefaultData();
        reconcileApplicationPriorities();
        reconcileNotifications();
    }

    // ==================== Sessions ====================

    /**
     * Handles the create session operation.
     */
    public String createSession(String userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, userId);
        return token;
    }

    /**
     * Handles the remove session operation.
     */
    public void removeSession(String token) { sessions.remove(token); }

    /**
     * Handles the get session user operation.
     */
    public User getSessionUser(String token) {
        String userId = sessions.get(token);
        return userId == null ? null : getUserById(userId);
    }

    // ==================== Users ====================

    /**
     * Handles the get all users operation.
     */
    public synchronized List<User> getAllUsers() {
        return readList("users.json", new TypeToken<List<User>>(){}.getType());
    }

    /**
     * Handles the get user by id operation.
     */
    public synchronized User getUserById(String id) {
        return getAllUsers().stream().filter(u -> u.id.equals(id)).findFirst().orElse(null);
    }

    /**
     * Handles the get user by username operation.
     */
    public synchronized User getUserByUsername(String username) {
        return getUserByUsername(username, null);
    }

    /**
     * Handles the get user by username operation.
     */
    public synchronized User getUserByUsername(String username, String role) {
        if (username == null) return null;
        String roleFilter = normalizeRoleFilter(role);
        return getAllUsers().stream()
                .filter(u -> u.username != null && u.username.equals(username))
                .filter(u -> matchesRoleFilter(u, roleFilter))
                .findFirst()
                .orElse(null);
    }

    /**
     * Handles the get user by student id or email operation.
     */
    public synchronized User getUserByStudentIdOrEmail(String identifier) {
        return getUserByStudentIdOrEmail(identifier, null);
    }

    /**
     * Handles the get user by student id or email operation.
     */
    public synchronized User getUserByStudentIdOrEmail(String identifier, String role) {
        String normalized = identifier == null ? "" : identifier.trim();
        if (normalized.isEmpty()) return null;
        String roleFilter = normalizeRoleFilter(role);
        return getAllUsers().stream()
                .filter(u -> matchesRoleFilter(u, roleFilter))
                .filter(u -> normalized.equalsIgnoreCase(u.studentId == null ? "" : u.studentId.trim())
                        || normalized.equalsIgnoreCase(u.email == null ? "" : u.email.trim()))
                .findFirst()
                .orElse(null);
    }

    /** Resolve login identifier (student/teacher ID, email, or username) scoped to a portal role. */
    /**
     * Handles the find user by portal identifier operation.
     */
    public synchronized User findUserByPortalIdentifier(String identifier, String role) {
        String normalized = identifier == null ? "" : identifier.trim();
        if (normalized.isEmpty()) return null;
        User byIdOrEmail = getUserByStudentIdOrEmail(normalized, role);
        if (byIdOrEmail != null) return byIdOrEmail;
        return getUserByUsername(normalized, role);
    }

    /** Match student/teacher ID or username, optionally scoped by role (TA/MO). */
    /**
     * Handles the find user by id number operation.
     */
    public synchronized User findUserByIdNumber(String idNumber, String role) {
        String normalized = idNumber == null ? "" : idNumber.trim();
        if (normalized.isEmpty()) return null;
        String roleFilter = normalizeRoleFilter(role);
        return getAllUsers().stream()
                .filter(u -> matchesRoleFilter(u, roleFilter))
                .filter(u -> normalized.equals(u.studentId) || normalized.equals(u.username))
                .findFirst()
                .orElse(null);
    }

    private static String normalizeRoleFilter(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean matchesRoleFilter(User user, String roleFilter) {
        if (roleFilter == null || roleFilter.isEmpty()) return true;
        return roleFilter.equalsIgnoreCase(user.role == null ? "" : user.role);
    }

    /**
     * Handles the add user operation.
     */
    public synchronized User addUser(User user) {
        List<User> users = getAllUsers();
        user.id = UUID.randomUUID().toString().substring(0, 8);
        user.createdAt = System.currentTimeMillis();
        users.add(user);
        writeList("users.json", users);
        return user;
    }

    /**
     * Handles the update user operation.
     */
    public synchronized void updateUser(User user) {
        List<User> users = getAllUsers();
        users.removeIf(u -> u.id.equals(user.id));
        users.add(user);
        writeList("users.json", users);
    }

    /**
     * Handles the delete user operation.
     */
    public synchronized void deleteUser(String id) {
        List<User> users = getAllUsers();
        users.removeIf(u -> u.id.equals(id));
        writeList("users.json", users);
    }

    // ==================== Jobs ====================

    /**
     * Handles the get all jobs operation.
     */
    public synchronized List<Job> getAllJobs() {
        return readList("jobs.json", new TypeToken<List<Job>>(){}.getType());
    }

    /**
     * Handles the get job by id operation.
     */
    public synchronized Job getJobById(String id) {
        return getAllJobs().stream().filter(j -> j.id.equals(id)).findFirst().orElse(null);
    }

    /**
     * Handles the add job operation.
     */
    public synchronized Job addJob(Job job) {
        List<Job> jobs = getAllJobs();
        job.id = UUID.randomUUID().toString().substring(0, 8);
        job.createdAt = System.currentTimeMillis();
        job.status = "OPEN";
        jobs.add(job);
        writeList("jobs.json", jobs);
        return job;
    }

    /**
     * Handles the update job operation.
     */
    public synchronized void updateJob(Job job) {
        List<Job> jobs = getAllJobs();
        jobs.removeIf(j -> j.id.equals(job.id));
        jobs.add(job);
        writeList("jobs.json", jobs);
    }

    /**
     * Handles the delete job operation.
     */
    public synchronized void deleteJob(String id) {
        List<Job> jobs = getAllJobs();
        jobs.removeIf(j -> j.id.equals(id));
        writeList("jobs.json", jobs);
    }

    // ==================== Applications ====================

    /**
     * Handles the get all applications operation.
     */
    public synchronized List<Application> getAllApplications() {
        return readList("applications.json", new TypeToken<List<Application>>(){}.getType());
    }

    /**
     * Handles the get application by id operation.
     */
    public synchronized Application getApplicationById(String id) {
        return getAllApplications().stream().filter(a -> a.id.equals(id)).findFirst().orElse(null);
    }

    /**
     * Handles the get applications by job operation.
     */
    public synchronized List<Application> getApplicationsByJob(String jobId) {
        return getAllApplications().stream().filter(a -> a.jobId.equals(jobId)).collect(Collectors.toList());
    }

    /**
     * Handles the get applications by applicant operation.
     */
    public synchronized List<Application> getApplicationsByApplicant(String applicantId) {
        return getAllApplications().stream().filter(a -> a.applicantId.equals(applicantId)).collect(Collectors.toList());
    }

    /**
     * Handles the get approved application count for job operation.
     */
    public synchronized long getApprovedApplicationCountForJob(String jobId) {
        return getApplicationsByJob(jobId).stream().filter(a -> "APPROVED".equals(a.status)).count();
    }

    /**
     * Handles the add application operation.
     */
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

    /**
     * Handles the update application operation.
     */
    public synchronized void updateApplication(Application app) {
        List<Application> apps = getAllApplications();
        app.updatedAt = System.currentTimeMillis();
        apps.removeIf(a -> a.id.equals(app.id));
        apps.add(app);
        writeList("applications.json", apps);
    }

    /**
     * Handles the delete applications by job operation.
     */
    public synchronized int deleteApplicationsByJob(String jobId) {
        List<Application> apps = getAllApplications();
        int before = apps.size();
        apps.removeIf(a -> Objects.equals(a.jobId, jobId));
        writeList("applications.json", apps);
        return before - apps.size();
    }

    /**
     * Handles the reconcile application priorities operation.
     */
    public synchronized int reconcileApplicationPriorities() {
        List<Application> apps = getAllApplications();
        int changed = 0;
        Map<String, List<Application>> byApplicant = apps.stream()
                .filter(a -> a.applicantId != null && isActiveApplicationStatus(a.status))
                .collect(Collectors.groupingBy(a -> a.applicantId));

        long now = System.currentTimeMillis();
        for (List<Application> applicantApps : byApplicant.values()) {
            applicantApps.sort(Comparator
                    .comparingInt((Application a) -> normalizePriority(a.priority))
                    .thenComparingLong(a -> a.createdAt));
            Set<Integer> usedPriorities = new HashSet<>();
            for (Application app : applicantApps) {
                int priority = normalizePriority(app.priority);
                if (priority < 1 || priority > 3 || usedPriorities.contains(priority)) {
                    app.status = "WITHDRAWN";
                    app.updatedAt = now;
                    changed++;
                    continue;
                }
                usedPriorities.add(priority);
            }
        }
        if (changed > 0) writeList("applications.json", apps);
        return changed;
    }

    private boolean isActiveApplicationStatus(String status) {
        return !"WITHDRAWN".equals(status) && !"REJECTED".equals(status);
    }

    private int normalizePriority(int priority) {
        return priority >= 1 && priority <= 3 ? priority : 99;
    }

    /**
     * Handles the get weekly workload limit operation.
     */
    public synchronized double getWeeklyWorkloadLimit() {
        Map<String, String> settings = getSettings();
        double maxHours = 20;
        try {
            if (settings.containsKey("maxWeeklyHours")) {
                maxHours = Double.parseDouble(settings.get("maxWeeklyHours"));
            }
        } catch (NumberFormatException ignored) {}
        return maxHours;
    }

    /**
     * Handles the get job weekly hours operation.
     */
    public synchronized Map<Integer, Double> getJobWeeklyHours(Job job) {
        if (job == null) return Collections.emptyMap();
        String type = job.type == null ? "" : job.type.trim().toUpperCase(Locale.ROOT);
        if ("COURSE".equals(type)) type = "COURSE_TA";
        if ("ACTIVITY".equals(type)) type = "CLASS_TEST_TA";

        if ("FINAL_EXAM_TA".equals(type)) {
            if (job.examDuration <= 0) return Collections.emptyMap();
            return Map.of(0, job.examDuration);
        }

        String scheduleJson = null;
        if (job.courseScheduleGrid != null && !job.courseScheduleGrid.trim().isEmpty()) {
            scheduleJson = job.courseScheduleGrid;
        } else if (job.labSessions != null && !job.labSessions.trim().isEmpty()) {
            scheduleJson = job.labSessions;
        } else if (job.testScheduleDetail != null && !job.testScheduleDetail.trim().isEmpty()) {
            scheduleJson = job.testScheduleDetail;
        }
        Map<Integer, Double> weekly = parseWeeklyHoursFromScheduleEntriesJson(scheduleJson);
        if (weekly.isEmpty() && job.weeklyHours > 0) {
            Map<Integer, Double> fallback = new TreeMap<>();
            int start = job.courseWeekStart > 0 ? job.courseWeekStart : 1;
            int end = job.courseWeekEnd >= start ? job.courseWeekEnd : start;
            for (int week = start; week <= end; week++) {
                fallback.put(week, job.weeklyHours);
            }
            return fallback;
        }
        return weekly;
    }

    /**
     * Handles the get ta weekly hours operation.
     */
    public synchronized Map<Integer, Double> getTAWeeklyHours(String taId) {
        Map<Integer, Double> weekly = new TreeMap<>();
        if (taId == null || taId.isBlank()) return weekly;

        for (Application app : getAllApplications()) {
            if (!taId.equals(app.applicantId) || !"APPROVED".equals(app.status)) continue;
            Job job = getJobById(app.jobId);
            mergeWeeklyHours(weekly, getJobWeeklyHours(job));
        }
        return weekly;
    }

    /**
     * Handles the get overloaded weeks operation.
     */
    public synchronized Set<Integer> getOverloadedWeeks(String taId) {
        double maxHours = getWeeklyWorkloadLimit();
        Set<Integer> overloaded = new TreeSet<>();
        for (Map.Entry<Integer, Double> e : getTAWeeklyHours(taId).entrySet()) {
            double hours = e.getValue() == null ? 0 : e.getValue();
            if (hours > maxHours) overloaded.add(e.getKey());
        }
        return overloaded;
    }

    /**
     * Handles the would exceed weekly workload operation.
     */
    public synchronized boolean wouldExceedWeeklyWorkload(String taId, Job job) {
        if (taId == null || job == null) return false;
        double limit = getWeeklyWorkloadLimit();
        Map<Integer, Double> current = getTAWeeklyHours(taId);
        Map<Integer, Double> add = getJobWeeklyHours(job);
        for (Map.Entry<Integer, Double> e : add.entrySet()) {
            int week = e.getKey();
            double future = current.getOrDefault(week, 0.0) + (e.getValue() == null ? 0 : e.getValue());
            if (future > limit) return true;
        }
        return false;
    }

    /**
     * Handles the get weeks that would exceed weekly workload operation.
     */
    public synchronized List<Integer> getWeeksThatWouldExceedWeeklyWorkload(String taId, Job job) {
        if (taId == null || job == null) return Collections.emptyList();
        double limit = getWeeklyWorkloadLimit();
        Map<Integer, Double> current = getTAWeeklyHours(taId);
        Map<Integer, Double> add = getJobWeeklyHours(job);
        List<Integer> weeks = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : add.entrySet()) {
            int week = e.getKey();
            double future = current.getOrDefault(week, 0.0) + (e.getValue() == null ? 0 : e.getValue());
            if (future > limit) weeks.add(week);
        }
        return weeks;
    }

    private void mergeWeeklyHours(Map<Integer, Double> acc, Map<Integer, Double> add) {
        if (acc == null || add == null) return;
        for (Map.Entry<Integer, Double> e : add.entrySet()) {
            int week = e.getKey();
            double hours = e.getValue() == null ? 0 : e.getValue();
            if (hours <= 0) continue;
            acc.put(week, acc.getOrDefault(week, 0.0) + hours);
        }
    }

    private Map<Integer, Double> parseWeeklyHoursFromScheduleEntriesJson(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyMap();
        try {
            JsonElement el = gson.fromJson(json, JsonElement.class);
            if (el == null || !el.isJsonArray()) return Collections.emptyMap();
            JsonArray arr = el.getAsJsonArray();

            Map<Integer, Double> out = new TreeMap<>();
            for (JsonElement one : arr) {
                if (one == null || !one.isJsonObject()) continue;
                JsonObject obj = one.getAsJsonObject();
                int week = obj.has("week") ? obj.get("week").getAsInt() : 0;
                if (!obj.has("selection") || !obj.get("selection").isJsonObject()) continue;
                JsonObject sel = obj.getAsJsonObject("selection");

                int periods = 0;
                for (String day : List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) {
                    if (!sel.has(day) || !sel.get(day).isJsonArray()) continue;
                    periods += sel.getAsJsonArray(day).size();
                }
                double hours = periods * 0.75;
                if (hours <= 0) continue;
                out.put(week, out.getOrDefault(week, 0.0) + hours);
            }
            return out;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }


    /**
     * Handles the get all application drafts operation.
     */
    public synchronized List<ApplicationDraft> getAllApplicationDrafts() {
        return readList("application_drafts.json", new TypeToken<List<ApplicationDraft>>(){}.getType());
    }

    /**
     * Handles the get application draft operation.
     */
    public synchronized ApplicationDraft getApplicationDraft(String userId, String jobId) {
        String normalizedJobId = jobId == null ? "" : jobId.trim();
        return getAllApplicationDrafts().stream()
                .filter(d -> Objects.equals(d.userId, userId)
                        && Objects.equals(d.jobId == null ? "" : d.jobId.trim(), normalizedJobId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Handles the save application draft operation.
     */
    public synchronized ApplicationDraft saveApplicationDraft(ApplicationDraft draft) {
        List<ApplicationDraft> drafts = getAllApplicationDrafts();
        long now = System.currentTimeMillis();
        if (draft.id == null || draft.id.isBlank()) {
            draft.id = UUID.randomUUID().toString().substring(0, 8);
            draft.createdAt = now;
        }
        draft.updatedAt = now;
        drafts.removeIf(d -> Objects.equals(d.userId, draft.userId)
                && Objects.equals(d.jobId == null ? "" : d.jobId.trim(), draft.jobId == null ? "" : draft.jobId.trim()));
        drafts.add(draft);
        writeList("application_drafts.json", drafts);
        return draft;
    }

    /**
     * Handles the delete application draft operation.
     */
    public synchronized void deleteApplicationDraft(String userId, String jobId) {
        List<ApplicationDraft> drafts = getAllApplicationDrafts();
        String normalizedJobId = jobId == null ? "" : jobId.trim();
        drafts.removeIf(d -> Objects.equals(d.userId, userId)
                && Objects.equals(d.jobId == null ? "" : d.jobId.trim(), normalizedJobId));
        writeList("application_drafts.json", drafts);
    }

    /**
     * Handles the delete application drafts by job operation.
     */
    public synchronized int deleteApplicationDraftsByJob(String jobId) {
        List<ApplicationDraft> drafts = getAllApplicationDrafts();
        String normalizedJobId = jobId == null ? "" : jobId.trim();
        int before = drafts.size();
        drafts.removeIf(d -> Objects.equals(d.jobId == null ? "" : d.jobId.trim(), normalizedJobId));
        writeList("application_drafts.json", drafts);
        return before - drafts.size();
    }

    // ==================== Notifications ====================

    /**
     * Handles the get all notifications operation.
     */
    public synchronized List<Notification> getAllNotifications() {
        return readList("notifications.json", new TypeToken<List<Notification>>(){}.getType());
    }

    /**
     * Handles the get notifications by user operation.
     */
    public synchronized List<Notification> getNotificationsByUser(String userId) {
        return getAllNotifications().stream().filter(n -> n.userId.equals(userId)).collect(Collectors.toList());
    }

    /**
     * Handles the get notification by id operation.
     */
    public synchronized Notification getNotificationById(String id) {
        return getAllNotifications().stream().filter(n -> n.id.equals(id)).findFirst().orElse(null);
    }

    /**
     * Handles the add notification operation.
     */
    public synchronized Notification addNotification(Notification n) {
        List<Notification> list = getAllNotifications();
        n.id = UUID.randomUUID().toString().substring(0, 8);
        n.createdAt = System.currentTimeMillis();
        list.add(n);
        writeList("notifications.json", list);
        return n;
    }

    /**
     * Handles the update notification operation.
     */
    public synchronized void updateNotification(Notification n) {
        List<Notification> list = getAllNotifications();
        list.removeIf(x -> x.id.equals(n.id));
        list.add(n);
        writeList("notifications.json", list);
    }

    /**
     * Handles the delete notifications related to job operation.
     */
    public synchronized int deleteNotificationsRelatedToJob(Job job) {
        if (job == null || job.title == null || job.title.trim().isEmpty()) return 0;
        String title = job.title.trim();
        List<Notification> list = getAllNotifications();
        int before = list.size();
        list.removeIf(n -> "APPLICATION".equals(n.type)
                && n.content != null
                && n.content.contains(title));
        writeList("notifications.json", list);
        return before - list.size();
    }

    /**
     * Handles the reconcile notifications operation.
     */
    public synchronized void reconcileNotifications() {
        List<Notification> list = getAllNotifications();
        int before = list.size();

        for (Application app : getAllApplications()) {
            Job job = getJobById(app.jobId);
            User applicant = getUserById(app.applicantId);
            if (job == null || applicant == null) continue;

            String applicantName = applicant.fullName != null && !applicant.fullName.isBlank()
                    ? applicant.fullName : applicant.username;
            addNotificationIfMissing(list, job.postedBy, "New Application",
                    applicantName + " applied for " + job.title,
                    "APPLICATION", app.createdAt);

            if ("APPROVED".equals(app.status)) {
                addNotificationIfMissing(list, app.applicantId, "Application Approved",
                        "Your application for " + job.title + " has been approved!",
                        "APPLICATION", app.updatedAt > 0 ? app.updatedAt : app.createdAt);
            } else if ("REJECTED".equals(app.status)) {
                addNotificationIfMissing(list, app.applicantId, "Application Rejected",
                        "Your application for " + job.title + " has been rejected.",
                        "APPLICATION", app.updatedAt > 0 ? app.updatedAt : app.createdAt);
            } else if ("WITHDRAWN".equals(app.status)) {
                addNotificationIfMissing(list, app.applicantId, "Application Withdrawn",
                        "You withdrew your application for " + job.title + ".",
                        "APPLICATION", app.updatedAt > 0 ? app.updatedAt : app.createdAt);
            }
        }

        for (User u : getAllUsers()) {
            if (!"MO".equals(u.role)) continue;
            addNotificationIfMissing(list, u.id, "Weekly Recruitment Reminder",
                    "Please review pending TA applications and close positions that have reached quota.",
                    "SYSTEM", System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 3);
            addNotificationIfMissing(list, u.id, "Admin Notice",
                    "The admin office will periodically review job postings, TA workload, and password reset requests.",
                    "SYSTEM", System.currentTimeMillis() - 1000L * 60 * 60 * 24);
        }

        if (list.size() != before) writeList("notifications.json", list);
    }

    private void addNotificationIfMissing(List<Notification> list, String userId, String title, String content, String type, long createdAt) {
        if (userId == null || userId.isBlank()) return;
        boolean exists = list.stream().anyMatch(n -> Objects.equals(n.userId, userId)
                && Objects.equals(n.title, title)
                && Objects.equals(n.content, content)
                && Objects.equals(n.type, type));
        if (exists) return;
        Notification n = new Notification();
        n.id = UUID.randomUUID().toString().substring(0, 8);
        n.userId = userId;
        n.title = title;
        n.content = content;
        n.type = type;
        n.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        list.add(n);
    }

    // ==================== Audit Logs ====================

    /**
     * Handles the get all audit logs operation.
     */
    public synchronized List<AuditLog> getAllAuditLogs() {
        return readList("audit_logs.json", new TypeToken<List<AuditLog>>(){}.getType());
    }

    /**
     * Handles the add audit log operation.
     */
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

    /**
     * Handles the get all password resets operation.
     */
    public synchronized List<PasswordResetRequest> getAllPasswordResets() {
        return readList("password_resets.json", new TypeToken<List<PasswordResetRequest>>(){}.getType());
    }

    /**
     * Handles the get password reset by id operation.
     */
    public synchronized PasswordResetRequest getPasswordResetById(String id) {
        return getAllPasswordResets().stream().filter(r -> r.id.equals(id)).findFirst().orElse(null);
    }

    /**
     * Handles the add password reset operation.
     */
    public synchronized PasswordResetRequest addPasswordReset(PasswordResetRequest req) {
        List<PasswordResetRequest> list = getAllPasswordResets();
        req.id = UUID.randomUUID().toString().substring(0, 8);
        req.createdAt = System.currentTimeMillis();
        req.status = "PENDING";
        list.add(req);
        writeList("password_resets.json", list);
        return req;
    }

    /**
     * Handles the update password reset operation.
     */
    public synchronized void updatePasswordReset(PasswordResetRequest req) {
        List<PasswordResetRequest> list = getAllPasswordResets();
        list.removeIf(r -> r.id.equals(req.id));
        list.add(req);
        writeList("password_resets.json", list);
    }

    // ==================== Admin Role Templates ====================

    /**
     * Handles the get all admin role templates operation.
     */
    public synchronized List<AdminRoleTemplate> getAllAdminRoleTemplates() {
        return readList("admin_role_templates.json", new TypeToken<List<AdminRoleTemplate>>(){}.getType());
    }

    /**
     * Handles the get admin role template by id operation.
     */
    public synchronized AdminRoleTemplate getAdminRoleTemplateById(String id) {
        return getAllAdminRoleTemplates().stream()
                .filter(t -> Objects.equals(t.id, id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Handles the add admin role template operation.
     */
    public synchronized AdminRoleTemplate addAdminRoleTemplate(AdminRoleTemplate template) {
        List<AdminRoleTemplate> list = getAllAdminRoleTemplates();
        template.id = UUID.randomUUID().toString().substring(0, 8);
        template.createdAt = System.currentTimeMillis();
        if (template.tags == null) template.tags = new ArrayList<>();
        list.add(template);
        writeList("admin_role_templates.json", list);
        return template;
    }

    /**
     * Handles the update admin role template operation.
     */
    public synchronized void updateAdminRoleTemplate(AdminRoleTemplate template) {
        List<AdminRoleTemplate> list = getAllAdminRoleTemplates();
        if (template.tags == null) template.tags = new ArrayList<>();
        list.removeIf(t -> Objects.equals(t.id, template.id));
        list.add(template);
        writeList("admin_role_templates.json", list);
    }

    /**
     * Handles the delete admin role template operation.
     */
    public synchronized void deleteAdminRoleTemplate(String id) {
        List<AdminRoleTemplate> list = getAllAdminRoleTemplates();
        list.removeIf(t -> Objects.equals(t.id, id));
        writeList("admin_role_templates.json", list);
    }

    // ==================== Export Tasks ====================

    /**
     * Handles the get all export tasks operation.
     */
    public synchronized List<ExportTask> getAllExportTasks() {
        return readList("export_tasks.json", new TypeToken<List<ExportTask>>(){}.getType());
    }

    /**
     * Handles the get export task by id operation.
     */
    public synchronized ExportTask getExportTaskById(String id) {
        return getAllExportTasks().stream()
                .filter(t -> Objects.equals(t.id, id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Handles the add export task operation.
     */
    public synchronized ExportTask addExportTask(ExportTask task) {
        List<ExportTask> list = getAllExportTasks();
        long now = System.currentTimeMillis();
        task.id = UUID.randomUUID().toString().substring(0, 8);
        task.createdAt = now;
        task.updatedAt = now;
        list.add(task);
        writeList("export_tasks.json", list);
        return task;
    }

    /**
     * Handles the update export task operation.
     */
    public synchronized void updateExportTask(ExportTask task) {
        List<ExportTask> list = getAllExportTasks();
        task.updatedAt = System.currentTimeMillis();
        list.removeIf(t -> Objects.equals(t.id, task.id));
        list.add(task);
        writeList("export_tasks.json", list);
    }

    /**
     * Handles the save backup file operation.
     */
    public synchronized String saveBackupFile(String fileName, byte[] data) throws IOException {
        String safeName = System.currentTimeMillis() + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Files.write(uploadsDir.resolve(safeName), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return safeName;
    }

    // ==================== Settings ====================

    /**
     * Handles the get settings operation.
     */
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

    /**
     * Handles the update settings operation.
     */
    public synchronized void updateSettings(Map<String, String> settings) {
        try {
            Files.writeString(dataDir.resolve("settings.json"), gson.toJson(settings));
            this.mailService.refresh(settings);
        }
        catch (IOException e) { e.printStackTrace(); }
    }


    /**
     * Handles the get mail service operation.
     */
    public MailService getMailService() {
        return mailService;
    }

    // ==================== File Uploads ====================

    /**
     * Handles the save upload operation.
     */
    public String saveUpload(String fileName, byte[] data) throws IOException {
        String safeName = System.currentTimeMillis() + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Files.write(uploadsDir.resolve(safeName), data);
        return safeName;
    }

    /**
     * Handles the get upload operation.
     */
    public byte[] getUpload(String fileName) throws IOException {
        return Files.readAllBytes(uploadsDir.resolve(fileName));
    }

    /**
     * Handles the get uploads dir operation.
     */
    public Path getUploadsDir() { return uploadsDir; }

    // ==================== Export ====================

    /**
     * Handles the export all data csv operation.
     */
    public String exportAllDataCsv() {
        return exportAllDataCsv(null, null);
    }

    /**
     * Handles the export all data csv operation.
     */
    public String exportAllDataCsv(Long startMs, Long endMs) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== USERS ===\n");
        sb.append("ID,Username,Role,FullName,Email,Phone,Active,CreatedAt\n");
        for (User u : getAllUsers().stream().filter(u -> inRange(u.createdAt, startMs, endMs)).collect(Collectors.toList())) {
            sb.append(String.format("%s,%s,%s,%s,%s,%s,%s,%d\n",
                    u.id, u.username, u.role, safe(u.fullName), safe(u.email), safe(u.phone), u.active, u.createdAt));
        }

        sb.append("\n=== JOBS ===\n");
        sb.append("ID,Title,Type,CourseName,PostedBy,Quota,WeeklyHours,Status,CreatedAt\n");
        for (Job j : getAllJobs().stream().filter(j -> inRange(j.createdAt, startMs, endMs)).collect(Collectors.toList())) {
            sb.append(String.format("%s,%s,%s,%s,%s,%d,%.1f,%s,%d\n",
                    j.id, safe(j.title), j.type, safe(j.courseName), j.postedBy, j.quota, j.weeklyHours, j.status, j.createdAt));
        }

        sb.append("\n=== APPLICATIONS ===\n");
        sb.append("ID,JobId,ApplicantId,Status,Priority,CvFile,CreatedAt,UpdatedAt\n");
        for (Application a : getAllApplications().stream().filter(a -> inRange(a.createdAt, startMs, endMs)).collect(Collectors.toList())) {
            sb.append(String.format("%s,%s,%s,%s,%d,%s,%d,%d\n",
                    a.id, a.jobId, a.applicantId, a.status, a.priority, safe(a.cvFileName), a.createdAt, a.updatedAt));
        }

        return sb.toString();
    }

    private String safe(String s) { return s == null ? "" : s.replace(",", ";"); }

    private boolean inRange(long value, Long startMs, Long endMs) {
        if (startMs != null && value < startMs) return false;
        if (endMs != null && value > endMs) return false;
        return true;
    }

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

        boolean usersChanged = false;
        List<User> users = getAllUsers();
        java.util.function.Function<String, User> findUserById = (uid) -> {
            for (User u : users) {
                if (u != null && u.id != null && u.id.equals(uid)) return u;
            }
            return null;
        };

        java.util.function.Function<User, Boolean> upsertUser = (seed) -> {
            User existing = findUserById.apply(seed.id);
            if (existing == null) {
                users.add(seed);
                return true;
            }
            return false;
        };

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
        String[] moAccountIds = new String[]{"231225731", "231225270", "231225672", "231225557", "231225339", "231225649"};
        for (int i = 0; i < 6; i++) {
            String teacherId = String.format("teacher%02d", i + 1);
            User mo = new User(String.format("mo%03d", i + 1), teacherId, "123456", "MO");
            mo.fullName = moLastNames[i];
            mo.studentId = moAccountIds[i];
            mo.email = teacherId + "@bupt.edu.cn";
            usersChanged |= upsertUser.apply(mo);
        }

        if (usersChanged) writeList("users.json", users);

        // Skip demo data reset if applications already exist (preserve user-generated data)
        if (Files.exists(dataDir.resolve("applications.json"))) {
            List<Application> existingApps = readList("applications.json", new TypeToken<List<Application>>(){}.getType());
            if (existingApps != null && !existingApps.isEmpty()) return;
        }

        // ==================== Demo Data Reset (Jobs/Applications/Notifications) ====================

        long now = System.currentTimeMillis();

        List<String> courseNamePresets = List.of(
                "Machine Learning",
                "Design and Build",
                "Personal Development Plan and Entrepreneurial Skills",
                "Operating Systems",
                "Communications and Networks",
                "Embedded Systems",
                "Cryptography and Cyber Security",
                "Software Engineering",
                "Sensors and Radio Frequency Identification",
                "Middleware",
                "Database Systems",
                "Data Structures",
                "Computer Networks",
                "Artificial Intelligence",
                "Human-Computer Interaction",
                "Computer Architecture",
                "Software Testing",
                "Web Development"
        );

        Map<String, List<String>> requirementPresets = new LinkedHashMap<>();
        requirementPresets.put("COURSE_TA", List.of(
                "I have taken the corresponding courses and have a solid grasp of the core knowledge points and syllabus.",
                "Possess good communication skills and can answer students' questions clearly.",
                "Work with a serious and responsible attitude and complete tasks on time.",
                "Able to assist in organizing teaching materials and grading assignments.",
                "Have patience and a strong sense of responsibility."
        ));
        requirementPresets.put("LAB_TA", List.of(
                "The corresponding experimental course has been taken.",
                "Proficient in experimental principles, operational procedures, and the use of relevant software/instruments.",
                "Capable of guiding students to complete experimental operations and answering lab questions.",
                "Able to standardize the marking of experimental reports."
        ));
        requirementPresets.put("FINAL_EXAM_TA", List.of(
                "Strictly abide by the school's regulations on the administration of final exams.",
                "Possess a strong sense of responsibility and confidentiality awareness.",
                "Arrive on time and stay throughout the entire process.",
                "Capable of handling unexpected situations according to regulations."
        ));
        requirementPresets.put("CLASS_TEST_TA", List.of(
                "Strictly abide by the school's examination management regulations.",
                "Meticulous and rigorous in work, with a strong sense of responsibility.",
                "Possess confidentiality awareness.",
                "Have strong on-site adaptability for in-class tests."
        ));

        String[] moIds = new String[]{"mo001", "mo002", "mo003", "mo004", "mo005", "mo006"};
        String[][] moTypes = new String[][]{
                {"COURSE_TA", "LAB_TA", "FINAL_EXAM_TA"},
                {"COURSE_TA", "LAB_TA", "CLASS_TEST_TA"},
                {"COURSE_TA", "FINAL_EXAM_TA", "CLASS_TEST_TA"},
                {"LAB_TA", "FINAL_EXAM_TA", "CLASS_TEST_TA"},
                {"COURSE_TA", "LAB_TA", "FINAL_EXAM_TA"},
                {"COURSE_TA", "LAB_TA", "CLASS_TEST_TA"}
        };

        List<Job> newJobs = new ArrayList<>();
        int courseIdx = 0;
        int jobCounter = 1;
        for (int i = 0; i < moIds.length; i++) {
            for (int t = 0; t < 3; t++) {
                String type = moTypes[i][t];
                String courseName = courseNamePresets.get(courseIdx % courseNamePresets.size());
                courseIdx++;

                Job j = new Job();
                j.id = String.format("job%03d", jobCounter++);
                j.postedBy = moIds[i];
                j.type = type;
                j.courseName = courseName;
                j.title = buildSeedJobTitle(type, courseName);
                j.description = buildSeedJobDescription(type);
                j.requirements = new ArrayList<>(requirementPresets.getOrDefault(type, List.of()));
                j.quota = 1 + ((i + t) % 3);
                j.status = "OPEN";
                j.createdAt = now - 1000L * 60 * 60 * 24 * (20 - (i * 3 + t));
                j.deadline = buildSeedDeadlineIso(now, 7 + (i * 3 + t));

                if ("FINAL_EXAM_TA".equals(type)) {
                    j.examDateTime = buildSeedExamDateTimeIso(now, 10 + i + t, 14 + (i % 3), 0);
                    j.examDuration = 2.0 + (t % 2) * 0.5;
                    j.examLocation = "Teaching Building A-" + (101 + i);
                    j.weeklyHours = j.examDuration;
                    j.schedule = buildSeedFinalExamScheduleText(j.examDateTime, j.examDuration, j.examLocation);
                } else {
                    List<Map<String, Object>> entries = buildSeedScheduleEntries(i, t);
                    j.courseScheduleGrid = gson.toJson(entries);
                    j.schedule = buildSeedScheduleSummaryText(entries);
                    j.weeklyHours = calcSeedWeeklyHours(entries);
                }

                newJobs.add(j);
            }
        }
        writeList("jobs.json", newJobs);

        List<Application> newApps = new ArrayList<>();
        String[] taIds = new String[]{"ta001", "ta002", "ta003", "ta004", "ta005", "ta006"};
        int appCounter = 1;
        for (int i = 0; i < taIds.length; i++) {
            String taId = taIds[i];
            int baseJob = (i * 3) % newJobs.size();
            Job j1 = newJobs.get((baseJob + 0) % newJobs.size());
            Job j2 = newJobs.get((baseJob + 5) % newJobs.size());
            Job j3 = newJobs.get((baseJob + 9) % newJobs.size());
            Job j4 = newJobs.get((baseJob + 12) % newJobs.size());
            Job j5 = newJobs.get((baseJob + 15) % newJobs.size());

            List<Job> picked = List.of(j1, j2, j3, j4, j5);
            String[] statuses = new String[]{"PENDING", "APPROVED", "REJECTED", "PENDING", "REJECTED"};
            int[] priorities = new int[]{1, 2, 3, 3, 1};
            for (int k = 0; k < picked.size(); k++) {
                Application a = new Application();
                a.id = String.format("app%03d", appCounter++);
                a.jobId = picked.get(k).id;
                a.applicantId = taId;
                a.priority = priorities[k];
                a.cvFileName = "cv_" + taId + ".pdf";
                a.coverLetter = buildSeedCoverLetter(picked.get(k));
                a.status = statuses[k];
                a.createdAt = now - 1000L * 60 * 60 * (12L * (k + 1) + i);
                a.updatedAt = a.createdAt + ("APPROVED".equals(a.status) || "REJECTED".equals(a.status) ? 1000L * 60 * 60 * 6 : 0);
                newApps.add(a);
            }
        }
        writeList("applications.json", newApps);

        writeList("notifications.json", new ArrayList<Notification>());

        String[] emptyLists = {"audit_logs.json", "password_resets.json", "export_tasks.json", "admin_role_templates.json", "application_drafts.json"};
        for (String f : emptyLists) {
            if (!Files.exists(dataDir.resolve(f))) writeList(f, new ArrayList<>());
        }
    }

    private String buildSeedJobTitle(String type, String courseName) {
        if ("COURSE_TA".equals(type)) return "Course TA: " + courseName;
        if ("LAB_TA".equals(type)) return "Lab TA: " + courseName;
        if ("FINAL_EXAM_TA".equals(type)) return "Final Exam TA: " + courseName;
        if ("CLASS_TEST_TA".equals(type)) return "Class Test TA: " + courseName;
        return "TA: " + courseName;
    }

    private String buildSeedJobDescription(String type) {
        if ("COURSE_TA".equals(type)) return "Weekly teaching support based on course timetable.";
        if ("LAB_TA".equals(type)) return "Assist laboratory sessions and experiments.";
        if ("FINAL_EXAM_TA".equals(type)) return "Support invigilation and exam logistics.";
        if ("CLASS_TEST_TA".equals(type)) return "Support quizzes and in-class tests.";
        return "Teaching assistant support.";
    }

    private String buildSeedDeadlineIso(long now, int plusDays) {
        java.time.LocalDate d = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate().plusDays(plusDays);
        return d.toString();
    }

    private String buildSeedExamDateTimeIso(long now, int plusDays, int hour, int minute) {
        java.time.LocalDateTime dt = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime().plusDays(plusDays).withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        return dt.toString().substring(0, 16);
    }

    private String buildSeedFinalExamScheduleText(String examDateTimeIso, double duration, String location) {
        String display = examDateTimeIso == null ? "" : examDateTimeIso.replace('T', ' ');
        return (display.isEmpty() ? "" : (display + " (" + duration + " hours)")) + (location == null || location.isEmpty() ? "" : (" @ " + location));
    }

    private List<Map<String, Object>> buildSeedScheduleEntries(int moIdx, int typeIdx) {
        List<Map<String, Object>> entries = new ArrayList<>();

        Map<String, List<Integer>> sel1 = new LinkedHashMap<>();
        sel1.put("Mon", List.of(1, 2));
        sel1.put("Tue", List.of());
        sel1.put("Wed", List.of(7, 8));
        sel1.put("Thu", List.of());
        sel1.put("Fri", List.of());

        Map<String, List<Integer>> sel2 = new LinkedHashMap<>();
        sel2.put("Mon", List.of());
        sel2.put("Tue", List.of(9, 10));
        sel2.put("Wed", List.of());
        sel2.put("Thu", List.of(3, 4));
        sel2.put("Fri", List.of());

        int w1 = 1 + ((moIdx + typeIdx) % 6);
        int w2 = 7 + ((moIdx + typeIdx) % 6);

        entries.add(buildSeedScheduleEntry(w1, moIdx, typeIdx, sel1));
        entries.add(buildSeedScheduleEntry(w2, moIdx, typeIdx, sel2));
        return entries;
    }

    private Map<String, Object> buildSeedScheduleEntry(int week, int moIdx, int typeIdx, Map<String, List<Integer>> baseSelection) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("week", week);

        Map<String, List<Integer>> sel = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> e : baseSelection.entrySet()) {
            List<Integer> ps = new ArrayList<>(e.getValue());
            int shift = (moIdx + typeIdx) % 3;
            if (!ps.isEmpty() && shift > 0) {
                ps = ps.stream().map(p -> Math.min(14, p + shift)).collect(Collectors.toList());
            }
            sel.put(e.getKey(), ps);
        }
        entry.put("selection", sel);
        return entry;
    }

    private double calcSeedWeeklyHours(List<Map<String, Object>> entries) {
        if (entries == null) return 0;
        int total = 0;
        for (Map<String, Object> e : entries) {
            Object selObj = e.get("selection");
            if (!(selObj instanceof Map)) continue;
            Map<?, ?> sel = (Map<?, ?>) selObj;
            for (String d : List.of("Mon", "Tue", "Wed", "Thu", "Fri")) {
                Object v = sel.get(d);
                if (v instanceof List) total += ((List<?>) v).size();
            }
        }
        return total * 0.75;
    }

    private String buildSeedScheduleSummaryText(List<Map<String, Object>> entries) {
        if (entries == null) return "";
        List<String> lines = new ArrayList<>();
        Map<String, String> dayLabel = Map.of(
                "Mon", "Monday",
                "Tue", "Tuesday",
                "Wed", "Wednesday",
                "Thu", "Thursday",
                "Fri", "Friday"
        );

        for (Map<String, Object> e : entries) {
            int week = 1;
            Object wObj = e.get("week");
            if (wObj instanceof Number) week = ((Number) wObj).intValue();

            Object selObj = e.get("selection");
            if (!(selObj instanceof Map)) continue;
            Map<?, ?> sel = (Map<?, ?>) selObj;

            for (String d : List.of("Mon", "Tue", "Wed", "Thu", "Fri")) {
                Object v = sel.get(d);
                if (!(v instanceof List)) continue;
                List<Integer> periods = ((List<?>) v).stream()
                        .map(x -> {
                            try { return Integer.parseInt(String.valueOf(x)); } catch (Exception ex) { return null; }
                        })
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
                if (periods.isEmpty()) continue;

                for (int[] r : mergeSeedConsecutivePeriods(periods)) {
                    String st = seedPeriodStart(r[0]);
                    String et = seedPeriodEnd(r[1]);
                    if (st.isEmpty() || et.isEmpty()) continue;
                    lines.add("Week " + week + " " + dayLabel.getOrDefault(d, d) + "：" + st + "-" + et);
                }
            }
        }
        return String.join("\n", lines);
    }

    private List<int[]> mergeSeedConsecutivePeriods(List<Integer> periods) {
        List<int[]> ranges = new ArrayList<>();
        Integer start = null;
        Integer prev = null;
        for (Integer p : periods) {
            if (p == null) continue;
            if (start == null) { start = p; prev = p; continue; }
            if (p == prev + 1) { prev = p; continue; }
            ranges.add(new int[]{start, prev});
            start = p;
            prev = p;
        }
        if (start != null && prev != null) ranges.add(new int[]{start, prev});
        return ranges;
    }

    private String seedPeriodStart(int p) {
        switch (p) {
            case 1: return "08:00";
            case 2: return "08:50";
            case 3: return "09:50";
            case 4: return "10:40";
            case 5: return "11:30";
            case 6: return "13:00";
            case 7: return "13:50";
            case 8: return "14:45";
            case 9: return "15:40";
            case 10: return "16:35";
            case 11: return "17:25";
            case 12: return "18:30";
            case 13: return "19:20";
            case 14: return "20:10";
            default: return "";
        }
    }

    private String seedPeriodEnd(int p) {
        switch (p) {
            case 1: return "08:45";
            case 2: return "09:35";
            case 3: return "10:35";
            case 4: return "11:25";
            case 5: return "12:15";
            case 6: return "13:45";
            case 7: return "14:35";
            case 8: return "15:30";
            case 9: return "16:25";
            case 10: return "17:20";
            case 11: return "18:10";
            case 12: return "19:15";
            case 13: return "20:05";
            case 14: return "20:55";
            default: return "";
        }
    }

    private String buildSeedCoverLetter(Job job) {
        String course = job == null ? "the course" : (job.courseName == null ? "the course" : job.courseName);
        return "I am interested in this position and can contribute to " + course + ".";
    }
}
