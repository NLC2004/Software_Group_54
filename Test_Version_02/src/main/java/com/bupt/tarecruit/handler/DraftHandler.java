package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.ApplicationDraft;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DraftHandler extends BaseHandler {

    public DraftHandler(DataService ds) { super(ds); }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }
        if (!"TA".equals(user.role)) { sendError(ex, 403, "TA only"); return; }

        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        try {
            if (path.endsWith("/applications")) {
                if ("GET".equals(method)) listApplicationDrafts(ex, user);
                else sendError(ex, 405, "Method not allowed");
            } else if (path.endsWith("/application")) {
                if ("GET".equals(method)) getApplicationDraft(ex, user);
                else if ("PUT".equals(method)) upsertApplicationDraft(ex, user);
                else if ("DELETE".equals(method)) deleteApplicationDraft(ex, user);
                else sendError(ex, 405, "Method not allowed");
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Internal error");
        }
    }

    private void listApplicationDrafts(HttpExchange ex, User user) throws IOException {
        List<Map<String, Object>> drafts = ds.getAllApplicationDrafts().stream()
                .filter(d -> user.id.equals(d.userId))
                .sorted(Comparator.comparingLong((ApplicationDraft d) -> d.updatedAt).reversed())
                .map(this::toMapWithJob)
                .collect(Collectors.toList());
        sendJson(ex, 200, drafts);
    }

    private void getApplicationDraft(HttpExchange ex, User user) throws IOException {
        String jobId = normalizeJobId(getQueryParam(ex, "jobId"));
        ApplicationDraft draft = ds.getApplicationDraft(user.id, jobId);
        if (draft == null) {
            com.google.gson.JsonObject result = new com.google.gson.JsonObject();
            result.add("draft", com.google.gson.JsonNull.INSTANCE);
            sendJson(ex, 200, result);
            return;
        }
        sendJson(ex, 200, Map.of("draft", toMap(draft)));
    }

    private void upsertApplicationDraft(HttpExchange ex, User user) throws IOException {
        JsonObject body = parseJson(readBody(ex));
        String jobId = normalizeJobId(body != null && body.has("jobId") ? body.get("jobId").getAsString() : getQueryParam(ex, "jobId"));
        ApplicationDraft draft = ds.getApplicationDraft(user.id, jobId);
        if (draft == null) {
            draft = new ApplicationDraft();
            draft.userId = user.id;
            draft.jobId = jobId;
        }
        draft.confirmStudentId = getString(body, "confirmStudentId");
        draft.confirmFullName = getString(body, "confirmFullName");
        draft.confirmPhone = getString(body, "confirmPhone");
        draft.confirmGender = getString(body, "confirmGender");
        draft.confirmSchool = getString(body, "confirmSchool");
        draft.confirmYear = getString(body, "confirmYear");
        draft.coverLetter = getString(body, "coverLetter");
        draft.priority = body != null && body.has("priority") && !body.get("priority").isJsonNull() ? body.get("priority").getAsInt() : 0;
        draft.resumeDraftFileName = getString(body, "resumeDraftFileName");
        draft = ds.saveApplicationDraft(draft);
        sendJson(ex, 200, Map.of("draft", toMap(draft), "message", "Draft saved"));
    }

    private void deleteApplicationDraft(HttpExchange ex, User user) throws IOException {
        String jobId = normalizeJobId(getQueryParam(ex, "jobId"));
        ds.deleteApplicationDraft(user.id, jobId);
        sendJson(ex, 200, Map.of("message", "Draft deleted"));
    }

    private String getString(JsonObject body, String key) {
        if (body == null || !body.has(key) || body.get(key).isJsonNull()) return "";
        return body.get(key).getAsString();
    }

    private String normalizeJobId(String jobId) {
        return jobId == null ? "" : jobId.trim();
    }

    private Map<String, Object> toMap(ApplicationDraft draft) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", draft.id);
        m.put("userId", draft.userId);
        m.put("jobId", draft.jobId);
        m.put("confirmStudentId", draft.confirmStudentId);
        m.put("confirmFullName", draft.confirmFullName);
        m.put("confirmPhone", draft.confirmPhone);
        m.put("confirmGender", draft.confirmGender);
        m.put("confirmSchool", draft.confirmSchool);
        m.put("confirmYear", draft.confirmYear);
        m.put("coverLetter", draft.coverLetter);
        m.put("priority", draft.priority);
        m.put("resumeDraftFileName", draft.resumeDraftFileName);
        m.put("createdAt", draft.createdAt);
        m.put("updatedAt", draft.updatedAt);
        return m;
    }

    private Map<String, Object> toMapWithJob(ApplicationDraft draft) {
        Map<String, Object> m = toMap(draft);
        Job job = ds.getJobById(draft.jobId);
        if (job != null) {
            m.put("jobTitle", job.title);
            m.put("jobType", job.type);
            m.put("courseName", job.courseName);
            m.put("deadline", job.deadline);
            m.put("jobStatus", job.status);
        }
        return m;
    }
}
