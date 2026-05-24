package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.*;
import com.bupt.tarecruit.service.AiMatchingService;
import com.bupt.tarecruit.service.DataService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the job handler component of the TA recruitment system.
 */
public class JobHandler extends BaseHandler {

    /**
     * Creates a new job handler instance.
     */
    public JobHandler(DataService ds) { super(ds); }

    /**
     * Handles the handle operation.
     */
    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        String[] parts = path.split("/");
        try {
            if (parts.length == 3) {
                if ("GET".equals(method)) listJobs(ex);
                else if ("POST".equals(method)) createJob(ex);
                else sendError(ex, 405, "Method not allowed");
            } else if (parts.length == 4) {
                String jobId = parts[3];
                if ("GET".equals(method)) getJob(ex, jobId);
                else if ("PUT".equals(method)) updateJob(ex, jobId);
                else if ("DELETE".equals(method)) deleteJob(ex, jobId);
                else sendError(ex, 405, "Method not allowed");
            } else if (parts.length >= 5) {
                String jobId = parts[3];
                String sub = parts[4];
                if ("apply".equals(sub) && "POST".equals(method)) applyForJob(ex, jobId);
                else if ("applications".equals(sub) && "GET".equals(method)) getJobApplications(ex, jobId);
                else if ("match".equals(sub) && ("GET".equals(method) || "POST".equals(method))) getAiMatch(ex, jobId, method);
                else sendError(ex, 404, "Not found");
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Internal error");
        }
    }

    private void listJobs(HttpExchange ex) throws IOException {
        List<Job> jobs = ds.getAllJobs();
        String type = getQueryParam(ex, "type");
        String status = getQueryParam(ex, "status");
        String postedBy = getQueryParam(ex, "postedBy");
        String search = getQueryParam(ex, "search");
        if (type != null && !type.isEmpty()) jobs = jobs.stream().filter(j -> j.type.equals(type)).collect(Collectors.toList());
        if (status != null && !status.isEmpty()) jobs = jobs.stream().filter(j -> j.status.equals(status)).collect(Collectors.toList());
        if (postedBy != null && !postedBy.isEmpty()) jobs = jobs.stream().filter(j -> j.postedBy.equals(postedBy)).collect(Collectors.toList());
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            jobs = jobs.stream().filter(j ->
                (j.title != null && j.title.toLowerCase().contains(q)) ||
                (j.courseName != null && j.courseName.toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Job j : jobs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", j.id); m.put("title", j.title); m.put("type", j.type);
            m.put("courseName", j.courseName); m.put("description", j.description);
            m.put("requirements", j.requirements); m.put("quota", j.quota);
            m.put("schedule", j.schedule); m.put("weeklyHours", j.weeklyHours);
            m.put("deadline", j.deadline);
            m.put("courseScheduleGrid", j.courseScheduleGrid);
            m.put("courseWeekStart", j.courseWeekStart);
            m.put("courseWeekEnd", j.courseWeekEnd);
            m.put("labSessionCount", j.labSessionCount);
            m.put("labTime", j.labTime);
            m.put("labLocation", j.labLocation);
            m.put("labSessions", j.labSessions);
            m.put("examDateTime", j.examDateTime);
            m.put("examDuration", j.examDuration);
            m.put("examLocation", j.examLocation);
            m.put("testScheduleType", j.testScheduleType);
            m.put("testScheduleDetail", j.testScheduleDetail);
            m.put("status", j.status); m.put("createdAt", j.createdAt);
            m.put("postedBy", j.postedBy);
            User poster = ds.getUserById(j.postedBy);
            m.put("posterName", poster != null ? (poster.fullName != null && !poster.fullName.isEmpty() ? poster.fullName : poster.username) : "Unknown");

            List<Application> apps = ds.getApplicationsByJob(j.id);
            long appCount = apps.size();
            long approved = apps.stream().filter(a -> "APPROVED".equals(a.status)).count();
            long pending = apps.stream().filter(a -> "PENDING".equals(a.status)).count();
            long rejected = apps.stream().filter(a -> "REJECTED".equals(a.status)).count();
            long withdrawn = apps.stream().filter(a -> "WITHDRAWN".equals(a.status)).count();

            m.put("applicationCount", appCount);
            m.put("approvedCount", approved);
            m.put("pendingCount", pending);
            m.put("rejectedCount", rejected);
            m.put("withdrawnCount", withdrawn);
            result.add(m);
        }
        sendJson(ex, 200, result);
    }

    private void getJob(HttpExchange ex, String jobId) throws IOException {
        Job job = ds.getJobById(jobId);
        if (job == null) { sendError(ex, 404, "Job not found"); return; }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", job.id); m.put("title", job.title); m.put("type", job.type);
        m.put("courseName", job.courseName); m.put("description", job.description);
        m.put("requirements", job.requirements); m.put("quota", job.quota);
        m.put("schedule", job.schedule); m.put("weeklyHours", job.weeklyHours);
        m.put("deadline", job.deadline);
        m.put("courseScheduleGrid", job.courseScheduleGrid);
        m.put("courseWeekStart", job.courseWeekStart);
        m.put("courseWeekEnd", job.courseWeekEnd);
        m.put("labSessionCount", job.labSessionCount);
        m.put("labTime", job.labTime);
        m.put("labLocation", job.labLocation);
        m.put("labSessions", job.labSessions);
        m.put("examDateTime", job.examDateTime);
        m.put("examDuration", job.examDuration);
        m.put("examLocation", job.examLocation);
        m.put("testScheduleType", job.testScheduleType);
        m.put("testScheduleDetail", job.testScheduleDetail);
        m.put("status", job.status); m.put("createdAt", job.createdAt);
        m.put("postedBy", job.postedBy);
        User poster = ds.getUserById(job.postedBy);
        m.put("posterName", poster != null ? (poster.fullName != null && !poster.fullName.isEmpty() ? poster.fullName : poster.username) : "Unknown");

        List<Application> apps = ds.getApplicationsByJob(job.id);
        long approved = apps.stream().filter(a -> "APPROVED".equals(a.status)).count();
        long pending = apps.stream().filter(a -> "PENDING".equals(a.status)).count();
        long rejected = apps.stream().filter(a -> "REJECTED".equals(a.status)).count();
        long withdrawn = apps.stream().filter(a -> "WITHDRAWN".equals(a.status)).count();

        m.put("applicationCount", apps.size());
        m.put("approvedCount", approved);
        m.put("pendingCount", pending);
        m.put("rejectedCount", rejected);
        m.put("withdrawnCount", withdrawn);
        sendJson(ex, 200, m);
    }

    private void createJob(HttpExchange ex) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        if (!"MO".equals(user.role) && !"ADMIN".equals(user.role)) {
            sendError(ex, 403, "Only Module Organisers can post jobs"); return;
        }
        JsonObject body = parseJson(readBody(ex));
        Job job = new Job();
        job.postedBy = user.id;
        job.title = body.has("title") ? body.get("title").getAsString() : "";
        job.type = body.has("type") ? body.get("type").getAsString() : "COURSE_TA";
        job.courseName = body.has("courseName") ? body.get("courseName").getAsString() : "";
        job.description = body.has("description") ? body.get("description").getAsString() : "";
        job.quota = body.has("quota") ? body.get("quota").getAsInt() : 1;
        job.schedule = body.has("schedule") ? body.get("schedule").getAsString() : "";
        job.weeklyHours = body.has("weeklyHours") ? body.get("weeklyHours").getAsDouble() : 0;
        job.deadline = body.has("deadline") ? body.get("deadline").getAsString() : "";

        if (body.has("courseScheduleGrid")) job.courseScheduleGrid = body.get("courseScheduleGrid").getAsString();
        if (body.has("courseWeekStart")) job.courseWeekStart = body.get("courseWeekStart").getAsInt();
        if (body.has("courseWeekEnd")) job.courseWeekEnd = body.get("courseWeekEnd").getAsInt();

        if (body.has("labSessionCount")) job.labSessionCount = body.get("labSessionCount").getAsInt();
        if (body.has("labTime")) job.labTime = body.get("labTime").getAsString();
        if (body.has("labLocation")) job.labLocation = body.get("labLocation").getAsString();
        if (body.has("labSessions")) job.labSessions = body.get("labSessions").getAsString();

        if (body.has("examDateTime")) job.examDateTime = body.get("examDateTime").getAsString();
        if (body.has("examDuration")) job.examDuration = body.get("examDuration").getAsDouble();
        if (body.has("examLocation")) job.examLocation = body.get("examLocation").getAsString();

        if (body.has("testScheduleType")) job.testScheduleType = body.get("testScheduleType").getAsString();
        if (body.has("testScheduleDetail")) job.testScheduleDetail = body.get("testScheduleDetail").getAsString();

        if (body.has("requirements")) {
            JsonArray arr = body.getAsJsonArray("requirements");
            job.requirements = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) job.requirements.add(arr.get(i).getAsString());
        }
        if (requiresUniqueScheduleWeeks(job.type) && hasDuplicateScheduleWeeks(job.courseScheduleGrid)) {
            sendError(ex, 400, "Course Week must be unique across schedule entries"); return;
        }
        Job saved = ds.addJob(job);
        ds.addAuditLog(user.id, user.username, "CREATE_JOB", "Created job: " + saved.title);
        sendJson(ex, 201, saved);
    }

    private void updateJob(HttpExchange ex, String jobId) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        Job job = ds.getJobById(jobId);
        if (job == null) { sendError(ex, 404, "Job not found"); return; }
        if (!job.postedBy.equals(user.id) && !"ADMIN".equals(user.role)) {
            sendError(ex, 403, "Not authorized"); return;
        }
        JsonObject body = parseJson(readBody(ex));
        if (body.has("title")) job.title = body.get("title").getAsString();
        if (body.has("description")) job.description = body.get("description").getAsString();
        if (body.has("status")) job.status = body.get("status").getAsString();
        if (body.has("quota")) job.quota = body.get("quota").getAsInt();
        if (body.has("weeklyHours")) job.weeklyHours = body.get("weeklyHours").getAsDouble();
        if (body.has("schedule")) job.schedule = body.get("schedule").getAsString();
        if (body.has("courseName")) job.courseName = body.get("courseName").getAsString();
        if (body.has("deadline")) job.deadline = body.get("deadline").getAsString();
        if (body.has("type")) job.type = body.get("type").getAsString();

        if (body.has("courseScheduleGrid")) job.courseScheduleGrid = body.get("courseScheduleGrid").getAsString();
        if (body.has("courseWeekStart")) job.courseWeekStart = body.get("courseWeekStart").getAsInt();
        if (body.has("courseWeekEnd")) job.courseWeekEnd = body.get("courseWeekEnd").getAsInt();

        if (body.has("labSessionCount")) job.labSessionCount = body.get("labSessionCount").getAsInt();
        if (body.has("labTime")) job.labTime = body.get("labTime").getAsString();
        if (body.has("labLocation")) job.labLocation = body.get("labLocation").getAsString();
        if (body.has("labSessions")) job.labSessions = body.get("labSessions").getAsString();

        if (body.has("examDateTime")) job.examDateTime = body.get("examDateTime").getAsString();
        if (body.has("examDuration")) job.examDuration = body.get("examDuration").getAsDouble();
        if (body.has("examLocation")) job.examLocation = body.get("examLocation").getAsString();

        if (body.has("testScheduleType")) job.testScheduleType = body.get("testScheduleType").getAsString();
        if (body.has("testScheduleDetail")) job.testScheduleDetail = body.get("testScheduleDetail").getAsString();

        if (body.has("requirements")) {
            JsonArray arr = body.getAsJsonArray("requirements");
            job.requirements = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) job.requirements.add(arr.get(i).getAsString());
        }
        if (requiresUniqueScheduleWeeks(job.type) && hasDuplicateScheduleWeeks(job.courseScheduleGrid)) {
            sendError(ex, 400, "Course Week must be unique across schedule entries"); return;
        }
        ds.updateJob(job);
        ds.addAuditLog(user.id, user.username, "UPDATE_JOB", "Updated job: " + job.title);
        sendJson(ex, 200, job);
    }

    private void deleteJob(HttpExchange ex, String jobId) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        Job job = ds.getJobById(jobId);
        if (job == null) { sendError(ex, 404, "Job not found"); return; }
        if (!job.postedBy.equals(user.id) && !"ADMIN".equals(user.role)) {
            sendError(ex, 403, "Not authorized"); return;
        }
        int deletedApps = ds.deleteApplicationsByJob(jobId);
        int deletedDrafts = ds.deleteApplicationDraftsByJob(jobId);
        int deletedNotifications = ds.deleteNotificationsRelatedToJob(job);
        ds.deleteJob(jobId);
        ds.addAuditLog(user.id, user.username, "DELETE_JOB", "Deleted job: " + job.title
                + " (applications=" + deletedApps
                + ", drafts=" + deletedDrafts
                + ", notifications=" + deletedNotifications + ")");
        sendJson(ex, 200, Map.of(
                "message", "Job deleted",
                "deletedApplications", deletedApps,
                "deletedDrafts", deletedDrafts,
                "deletedNotifications", deletedNotifications
        ));
    }

    private void applyForJob(HttpExchange ex, String jobId) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        if (!"TA".equals(user.role)) { sendError(ex, 403, "Only TAs can apply"); return; }
        Job job = ds.getJobById(jobId);
        if (job == null) { sendError(ex, 404, "Job not found"); return; }
        if (!"OPEN".equals(job.status)) {
            sendError(ex, 400, "This position is no longer accepting applications"); return;
        }
        if (isDeadlinePassed(job.deadline)) {
            sendError(ex, 400, "This position is past the application deadline"); return;
        }
        if (isJobFull(job)) {
            sendError(ex, 409, "This position has reached its quota"); return;
        }
        boolean alreadyApplied = ds.getApplicationsByApplicant(user.id).stream()
                .anyMatch(a -> a.jobId.equals(jobId)
                        && !"WITHDRAWN".equals(a.status)
                        && !"REJECTED".equals(a.status));
        if (alreadyApplied) {
            sendError(ex, 409, "You have already applied for this position"); return;
        }
        JsonObject body = parseJson(readBody(ex));
        int priority = body.has("priority") ? body.get("priority").getAsInt() : 0;
        if (priority < 1 || priority > 3) {
            sendError(ex, 400, "Priority must be 1, 2, or 3"); return;
        }
        List<Application> activeApplications = ds.getApplicationsByApplicant(user.id).stream()
                .filter(a -> !"WITHDRAWN".equals(a.status) && !"REJECTED".equals(a.status))
                .collect(Collectors.toList());
        if (activeApplications.size() >= 3) {
            sendError(ex, 409, "You already have 3 active applications"); return;
        }
        boolean duplicatePriority = activeApplications.stream()
                .anyMatch(a -> a.priority == priority);
        if (duplicatePriority) {
            sendError(ex, 409, "This priority is already used by another active application"); return;
        }
        if (ds.wouldExceedWeeklyWorkload(user.id, job)) {
            Map<Integer, Double> currentWeekly = ds.getTAWeeklyHours(user.id);
            Map<Integer, Double> addedWeekly = ds.getJobWeeklyHours(job);
            double limit = ds.getWeeklyWorkloadLimit();

            List<String> details = new ArrayList<>();
            List<Integer> blockedWeeks = new ArrayList<>();
            for (Map.Entry<Integer, Double> e : addedWeekly.entrySet()) {
                int week = e.getKey();
                double current = currentWeekly.getOrDefault(week, 0.0);
                double added = e.getValue() == null ? 0 : e.getValue();
                double future = current + added;
                if (future > limit) {
                    blockedWeeks.add(week);
                    details.add("Week " + week + ": " + String.format(Locale.ROOT, "%.2f", current)
                            + " + " + String.format(Locale.ROOT, "%.2f", added)
                            + " = " + String.format(Locale.ROOT, "%.2f", future)
                            + " / limit " + String.format(Locale.ROOT, "%.2f", limit)
                            + " (exceeds by " + String.format(Locale.ROOT, "%.2f", future - limit) + "h)");
                }
            }
            if (blockedWeeks.isEmpty()) {
                blockedWeeks = ds.getWeeksThatWouldExceedWeeklyWorkload(user.id, job);
            }

            String weekList = blockedWeeks.stream().map(w -> "Week " + w).collect(Collectors.joining(", "));
            String detailText = details.isEmpty() ? (weekList.isEmpty() ? "This application would exceed your weekly workload limit." : weekList)
                    : String.join("; ", details);

            Notification blocked = new Notification();
            blocked.userId = user.id;
            blocked.title = "Application blocked by weekly workload limit";
            blocked.content = detailText;
            blocked.type = "WORKLOAD";
            ds.addNotification(blocked);
            ds.addAuditLog(user.id, user.username, "WORKLOAD_BLOCK",
                    "Blocked application for job: " + job.title + " due to weekly workload limit; " + detailText);
            sendError(ex, 409, detailText);
            return;
        }
        Application app = new Application();
        app.jobId = jobId;
        app.applicantId = user.id;
        app.coverLetter = body.has("coverLetter") ? body.get("coverLetter").getAsString() : "";
        app.cvFileName = body.has("cvFileName") ? body.get("cvFileName").getAsString() : "";
        app.priority = priority;
        app = ds.addApplication(app);

        ds.addAuditLog(user.id, user.username, "APPLY", "Applied for job: " + job.title);

        Notification n = new Notification();
        n.userId = job.postedBy;
        n.title = "New Application";
        n.content = (user.fullName != null && !user.fullName.isEmpty() ? user.fullName : user.username) + " applied for " + job.title;
        n.type = "APPLICATION";
        ds.addNotification(n);

        sendJson(ex, 201, app);
    }

    private void getJobApplications(HttpExchange ex, String jobId) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        Job job = ds.getJobById(jobId);
        if (job == null) { sendError(ex, 404, "Job not found"); return; }
        if (!job.postedBy.equals(user.id) && !"ADMIN".equals(user.role)) {
            sendError(ex, 403, "Not authorized"); return;
        }
        List<Application> apps = ds.getApplicationsByJob(jobId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Application a : apps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.id); m.put("jobId", a.jobId);
            m.put("applicantId", a.applicantId); m.put("cvFileName", a.cvFileName);
            m.put("coverLetter", a.coverLetter); m.put("status", a.status);
            m.put("priority", a.priority);
            m.put("createdAt", a.createdAt); m.put("updatedAt", a.updatedAt);
            User applicant = ds.getUserById(a.applicantId);
            if (applicant != null) {
                m.put("applicantName", applicant.fullName != null && !applicant.fullName.isEmpty() ? applicant.fullName : applicant.username);
                m.put("applicantEmail", applicant.email);
                m.put("applicantPhone", applicant.phone);
                m.put("applicantStudentId", applicant.studentId);
                m.put("applicantSchool", applicant.school);
                m.put("applicantDegree", applicant.degree);
                Object storedMatch = parseStoredAiMatch(a);
                if (storedMatch != null) {
                    m.put("aiMatch", storedMatch);
                    m.put("aiMatchModel", a.aiMatchModel);
                    m.put("aiMatchUpdatedAt", a.aiMatchUpdatedAt);
                }
            } else {
                m.put("applicantName", "Unknown");
            }
            result.add(m);
        }
        sendJson(ex, 200, result);
    }

    private void getAiMatch(HttpExchange ex, String jobId, String method) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        Job job = ds.getJobById(jobId);
        if (job == null) { sendError(ex, 404, "Job not found"); return; }

        String coverLetter = "";
        String model = Optional.ofNullable(getQueryParam(ex, "model")).orElse("");
        User applicant = user;
        Application app = null;

        if ("POST".equals(method)) {
            JsonObject body = parseJson(readBody(ex));
            if (body.has("model") && !body.get("model").isJsonNull()) {
                model = body.get("model").getAsString();
            }
            if (body.has("coverLetter") && !body.get("coverLetter").isJsonNull()) {
                coverLetter = body.get("coverLetter").getAsString();
            }
            if (body.has("applicationId") && !body.get("applicationId").isJsonNull()) {
                app = ds.getApplicationById(body.get("applicationId").getAsString());
                if (app != null) {
                    if (!Objects.equals(app.jobId, jobId)) {
                        sendError(ex, 400, "Application does not belong to this job"); return;
                    }
                    boolean owner = Objects.equals(app.applicantId, user.id);
                    boolean jobOwner = Objects.equals(job.postedBy, user.id);
                    if (!owner && !jobOwner && !"ADMIN".equals(user.role)) {
                        sendError(ex, 403, "Not authorized"); return;
                    }
                    applicant = ds.getUserById(app.applicantId);
                    coverLetter = app.coverLetter;
                }
            }
        }

        if (!"TA".equals(user.role) && !Objects.equals(job.postedBy, user.id) && !"ADMIN".equals(user.role)) {
            sendError(ex, 403, "Not authorized"); return;
        }
        if (applicant == null) { sendError(ex, 404, "Applicant not found"); return; }

        AiMatchingService ai = new AiMatchingService(ds);
        if (!ai.isAllowedModel(model)) {
            sendError(ex, 400, "Invalid AI model. Allowed models: gemini-2.5-pro, qwen-plus, gpt-5-mini"); return;
        }
        String resolvedModel = (model == null || model.isBlank()) ? ai.getDefaultModel() : model.trim();
        boolean taConsumesQuota = "TA".equals(user.role);
        if (taConsumesQuota && user.aiMatchUsedCount >= 3) {
            sendError(ex, 429, "AI match limit reached: each TA can use AI match at most 3 times"); return;
        }

        Map<String, Object> result;
        try {
            result = ai.match(job, applicant, coverLetter, app, resolvedModel);
        } catch (IllegalStateException exn) {
            sendError(ex, 502, exn.getMessage()); return;
        }
        if (taConsumesQuota) {
            user.aiMatchUsedCount++;
            ds.updateUser(user);
            result.put("aiMatchUsedCount", user.aiMatchUsedCount);
            result.put("aiMatchRemaining", Math.max(0, 3 - user.aiMatchUsedCount));
        }
        if (app != null) {
            app.aiMatchJson = gson.toJson(result);
            app.aiMatchModel = resolvedModel;
            app.aiMatchUpdatedAt = System.currentTimeMillis();
            ds.updateApplication(app);
        }
        sendJson(ex, 200, result);
    }

    private Object parseStoredAiMatch(Application app) {
        if (app == null || app.aiMatchJson == null || app.aiMatchJson.isBlank()) return null;
        try {
            return gson.fromJson(app.aiMatchJson, Object.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isJobFull(Job job) {
        if (job == null) return true;
        return job.quota <= 0 || ds.getApprovedApplicationCountForJob(job.id) >= job.quota;
    }

    private boolean requiresUniqueScheduleWeeks(String type) {
        String tv = type == null ? "" : type.trim();
        return "COURSE_TA".equals(tv) || "CLASS_TEST_TA".equals(tv) || "LAB_TA".equals(tv);
    }

    private boolean hasDuplicateScheduleWeeks(String scheduleJson) {
        if (scheduleJson == null || scheduleJson.trim().isEmpty()) return false;
        try {
            JsonArray arr = gson.fromJson(scheduleJson, JsonArray.class);
            if (arr == null) return false;
            Set<Integer> weeks = new HashSet<>();
            for (int i = 0; i < arr.size(); i++) {
                if (arr.get(i) == null || !arr.get(i).isJsonObject()) continue;
                JsonObject entry = arr.get(i).getAsJsonObject();
                if (!entry.has("week") || entry.get("week").isJsonNull()) continue;
                int week = entry.get("week").getAsInt();
                if (week < 1 || week > 17) continue;
                if (!weeks.add(week)) return true;
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private boolean isDeadlinePassed(String deadline) {
        if (deadline == null || deadline.trim().isEmpty()) return false;
        try {
            return LocalDate.parse(deadline.trim()).isBefore(LocalDate.now());
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }
}
