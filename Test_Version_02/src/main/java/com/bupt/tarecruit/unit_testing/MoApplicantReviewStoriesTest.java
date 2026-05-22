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

class MoApplicantReviewStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void mo06_viewApplicantsShouldReturnCvAndApplicantInfoForOwnJob() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "teacher_review");
        User ta = addTa(ds, "ta_review");
        Job job = addOpenJob(ds, mo, "Software Testing TA");
        Application app = new Application();
        app.jobId = job.id;
        app.applicantId = ta.id;
        app.cvFileName = "resume_review.pdf";
        app.coverLetter = "I can help with labs";
        app.priority = 1;
        ds.addApplication(app);
        String token = ds.createSession(mo.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/jobs/" + job.id + "/applications", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("resume_review.pdf"));
        assertTrue(body.contains(ta.email));
        assertTrue(body.contains(ta.studentId));
    }

    @Test
    void mo06_viewApplicantsShouldRejectOtherMosJob() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User owner = addMo(ds, "teacher_owner_review");
        User otherMo = addMo(ds, "teacher_other_review");
        Job job = addOpenJob(ds, owner, "Protected Review Job");
        String token = ds.createSession(otherMo.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/jobs/" + job.id + "/applications", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(403, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Not authorized"));
    }

    @Test
    void mo07_approveApplicantShouldUpdateStatusAndNotifyTa() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "teacher_approve");
        User ta = addTa(ds, "ta_approve");
        Job job = addOpenJob(ds, mo, "Approval Job");
        Application app = ds.addApplication(buildApplication(ta.id, job.id, 1, "cv_approve.pdf"));
        String token = ds.createSession(mo.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("PUT", "/api/applications/" + app.id + "/status", null, "{\"status\":\"APPROVED\"}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertEquals("APPROVED", ds.getApplicationById(app.id).status);
        assertTrue(ds.getNotificationsByUser(ta.id).stream().anyMatch(n -> "Application Approved".equals(n.title)));
    }

    @Test
    void mo07_approveApplicantShouldRejectWhenQuotaIsFull() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "teacher_quota_full");
        User approvedTa = addTa(ds, "ta_already_approved");
        User pendingTa = addTa(ds, "ta_pending_quota");
        Job job = addOpenJob(ds, mo, "Quota Full Job");
        Application approved = ds.addApplication(buildApplication(approvedTa.id, job.id, 1, "cv_approved.pdf"));
        approved.status = "APPROVED";
        ds.updateApplication(approved);
        Application pending = ds.addApplication(buildApplication(pendingTa.id, job.id, 2, "cv_pending.pdf"));
        String token = ds.createSession(mo.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("PUT", "/api/applications/" + pending.id + "/status", null, "{\"status\":\"APPROVED\"}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(409, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("reached its quota"));
        assertEquals("PENDING", ds.getApplicationById(pending.id).status);
    }

    @Test
    void mo07_rejectApplicantShouldUpdateStatusAndNotifyTa() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "teacher_reject");
        User ta = addTa(ds, "ta_reject");
        Job job = addOpenJob(ds, mo, "Rejection Job");
        Application app = ds.addApplication(buildApplication(ta.id, job.id, 2, "cv_reject.pdf"));
        String token = ds.createSession(mo.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("PUT", "/api/applications/" + app.id + "/status", null, "{\"status\":\"REJECTED\"}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertEquals("REJECTED", ds.getApplicationById(app.id).status);
        assertTrue(ds.getNotificationsByUser(ta.id).stream().anyMatch(n -> "Application Rejected".equals(n.title)));
    }

    @Test
    void mo07_shouldRejectInvalidMoStatusChange() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "teacher_invalid_status");
        User ta = addTa(ds, "ta_invalid_status");
        Job job = addOpenJob(ds, mo, "Invalid Status Job");
        Application app = ds.addApplication(buildApplication(ta.id, job.id, 1, "cv_invalid.pdf"));
        String token = ds.createSession(mo.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("PUT", "/api/applications/" + app.id + "/status", null, "{\"status\":\"WITHDRAWN\"}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Invalid status"));
        assertEquals("PENDING", ds.getApplicationById(app.id).status);
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

    private Job addOpenJob(DataService ds, User mo, String title) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = title;
        job.type = "COURSE";
        job.courseName = title + " Course";
        job.description = "desc";
        job.quota = 1;
        job.schedule = "Mon";
        job.weeklyHours = 5;
        job.deadline = "2099-12-31";
        return ds.addJob(job);
    }

    private Application buildApplication(String applicantId, String jobId, int priority, String cvFileName) {
        Application app = new Application();
        app.applicantId = applicantId;
        app.jobId = jobId;
        app.priority = priority;
        app.cvFileName = cvFileName;
        app.coverLetter = "cover";
        app.status = "PENDING";
        return app;
    }
}
