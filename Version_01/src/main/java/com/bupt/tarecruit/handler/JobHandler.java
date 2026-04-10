package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.*;
import com.bupt.tarecruit.service.DataService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JobHandler extends BaseHandler {

    public JobHandler(DataService ds) { super(ds); }

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
            } else if (parts.length >= 5 && "apply".equals(parts[4])) {
                if ("POST".equals(method)) applyForJob(ex, parts[3]);
                else sendError(ex, 405, "Method not allowed");
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
        if (type != null) jobs = jobs.stream().filter(j -> j.type.equals(type)).collect(Collectors.toList());
        if (status != null) jobs = jobs.stream().filter(j -> j.status.equals(status)).collect(Collectors.toList());
        if (postedBy != null) jobs = jobs.stream().filter(j -> j.postedBy.equals(postedBy)).collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Job j : jobs) {
            Map<String, Object> m = toJobView(j);
            result.add(m);
        }
        sendJson(ex, 200, result);
    }

    private void getJob(HttpExchange ex, String jobId) throws IOException {
        Job job = ds.getJobById(jobId);
        if (job == null) { sendError(ex, 404, "Job not found"); return; }
        sendJson(ex, 200, toJobView(job));
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
        job.title = body.get("title").getAsString();
        job.type = body.has("type") ? body.get("type").getAsString() : "COURSE";
        job.courseName = body.has("courseName") ? body.get("courseName").getAsString() : "";
        job.description = body.has("description") ? body.get("description").getAsString() : "";
        job.quota = body.has("quota") ? body.get("quota").getAsInt() : 1;
        job.schedule = body.has("schedule") ? body.get("schedule").getAsString() : "";
        job.deadline = body.has("deadline") ? body.get("deadline").getAsString() : "";
        job.salary = body.has("salary") ? body.get("salary").getAsString() : "";
        job.weeklyHours = body.has("weeklyHours") ? body.get("weeklyHours").getAsDouble() : 0;
        if (body.has("requirements")) {
            JsonArray arr = body.getAsJsonArray("requirements");
            job.requirements = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) job.requirements.add(arr.get(i).getAsString());
        }
        Job saved = ds.addJob(job);
        sendJson(ex, 201, toJobView(saved));
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
        if (body.has("type")) job.type = body.get("type").getAsString();
        if (body.has("description")) job.description = body.get("description").getAsString();
        if (body.has("status")) job.status = body.get("status").getAsString();
        if (body.has("quota")) job.quota = body.get("quota").getAsInt();
        if (body.has("weeklyHours")) job.weeklyHours = body.get("weeklyHours").getAsDouble();
        if (body.has("schedule")) job.schedule = body.get("schedule").getAsString();
        if (body.has("courseName")) job.courseName = body.get("courseName").getAsString();
        if (body.has("deadline")) job.deadline = body.get("deadline").getAsString();
        if (body.has("salary")) job.salary = body.get("salary").getAsString();
        if (body.has("requirements")) {
            JsonArray arr = body.getAsJsonArray("requirements");
            job.requirements = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) job.requirements.add(arr.get(i).getAsString());
        }
        ds.updateJob(job);
        sendJson(ex, 200, toJobView(job));
    }

    private void deleteJob(HttpExchange ex, String jobId) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        Job job = ds.getJobById(jobId);
        if (job == null) { sendError(ex, 404, "Job not found"); return; }
        if (!job.postedBy.equals(user.id) && !"ADMIN".equals(user.role)) {
            sendError(ex, 403, "Not authorized"); return;
        }
        ds.deleteJob(jobId);
        sendJson(ex, 200, Map.of("message", "Job deleted"));
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
        boolean alreadyApplied = ds.getApplicationsByApplicant(user.id).stream()
                .anyMatch(a -> a.jobId.equals(jobId) && !"WITHDRAWN".equals(a.status));
        if (alreadyApplied) {
            sendError(ex, 409, "You have already applied for this position"); return;
        }
        JsonObject body = parseJson(readBody(ex));
        Application app = new Application();
        app.jobId = jobId;
        app.applicantId = user.id;
        app.coverLetter = body.has("coverLetter") ? body.get("coverLetter").getAsString() : "";
        app.cvFileName = body.has("cvFileName") ? body.get("cvFileName").getAsString() : "";
        app.priority = body.has("priority") ? body.get("priority").getAsInt() : 0;
        app = ds.addApplication(app);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", app.id);
        result.put("jobId", app.jobId);
        result.put("status", app.status);
        result.put("prefillProfile", Map.of(
                "studentId", user.studentId == null ? "" : user.studentId,
                "fullName", user.fullName == null ? "" : user.fullName,
                "email", user.email == null ? "" : user.email,
                "phone", user.phone == null ? "" : user.phone,
                "gender", user.gender == null ? "" : user.gender
        ));
        sendJson(ex, 201, result);
    }

    private Map<String, Object> toJobView(Job j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", j.id); m.put("title", j.title); m.put("type", j.type);
        m.put("courseName", j.courseName); m.put("description", j.description);
        m.put("requirements", j.requirements); m.put("quota", j.quota);
        m.put("schedule", j.schedule); m.put("deadline", j.deadline);
        m.put("salary", j.salary); m.put("weeklyHours", j.weeklyHours);
        m.put("status", j.status); m.put("createdAt", j.createdAt);
        m.put("postedBy", j.postedBy);
        User poster = ds.getUserById(j.postedBy);
        m.put("posterName", poster != null ? poster.fullName : "Unknown");
        m.put("applicationCount", ds.getApplicationsByJob(j.id).size());
        long approved = ds.getApplicationsByJob(j.id).stream().filter(a -> "APPROVED".equals(a.status)).count();
        m.put("approvedCount", approved);
        m.put("tasNeeded", j.quota);
        m.put("currentApplicants", ds.getApplicationsByJob(j.id).stream().filter(a -> !"WITHDRAWN".equals(a.status)).count());
        return m;
    }
}
