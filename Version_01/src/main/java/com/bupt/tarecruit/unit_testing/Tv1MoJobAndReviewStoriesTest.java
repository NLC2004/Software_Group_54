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

class Tv1MoJobAndReviewStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void mo01_createJobShouldAllowMoToPostTask() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "tv1_post_mo");
        String token = ds.createSession(mo.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/jobs",
                null,
                "{\"title\":\"TV1 Lab Support\",\"type\":\"COURSE\",\"courseName\":\"Software Engineering\",\"description\":\"Need weekly lab support\",\"quota\":2,\"schedule\":\"Tue 10:00\",\"deadline\":\"2099-12-31\",\"salary\":\"100\",\"weeklyHours\":6,\"requirements\":[\"Java\",\"Communication\"]}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        Job stored = ds.getAllJobs().stream()
                .filter(job -> mo.id.equals(job.postedBy) && "TV1 Lab Support".equals(job.title))
                .findFirst()
                .orElse(null);
        assertNotNull(stored);
        assertEquals("OPEN", stored.status);
        assertEquals("Software Engineering", stored.courseName);
    }

    @Test
    void mo01_updateJobShouldAllowOwnerToEditPostedTask() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "tv1_edit_mo");
        Job job = addJob(ds, mo, "Old TV1 Job", "COURSE", 2);
        String token = ds.createSession(mo.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/jobs/" + job.id,
                null,
                "{\"title\":\"Updated TV1 Job\",\"weeklyHours\":8,\"status\":\"CLOSED\",\"salary\":\"120\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        Job updated = ds.getJobById(job.id);
        assertEquals("Updated TV1 Job", updated.title);
        assertEquals(8.0, updated.weeklyHours);
        assertEquals("CLOSED", updated.status);
        assertEquals("120", updated.salary);
    }

    @Test
    void mo01_deleteJobShouldRejectNonOwner() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User owner = addMo(ds, "tv1_owner_mo");
        User intruder = addMo(ds, "tv1_intruder_mo");
        Job job = addJob(ds, owner, "Protected TV1 Job", "COURSE", 1);
        String token = ds.createSession(intruder.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("DELETE", "/api/jobs/" + job.id, null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(403, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Not authorized"));
        assertNotNull(ds.getJobById(job.id));
    }

    @Test
    void mo11_createJobShouldSupportDifferentCourseTypes() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "tv1_type_mo");
        String token = ds.createSession(mo.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/jobs",
                null,
                "{\"title\":\"Seminar Assistant\",\"type\":\"ACTIVITY\",\"courseName\":\"Research Seminar\",\"description\":\"Event support\",\"quota\":1,\"schedule\":\"Fri\",\"deadline\":\"2099-12-31\",\"salary\":\"80\",\"weeklyHours\":3}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        Job stored = ds.getAllJobs().stream()
                .filter(job -> mo.id.equals(job.postedBy) && "Seminar Assistant".equals(job.title))
                .findFirst()
                .orElse(null);
        assertNotNull(stored);
        assertEquals("ACTIVITY", stored.type);
        assertEquals("Research Seminar", stored.courseName);
    }

    @Test
    void mo06_listApplicationsShouldShowOnlyOwnJobsApplicants() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo1 = addMo(ds, "tv1_review_mo1");
        User mo2 = addMo(ds, "tv1_review_mo2");
        User ta1 = addTa(ds, "tv1_review_ta1");
        User ta2 = addTa(ds, "tv1_review_ta2");
        Job job1 = addJob(ds, mo1, "Database TA", "COURSE", 2);
        Job job2 = addJob(ds, mo2, "Networks TA", "COURSE", 1);
        ds.addApplication(buildApplication(ta1.id, job1.id, 1, "cv_db.pdf"));
        ds.addApplication(buildApplication(ta2.id, job2.id, 2, "cv_net.pdf"));

        String token = ds.createSession(mo1.id);
        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/applications", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("Database TA"));
        assertTrue(body.contains("cv_db.pdf"));
        assertFalse(body.contains("Networks TA"));
    }

    @Test
    void mo07_approveShouldUpdateStatusAndCloseJobWhenQuotaReached() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "tv1_approve_mo");
        User ta = addTa(ds, "tv1_approve_ta");
        Job job = addJob(ds, mo, "Approval Job", "COURSE", 1);
        Application app = ds.addApplication(buildApplication(ta.id, job.id, 1, "approve.pdf"));
        String token = ds.createSession(mo.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/applications/" + app.id + "/status",
                null,
                "{\"status\":\"APPROVED\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertEquals("APPROVED", ds.getApplicationById(app.id).status);
        assertEquals("CLOSED", ds.getJobById(job.id).status);
    }

    @Test
    void mo07_approveShouldRejectWhenQuotaAlreadyReached() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "tv1_quota_mo");
        User ta1 = addTa(ds, "tv1_quota_ta1");
        User ta2 = addTa(ds, "tv1_quota_ta2");
        Job job = addJob(ds, mo, "Quota Job", "COURSE", 1);

        Application approved = ds.addApplication(buildApplication(ta1.id, job.id, 1, "approved.pdf"));
        approved.status = "APPROVED";
        ds.updateApplication(approved);
        Application pending = ds.addApplication(buildApplication(ta2.id, job.id, 2, "pending.pdf"));

        String token = ds.createSession(mo.id);
        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/applications/" + pending.id + "/status",
                null,
                "{\"status\":\"APPROVED\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Quota reached"));
        assertEquals("PENDING", ds.getApplicationById(pending.id).status);
    }

    @Test
    void mo07_rejectShouldUpdateStatus() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "tv1_reject_mo");
        User ta = addTa(ds, "tv1_reject_ta");
        Job job = addJob(ds, mo, "Reject Job", "COURSE", 2);
        Application app = ds.addApplication(buildApplication(ta.id, job.id, 1, "reject.pdf"));
        String token = ds.createSession(mo.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/applications/" + app.id + "/status",
                null,
                "{\"status\":\"REJECTED\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertEquals("REJECTED", ds.getApplicationById(app.id).status);
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

    private Job addJob(DataService ds, User mo, String title, String type, int quota) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = title;
        job.type = type;
        job.courseName = title + " Course";
        job.description = "desc";
        job.quota = quota;
        job.schedule = "Mon";
        job.deadline = "2099-12-31";
        job.salary = "100";
        job.weeklyHours = 5;
        return ds.addJob(job);
    }

    private Application buildApplication(String applicantId, String jobId, int priority, String cvFileName) {
        Application app = new Application();
        app.applicantId = applicantId;
        app.jobId = jobId;
        app.priority = priority;
        app.cvFileName = cvFileName;
        app.coverLetter = "cover";
        return app;
    }
}
