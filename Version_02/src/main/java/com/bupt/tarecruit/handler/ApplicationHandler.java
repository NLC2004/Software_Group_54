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
            } else if (parts.length == 4 && "GET".equals(method)) {
                getApplication(ex, parts[3]);
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
        String jobId = getQueryParam(ex, "jobId");

        if (jobId != null && !jobId.isEmpty()) {
            apps = ds.getApplicationsByJob(jobId);
            if ("MO".equals(user.role)) {
                Job job = ds.getJobById(jobId);
                if (job == null || !job.postedBy.equals(user.id)) {
                    sendError(ex, 403, "Not your job posting"); return;
                }
            } else if (!"ADMIN".equals(user.role)) {
                apps = apps.stream().filter(a -> a.applicantId.equals(user.id)).collect(Collectors.toList());
            }
        } else if ("TA".equals(user.role)) {
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

        sendJson(ex, 200, enrichApplications(apps));
    }

    private void getApplication(HttpExchange ex, String appId) throws IOException {
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }

        Application app = ds.getApplicationById(appId);
        if (app == null) { sendError(ex, 404, "Application not found"); return; }

        if ("TA".equals(user.role) && !app.applicantId.equals(user.id)) {
            sendError(ex, 403, "Not your application"); return;
        }
        if ("MO".equals(user.role)) {
            Job job = ds.getJobById(app.jobId);
            if (job == null || !job.postedBy.equals(user.id)) {
                sendError(ex, 403, "Not authorized"); return;
            }
        }

        Map<String, Object> m = enrichSingleApplication(app);
        sendJson(ex, 200, m);
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

        Job job = ds.getJobById(app.jobId);
        String jobTitle = job != null ? job.title : "Unknown";
        ds.addAuditLog(user.id, user.username, "APPLICATION_" + newStatus, "Application " + appId + " for " + jobTitle);

        Notification n = new Notification();
        n.userId = app.applicantId;
        n.type = "APPLICATION";
        if ("APPROVED".equals(newStatus)) {
            n.title = "Application Approved";
            n.content = "Your application for " + jobTitle + " has been approved!";
        } else if ("REJECTED".equals(newStatus)) {
            n.title = "Application Rejected";
            n.content = "Your application for " + jobTitle + " has been rejected.";
        } else if ("WITHDRAWN".equals(newStatus)) {
            n.title = "Application Withdrawn";
            n.content = "You withdrew your application for " + jobTitle + ".";
        }
        if (n.title != null) ds.addNotification(n);

        sendJson(ex, 200, Map.of("message", "Status updated", "status", newStatus));
    }

    private List<Map<String, Object>> enrichApplications(List<Application> apps) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Application a : apps) result.add(enrichSingleApplication(a));
        return result;
    }

    private Map<String, Object> enrichSingleApplication(Application a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id); m.put("jobId", a.jobId);
        m.put("applicantId", a.applicantId); m.put("cvFileName", a.cvFileName);
        m.put("coverLetter", a.coverLetter); m.put("status", a.status);
        m.put("priority", a.priority);
        m.put("createdAt", a.createdAt); m.put("updatedAt", a.updatedAt);
        Job job = ds.getJobById(a.jobId);
        m.put("jobTitle", job != null ? job.title : "Unknown");
        m.put("jobType", job != null ? job.type : "");
        m.put("jobCourseName", job != null ? job.courseName : "");
        User applicant = ds.getUserById(a.applicantId);
        if (applicant != null) {
            m.put("applicantName", applicant.fullName != null && !applicant.fullName.isEmpty() ? applicant.fullName : applicant.username);
            m.put("applicantEmail", applicant.email);
            m.put("applicantPhone", applicant.phone);
            m.put("applicantStudentId", applicant.studentId);
        } else {
            m.put("applicantName", "Unknown");
        }
        return m;
    }
}
