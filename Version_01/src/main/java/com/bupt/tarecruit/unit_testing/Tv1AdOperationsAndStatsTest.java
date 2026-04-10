package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AdminHandler;
import com.bupt.tarecruit.model.Application;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.PasswordResetRequest;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Tv1AdOperationsAndStatsTest {

    @TempDir
    Path tempDir;

    @Test
    void ad01_workloadShouldMarkOverloadedTa() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addUser(ds, "workload_mo", "MO");
        User ta = addUser(ds, "overloaded_ta", "TA");

        Map<String, String> settings = new HashMap<>();
        settings.put("maxWeeklyHours", "10");
        ds.updateSettings(settings);

        Job job1 = addJob(ds, mo, "Heavy Job 1", 6);
        Job job2 = addJob(ds, mo, "Heavy Job 2", 7);
        persistApprovedApplication(ds, ta.id, job1.id, 1);
        persistApprovedApplication(ds, ta.id, job2.id, 2);

        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/workload", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("overloaded_ta"));
        assertTrue(body.contains("\"overloaded\":true"));
    }

    @Test
    void ad04_statsShouldReturnPlatformSummary() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addUser(ds, "stats_mo", "MO");
        User ta = addUser(ds, "stats_ta", "TA");
        Job job = addJob(ds, mo, "Stats Job", 5);
        persistApprovedApplication(ds, ta.id, job.id, 1);
        PasswordResetRequest req = new PasswordResetRequest();
        req.userId = ta.id;
        req.username = ta.username;
        req.role = ta.role;
        req.fullName = ta.fullName;
        req.email = ta.email;
        ds.addPasswordResetRequest(req);

        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/stats", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("totalUsers"));
        assertTrue(body.contains("approvedApplications"));
        assertTrue(body.contains("pendingResetRequests"));
        assertTrue(body.contains("overloadedTAs"));
    }

    @Test
    void ad06_auditLogsShouldReturnNewestLogs() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User target = addUser(ds, "audit_target", "TA");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange updateEx = new TestHttpExchange(
                "PUT",
                "/api/admin/users/" + target.id,
                null,
                "{\"active\":false}"
        );
        updateEx.setBearerToken(token);
        handler.handle(updateEx);

        TestHttpExchange logsEx = new TestHttpExchange("GET", "/api/admin/audit-logs", null, null);
        logsEx.setBearerToken(token);
        handler.handle(logsEx);

        assertEquals(200, logsEx.getResponseCode());
        String body = logsEx.getResponseBodyAsString();
        assertTrue(body.contains("UPDATE_USER"));
        assertTrue(body.contains(target.id));
    }

    @Test
    void ad08_settingsShouldPersistRecruitmentConfiguration() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/admin/settings",
                null,
                "{\"applicationStartDate\":\"2026-01-01\",\"applicationEndDate\":\"2026-02-01\",\"maxWeeklyHours\":\"18\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertEquals("2026-01-01", ds.getSettings().get("applicationStartDate"));
        assertEquals("2026-02-01", ds.getSettings().get("applicationEndDate"));
        assertEquals("18", ds.getSettings().get("maxWeeklyHours"));
    }

    @Test
    void ad10_reviewPasswordResetShouldApproveAndResetPassword() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User ta = addUser(ds, "reset_ta", "TA");
        ta.password = "old-pass";
        ds.updateUser(ta);

        PasswordResetRequest req = new PasswordResetRequest();
        req.userId = ta.id;
        req.username = ta.username;
        req.role = ta.role;
        req.fullName = ta.fullName;
        req.email = ta.email;
        req = ds.addPasswordResetRequest(req);
        String requestId = req.id;

        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/admin/reset-requests/" + requestId + "/review",
                null,
                "{\"status\":\"APPROVED\",\"reviewComment\":\"verified\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertEquals("APPROVED", ds.getPasswordResetRequestById(requestId).status);
        assertEquals("123456", ds.getUserById(ta.id).password);
        assertTrue(ds.getAllAuditLogs().stream().anyMatch(log -> "APPROVE_RESET".equals(log.action) && requestId.equals(log.targetId)));
    }

    private User addUser(DataService ds, String username, String role) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = role;
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = "SID_" + username;
        return ds.addUser(user);
    }

    private Job addJob(DataService ds, User mo, String title, double weeklyHours) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = title;
        job.type = "COURSE";
        job.courseName = title + " Course";
        job.description = "desc";
        job.quota = 1;
        job.schedule = "Mon";
        job.deadline = "2099-12-31";
        job.salary = "100";
        job.weeklyHours = weeklyHours;
        return ds.addJob(job);
    }

    private void persistApprovedApplication(DataService ds, String taId, String jobId, int priority) {
        Application app = new Application();
        app.applicantId = taId;
        app.jobId = jobId;
        app.priority = priority;
        app.cvFileName = "cv.pdf";
        app.coverLetter = "cover";
        app = ds.addApplication(app);
        app.status = "APPROVED";
        ds.updateApplication(app);
    }
}
