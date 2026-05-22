package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.ApplicationHandler;
import com.bupt.tarecruit.handler.JobHandler;
import com.bupt.tarecruit.model.Application;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TaApplicationFlowTest {

    @TempDir
    Path tempDir;

    @Test
    void ta12_applyShouldRejectPriorityOutsideRange() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_priority_invalid");
        User mo = addMo(ds, "mo_priority_invalid");
        Job job = addOpenJob(ds, mo, "job-priority-invalid");
        String token = ds.createSession(ta.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("POST", "/api/jobs/" + job.id + "/apply", null, "{\"coverLetter\":\"test\",\"priority\":0}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Priority must be 1, 2, or 3"));
        assertEquals(0, ds.getApplicationsByApplicant(ta.id).size());
    }

    @Test
    void ta12_applyShouldRejectDuplicatePriorityAmongActiveApplications() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_duplicate_priority");
        User mo = addMo(ds, "mo_duplicate_priority");
        Job job1 = addOpenJob(ds, mo, "job-pri-1");
        Job job2 = addOpenJob(ds, mo, "job-pri-2");
        String token = ds.createSession(ta.id);

        Application existing = new Application();
        existing.jobId = job1.id;
        existing.applicantId = ta.id;
        existing.priority = 1;
        ds.addApplication(existing);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("POST", "/api/jobs/" + job2.id + "/apply", null, "{\"coverLetter\":\"test\",\"priority\":1}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(409, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("already used by another active application"));
        assertEquals(1, ds.getApplicationsByApplicant(ta.id).size());
    }

    @Test
    void ta12_applyShouldRejectWhenThreeActiveApplicationsAlreadyExist() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_three_active");
        User mo = addMo(ds, "mo_three_active");
        Job job1 = addOpenJob(ds, mo, "job-a");
        Job job2 = addOpenJob(ds, mo, "job-b");
        Job job3 = addOpenJob(ds, mo, "job-c");
        Job job4 = addOpenJob(ds, mo, "job-d");
        String token = ds.createSession(ta.id);

        ds.addApplication(buildApplication(ta.id, job1.id, 1, "PENDING"));
        ds.addApplication(buildApplication(ta.id, job2.id, 2, "APPROVED"));
        ds.addApplication(buildApplication(ta.id, job3.id, 3, "PENDING"));

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("POST", "/api/jobs/" + job4.id + "/apply", null, "{\"coverLetter\":\"test\",\"priority\":1}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(409, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("already have 3 active applications"));
        assertEquals(3, ds.getApplicationsByApplicant(ta.id).size());
    }

    @Test
    void ta12_applyShouldRejectExpiredJobDeadline() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_expired_deadline");
        User mo = addMo(ds, "mo_expired_deadline");
        Job job = addOpenJob(ds, mo, "job-expired-deadline");
        job.deadline = "2000-01-01";
        ds.updateJob(job);
        String token = ds.createSession(ta.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("POST", "/api/jobs/" + job.id + "/apply", null, "{\"coverLetter\":\"test\",\"priority\":1}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("past the application deadline"));
        assertEquals(0, ds.getApplicationsByApplicant(ta.id).size());
    }

    @Test
    void ta12_applyShouldRejectJobThatReachedQuota() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_full_quota");
        User approvedTa = addTa(ds, "ta_full_quota_existing");
        User mo = addMo(ds, "mo_full_quota");
        Job job = addOpenJob(ds, mo, "job-full-quota");
        persistApplication(ds, buildApplication(approvedTa.id, job.id, 1, "APPROVED"));
        String token = ds.createSession(ta.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("POST", "/api/jobs/" + job.id + "/apply", null, "{\"coverLetter\":\"test\",\"priority\":1}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(409, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("reached its quota"));
        assertEquals(0, ds.getApplicationsByApplicant(ta.id).size());
    }

    @Test
    void ta12_applyShouldAllowReusingPriorityFromWithdrawnOrRejectedApplications() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_reuse_inactive_priority");
        User mo = addMo(ds, "mo_reuse_inactive_priority");
        Job withdrawnJob = addOpenJob(ds, mo, "job-withdrawn-priority");
        Job rejectedJob = addOpenJob(ds, mo, "job-rejected-priority");
        Job newJob = addOpenJob(ds, mo, "job-new-priority");
        String token = ds.createSession(ta.id);

        persistApplication(ds, buildApplication(ta.id, withdrawnJob.id, 1, "WITHDRAWN"));
        persistApplication(ds, buildApplication(ta.id, rejectedJob.id, 2, "REJECTED"));

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("POST", "/api/jobs/" + newJob.id + "/apply", null, "{\"coverLetter\":\"test\",\"priority\":1}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        assertEquals(3, ds.getApplicationsByApplicant(ta.id).size());
        assertEquals(1, ds.getApplicationsByApplicant(ta.id).stream()
                .filter(a -> "PENDING".equals(a.status) && a.priority == 1)
                .count());
    }

    @Test
    void ta11_withdrawShouldAllowOwnApplication() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_withdraw_self");
        User mo = addMo(ds, "mo_withdraw_self");
        Job job = addOpenJob(ds, mo, "job-withdraw-self");
        Application app = ds.addApplication(buildApplication(ta.id, job.id, 1, "PENDING"));
        String token = ds.createSession(ta.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("PUT", "/api/applications/" + app.id + "/status", null, "{\"status\":\"WITHDRAWN\"}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Status updated"));
        assertEquals("WITHDRAWN", ds.getApplicationById(app.id).status);
    }

    @Test
    void ta11_withdrawShouldRejectNonPendingApplication() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_withdraw_non_pending");
        User mo = addMo(ds, "mo_withdraw_non_pending");
        Job job = addOpenJob(ds, mo, "job-withdraw-non-pending");
        Application app = persistApplication(ds, buildApplication(ta.id, job.id, 1, "APPROVED"));
        String token = ds.createSession(ta.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("PUT", "/api/applications/" + app.id + "/status", null, "{\"status\":\"WITHDRAWN\"}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Only pending applications can be withdrawn"));
        assertEquals("APPROVED", ds.getApplicationById(app.id).status);
    }

    @Test
    void ta11_withdrawShouldRejectOtherUsersApplication() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User owner = addTa(ds, "ta_owner");
        User attacker = addTa(ds, "ta_attacker");
        User mo = addMo(ds, "mo_withdraw_other");
        Job job = addOpenJob(ds, mo, "job-withdraw-other");
        Application app = ds.addApplication(buildApplication(owner.id, job.id, 1, "PENDING"));
        String token = ds.createSession(attacker.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("PUT", "/api/applications/" + app.id + "/status", null, "{\"status\":\"WITHDRAWN\"}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(403, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Not your application"));
        assertEquals("PENDING", ds.getApplicationById(app.id).status);
    }

    private User addTa(DataService ds, String username) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = "TA";
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = "SID_" + username;
        return ds.addUser(user);
    }

    private User addMo(DataService ds, String username) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = "MO";
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = "MO_" + username;
        return ds.addUser(user);
    }

    private Job addOpenJob(DataService ds, User mo, String title) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = title;
        job.type = "COURSE";
        job.courseName = title + " Course";
        job.description = "desc";
        job.quota = 1;
        job.weeklyHours = 5;
        job.schedule = "Mon";
        job.deadline = "2099-12-31";
        return ds.addJob(job);
    }

    private Application buildApplication(String applicantId, String jobId, int priority, String status) {
        Application app = new Application();
        app.applicantId = applicantId;
        app.jobId = jobId;
        app.priority = priority;
        app.coverLetter = "cover";
        app.cvFileName = "cv.pdf";
        app.status = status;
        return app;
    }

    private Application persistApplication(DataService ds, Application app) {
        String desiredStatus = app.status;
        Application saved = ds.addApplication(app);
        saved.status = desiredStatus;
        ds.updateApplication(saved);
        return saved;
    }
}
