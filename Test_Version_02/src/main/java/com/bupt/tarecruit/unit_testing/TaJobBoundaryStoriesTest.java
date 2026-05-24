package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.JobHandler;
import com.bupt.tarecruit.handler.UploadHandler;
import com.bupt.tarecruit.model.Application;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaJobBoundaryStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void applicationShouldLinkUploadedCvAndNotifyJobOwner() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addUser(ds, "linked_cv_ta", "TA");
        User mo = addUser(ds, "linked_cv_mo", "MO");
        Job job = addJob(ds, mo, "CV Linked Position", "COURSE_TA", "OPEN");
        String token = ds.createSession(ta.id);

        UploadHandler uploadHandler = new UploadHandler(ds);
        String encoded = Base64.getEncoder().encodeToString("%PDF-linked".getBytes());
        TestHttpExchange upload = new TestHttpExchange("POST", "/api/upload", null,
                "{\"fileName\":\"linked resume.pdf\",\"data\":\"" + encoded + "\"}");
        upload.setBearerToken(token);
        uploadHandler.handle(upload);
        assertEquals(200, upload.getResponseCode());
        String savedName = upload.getResponseBodyAsString()
                .replaceAll(".*\"fileName\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        JobHandler jobHandler = new JobHandler(ds);
        TestHttpExchange apply = new TestHttpExchange("POST", "/api/jobs/" + job.id + "/apply", null,
                "{\"priority\":1,\"coverLetter\":\"Ready to assist\",\"cvFileName\":\"" + savedName + "\"}");
        apply.setBearerToken(token);
        jobHandler.handle(apply);

        assertEquals(201, apply.getResponseCode());
        Application stored = ds.getApplicationsByApplicant(ta.id).stream()
                .filter(a -> job.id.equals(a.jobId)).findFirst().orElse(null);
        assertNotNull(stored);
        assertEquals(savedName, stored.cvFileName);
        assertTrue(ds.getNotificationsByUser(mo.id).stream()
                .anyMatch(n -> "New Application".equals(n.title) && n.content.contains(job.title)));
    }

    @Test
    void taShouldNotApplyForClosedPosition() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addUser(ds, "closed_ta", "TA");
        User mo = addUser(ds, "closed_mo", "MO");
        Job job = addJob(ds, mo, "Already Closed", "COURSE_TA", "CLOSED");

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange apply = new TestHttpExchange("POST", "/api/jobs/" + job.id + "/apply", null,
                "{\"priority\":1}");
        apply.setBearerToken(ds.createSession(ta.id));
        handler.handle(apply);

        assertEquals(400, apply.getResponseCode());
        assertTrue(apply.getResponseBodyAsString().contains("no longer accepting"));
        assertTrue(ds.getApplicationsByApplicant(ta.id).isEmpty());
    }

    @Test
    void vacancyListShouldFilterTypeAndStatusAndExposeApplicationCounts() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addUser(ds, "filter_mo", "MO");
        User ta = addUser(ds, "filter_ta", "TA");
        Job target = addJob(ds, mo, "Closed Course Match", "COURSE_TA", "CLOSED");
        addJob(ds, mo, "Open Lab Excluded", "LAB_TA", "OPEN");
        Application application = new Application();
        application.jobId = target.id;
        application.applicantId = ta.id;
        application.priority = 1;
        ds.addApplication(application);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange list = new TestHttpExchange("GET", "/api/jobs",
                "type=COURSE_TA&status=CLOSED", null);
        handler.handle(list);

        assertEquals(200, list.getResponseCode());
        assertTrue(list.getResponseBodyAsString().contains("Closed Course Match"));
        assertTrue(list.getResponseBodyAsString().contains("\"applicationCount\":1"));
        assertFalse(list.getResponseBodyAsString().contains("Open Lab Excluded"));
    }

    @Test
    void applicationShouldBeBlockedWhenApprovedWorkloadWouldExceedWeeklyLimit() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        Map<String, String> settings = new HashMap<>(ds.getSettings());
        settings.put("maxWeeklyHours", "2");
        ds.updateSettings(settings);
        User ta = addUser(ds, "hours_ta", "TA");
        User mo = addUser(ds, "hours_mo", "MO");
        Job approvedJob = addJob(ds, mo, "Approved Hours", "COURSE_TA", "OPEN");
        approvedJob.weeklyHours = 2;
        ds.updateJob(approvedJob);
        Application approved = new Application();
        approved.jobId = approvedJob.id;
        approved.applicantId = ta.id;
        approved.priority = 1;
        approved = ds.addApplication(approved);
        approved.status = "APPROVED";
        ds.updateApplication(approved);
        Job candidate = addJob(ds, mo, "Overflow Hours", "COURSE_TA", "OPEN");
        candidate.weeklyHours = 1;
        ds.updateJob(candidate);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange apply = new TestHttpExchange("POST", "/api/jobs/" + candidate.id + "/apply", null,
                "{\"priority\":2,\"coverLetter\":\"available\"}");
        apply.setBearerToken(ds.createSession(ta.id));
        handler.handle(apply);

        assertEquals(409, apply.getResponseCode());
        assertTrue(apply.getResponseBodyAsString().contains("Week 1"));
        assertTrue(ds.getNotificationsByUser(ta.id).stream().anyMatch(n -> "WORKLOAD".equals(n.type)));
        assertTrue(ds.getAllAuditLogs().stream().anyMatch(l -> "WORKLOAD_BLOCK".equals(l.action)));
    }

    @Test
    void taShouldNotCreateVacancyPosting() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addUser(ds, "posting_ta", "TA");
        JobHandler handler = new JobHandler(ds);
        TestHttpExchange create = new TestHttpExchange("POST", "/api/jobs", null,
                "{\"title\":\"Forbidden Position\",\"type\":\"COURSE_TA\",\"deadline\":\"2099-12-31\"}");
        create.setBearerToken(ds.createSession(ta.id));

        handler.handle(create);

        assertEquals(403, create.getResponseCode());
        assertTrue(ds.getAllJobs().stream().noneMatch(j -> "Forbidden Position".equals(j.title)));
    }

    @Test
    void moShouldNotSubmitTaApplication() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addUser(ds, "applying_mo", "MO");
        Job job = addJob(ds, mo, "MO Owned Job", "COURSE_TA", "OPEN");
        JobHandler handler = new JobHandler(ds);
        TestHttpExchange apply = new TestHttpExchange("POST", "/api/jobs/" + job.id + "/apply", null,
                "{\"priority\":1}");
        apply.setBearerToken(ds.createSession(mo.id));

        handler.handle(apply);

        assertEquals(403, apply.getResponseCode());
        assertTrue(ds.getApplicationsByApplicant(mo.id).isEmpty());
    }

    private User addUser(DataService ds, String username, String role) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = role;
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = "ID_" + username;
        return ds.addUser(user);
    }

    private Job addJob(DataService ds, User mo, String title, String type, String status) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = title;
        job.type = type;
        job.courseName = title + " Course";
        job.description = "desc";
        job.quota = 2;
        job.weeklyHours = 1;
        job.courseWeekStart = 1;
        job.courseWeekEnd = 1;
        job.deadline = "2099-12-31";
        Job saved = ds.addJob(job);
        saved.status = status;
        ds.updateJob(saved);
        return saved;
    }
}
