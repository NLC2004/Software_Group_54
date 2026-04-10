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

class Tv1TaApplicationFlowTest {

    @TempDir
    Path tempDir;

    @Test
    void ta07_listApplicationsShouldReturnOnlyCurrentTasApplications() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta1 = addTa(ds, "tv1_list_ta1");
        User ta2 = addTa(ds, "tv1_list_ta2");
        User mo = addMo(ds, "tv1_list_mo");
        Job job1 = addJob(ds, mo, "Database TA", 2);
        Job job2 = addJob(ds, mo, "Networks TA", 1);
        ds.addApplication(buildApplication(ta1.id, job1.id, 1, "cv1.pdf"));
        ds.addApplication(buildApplication(ta2.id, job2.id, 2, "cv2.pdf"));

        String token = ds.createSession(ta1.id);
        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/applications", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("Database TA"));
        assertFalse(body.contains("Networks TA"));
    }

    @Test
    void ta11_withdrawShouldAllowOwnPendingApplication() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "tv1_withdraw_ta");
        User mo = addMo(ds, "tv1_withdraw_mo");
        Job job = addJob(ds, mo, "Withdrawable Job", 1);
        Application app = ds.addApplication(buildApplication(ta.id, job.id, 1, "withdraw.pdf"));

        String token = ds.createSession(ta.id);
        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/applications/" + app.id + "/status",
                null,
                "{\"status\":\"WITHDRAWN\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertEquals("WITHDRAWN", ds.getApplicationById(app.id).status);
    }

    @Test
    void ta11_withdrawShouldRejectOtherTasApplication() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User owner = addTa(ds, "tv1_owner_ta");
        User intruder = addTa(ds, "tv1_intruder_ta");
        User mo = addMo(ds, "tv1_owner_mo");
        Job job = addJob(ds, mo, "Protected Job", 1);
        Application app = ds.addApplication(buildApplication(owner.id, job.id, 1, "protected.pdf"));

        String token = ds.createSession(intruder.id);
        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/applications/" + app.id + "/status",
                null,
                "{\"status\":\"WITHDRAWN\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(403, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Not your application"));
    }

    @Test
    void applyShouldRejectClosedJob() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "tv1_apply_closed_ta");
        User mo = addMo(ds, "tv1_apply_closed_mo");
        Job job = addJob(ds, mo, "Closed Job", 1);
        job.status = "CLOSED";
        ds.updateJob(job);

        String token = ds.createSession(ta.id);
        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/jobs/" + job.id + "/apply",
                null,
                "{\"coverLetter\":\"Please accept\",\"cvFileName\":\"cv.pdf\",\"priority\":1}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("no longer accepting applications"));
    }

    @Test
    void applyShouldRejectDuplicateNonWithdrawnApplication() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "tv1_apply_dup_ta");
        User mo = addMo(ds, "tv1_apply_dup_mo");
        Job job = addJob(ds, mo, "Duplicate Job", 1);
        ds.addApplication(buildApplication(ta.id, job.id, 1, "existing.pdf"));

        String token = ds.createSession(ta.id);
        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/jobs/" + job.id + "/apply",
                null,
                "{\"coverLetter\":\"Retry\",\"cvFileName\":\"retry.pdf\",\"priority\":2}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(409, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("already applied"));
    }

    @Test
    void applyShouldReturnPrefillProfileData() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "tv1_prefill_ta");
        ta.studentId = "20240009";
        ta.fullName = "Prefill TA";
        ta.email = "prefill@example.com";
        ta.phone = "18899990000";
        ta.gender = "F";
        ds.updateUser(ta);
        User mo = addMo(ds, "tv1_prefill_mo");
        Job job = addJob(ds, mo, "Prefill Job", 2);

        String token = ds.createSession(ta.id);
        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/jobs/" + job.id + "/apply",
                null,
                "{\"coverLetter\":\"I am interested\",\"cvFileName\":\"prefill.pdf\",\"priority\":1}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("prefillProfile"));
        assertTrue(body.contains("20240009"));
        assertTrue(body.contains("Prefill TA"));
        assertTrue(body.contains("prefill@example.com"));
        assertTrue(body.contains("18899990000"));
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
        user.studentId = username;
        return ds.addUser(user);
    }

    private Job addJob(DataService ds, User mo, String title, int quota) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = title;
        job.type = "COURSE";
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
