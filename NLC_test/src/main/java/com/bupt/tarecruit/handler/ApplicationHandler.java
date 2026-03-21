package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.*;
import com.bupt.tarecruit.service.DataService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationHandler extends BaseHandler {

    public ApplicationHandler(DataService ds) { super(ds); }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        String[] parts = path.split("/");
        try {
            if (parts.length == 3 && "GET".equals(method)) {
                listApplications(ex);
            } else if (parts.length == 5 && "status".equals(parts[4]) && "PUT".equals(method)) {
                updateStatus(ex, parts[3]);
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Internal error");
        }
    }

    private void listApplications(HttpExchange ex) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }

        List<Application> apps;
        if ("TA".equals(user.role)) {
            apps = ds.getApplicationsByApplicant(user.id);
        } else if ("MO".equals(user.role)) {
            Set<String> myJobIds = ds.getAllJobs().stream()
                    .filter(j -> j.postedBy.equals(user.id))
                    .map(j -> j.id).collect(Collectors.toSet());
            apps = ds.getAllApplications().stream()
                    .filter(a -> myJobIds.contains(a.jobId))
                    .collect(Collectors.toList());
        } else {
            apps = ds.getAllApplications();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Application a : apps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.id); m.put("jobId", a.jobId);
            m.put("applicantId", a.applicantId); m.put("cvFileName", a.cvFileName);
            m.put("coverLetter", a.coverLetter); m.put("status", a.status);
            m.put("priority", a.priority);
            m.put("createdAt", a.createdAt); m.put("updatedAt", a.updatedAt);
            Job job = ds.getJobById(a.jobId);
            m.put("jobTitle", job != null ? job.title : "Unknown");
            m.put("jobType", job != null ? job.type : "");
            User applicant = ds.getUserById(a.applicantId);
            m.put("applicantName", applicant != null ? (applicant.fullName != null && !applicant.fullName.isEmpty() ? applicant.fullName : applicant.username) : "Unknown");
            m.put("applicantEmail", applicant != null ? applicant.email : "");
            result.add(m);
        }
        sendJson(ex, 200, result);
    }

    private void updateStatus(HttpExchange ex, String appId) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }

        Application app = ds.getApplicationById(appId);
        if (app == null) { sendError(ex, 404, "Application not found"); return; }

        JsonObject body = parseJson(readBody(ex));
        String newStatus = body.get("status").getAsString();

        if ("TA".equals(user.role)) {
            if (!app.applicantId.equals(user.id)) { sendError(ex, 403, "Not your application"); return; }
            if (!"WITHDRAWN".equals(newStatus)) { sendError(ex, 400, "TAs can only withdraw applications"); return; }
        } else if ("MO".equals(user.role)) {
            Job job = ds.getJobById(app.jobId);
            if (job == null || !job.postedBy.equals(user.id)) { sendError(ex, 403, "Not your job posting"); return; }
            if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
                sendError(ex, 400, "Invalid status"); return;
            }
        } else if (!"ADMIN".equals(user.role)) {
            sendError(ex, 403, "Not authorized"); return;
        }

        app.status = newStatus;
        ds.updateApplication(app);
        sendJson(ex, 200, Map.of("message", "Status updated", "status", newStatus));
    }
}
