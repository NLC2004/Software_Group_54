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

class AdWorkloadAndStatsStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void ad01_workloadShouldShowOverloadedTaAndSupportStatusFilter() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addMo(ds, "workload_mo");
        User ta = addTa(ds, "overloaded_ta", "Engineering");
        Map<String, String> settings = new HashMap<>();
        settings.put("maxWeeklyHours", "10");
        ds.updateSettings(settings);

        Job job1 = addJob(ds, mo, "Heavy Job 1", 6);
        Job job2 = addJob(ds, mo, "Heavy Job 2", 7);
        persistApprovedApplication(ds, ta.id, job1.id, 1);
        persistApprovedApplication(ds, ta.id, job2.id, 2);

        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/workload", "status=overload&faculty=engineering", null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("overloaded_ta"));
        assertTrue(body.contains("\"overloaded\":true"));
    }

    @Test
    void ad04_statsShouldReturnApplicationAndUserSummary() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addMo(ds, "stats_mo");
        User ta = addTa(ds, "stats_ta", "Science");
        Job job = addJob(ds, mo, "Stats Job", 5);
        persistApprovedApplication(ds, ta.id, job.id, 1);
        PasswordResetRequest req = new PasswordResetRequest();
        req.studentId = ta.studentId;
        req.fullName = ta.fullName;
        req.email = ta.email;
        ds.addPasswordReset(req);

        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/stats", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("totalUsers"));
        assertTrue(body.contains("totalJobs"));
        assertTrue(body.contains("approvedApplications"));
        assertTrue(body.contains("pendingPasswordResets"));
    }

    @Test
    void ad12_statsShouldIncludePriorityAndQuotaSummary() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addMo(ds, "summary_mo");
        User ta1 = addTa(ds, "summary_ta1", "Science");
        User ta2 = addTa(ds, "summary_ta2", "Science");
        Job job1 = addJob(ds, mo, "Summary Job 1", 4);
        Job job2 = addJob(ds, mo, "Summary Job 2", 3);
        persistApprovedApplication(ds, ta1.id, job1.id, 1);
        persistApprovedApplication(ds, ta2.id, job2.id, 2);

        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/stats", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("activePreferenceUsage"));
        assertTrue(body.contains("taPriorityDistribution"));
        assertTrue(body.contains("applicationsBySchool"));
        assertTrue(body.contains("totalQuota"));
        assertTrue(body.contains("dailyTrend"));
        assertTrue(body.contains("dailyApplicationTrend"));
        assertTrue(body.contains("openJobs"));
    }

    @Test
    void ad04_statsOpenJobsShouldReflectAllLiveJobsNotOnlyCreatedInPeriod() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addMo(ds, "live_stats_mo");
        Job oldJob = addJob(ds, mo, "Old Job", 4);
        oldJob.createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 120;
        ds.updateJob(oldJob);
        Job newJob = addJob(ds, mo, "New Job", 3);

        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);
        long thirtyDaysAgo = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30;
        String start = java.time.Instant.ofEpochMilli(thirtyDaysAgo).atZone(java.time.ZoneId.systemDefault())
                .toLocalDate().toString();
        String end = java.time.LocalDate.now().toString();
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/stats?startDate=" + start + "&endDate=" + end,
                null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("\"openJobs\":2"), body);
        assertTrue(body.contains("\"totalJobs\":1"), body);
        assertTrue(body.contains("\"newJobsInPeriod\":1"), body);
    }

    private User addTa(DataService ds, String username, String school) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = "TA";
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = "SID_" + username;
        user.school = school;
        return ds.addUser(user);
    }

    private User addMo(DataService ds, String username) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = "MO";
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = username;
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
        job.weeklyHours = weeklyHours;
        job.deadline = "2099-12-31";
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
