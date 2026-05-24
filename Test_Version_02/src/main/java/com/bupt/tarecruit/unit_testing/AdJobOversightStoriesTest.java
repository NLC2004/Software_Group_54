package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.JobHandler;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies administrator oversight of MO-created postings, including global
 * edit/delete authority and visibility of applications for any vacancy.
 */
class AdJobOversightStoriesTest {

    @TempDir
    Path tempDir;

    /**
     * Updates a posting owned by an organiser while authenticated as admin and
     * verifies that administrative oversight can correct posting data.
     */
    @Test
    void ad03_adminShouldUpdateAnyJobPosting() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addMo(ds, "owner_mo");
        Job job = addJob(ds, mo, "Original Job");
        String token = ds.createSession(admin.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/jobs/" + job.id,
                null,
                "{\"title\":\"Admin Edited Job\",\"status\":\"CLOSED\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        Job updated = ds.getJobById(job.id);
        assertEquals("Admin Edited Job", updated.title);
        assertEquals("CLOSED", updated.status);
    }

    /**
     * Deletes an organiser-owned posting as admin and verifies its removal
     * regardless of original ownership.
     */
    @Test
    void ad03_adminShouldDeleteAnyJobPosting() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addMo(ds, "owner_delete_mo");
        Job job = addJob(ds, mo, "Delete Me");
        String token = ds.createSession(admin.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("DELETE", "/api/jobs/" + job.id, null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertNull(ds.getJobById(job.id));
    }

    /**
     * Lists applications under an organiser's posting as admin to verify the
     * cross-posting monitoring permission required for oversight.
     */
    @Test
    void ad03_adminShouldViewApplicationsForAnyJob() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addMo(ds, "owner_view_mo");
        User ta = addTa(ds, "applicant_ta");
        Job job = addJob(ds, mo, "View Apps Job");
        com.bupt.tarecruit.model.Application app = new com.bupt.tarecruit.model.Application();
        app.jobId = job.id;
        app.applicantId = ta.id;
        app.priority = 1;
        app.status = "PENDING";
        app.cvFileName = "cv_admin_view.pdf";
        ds.addApplication(app);
        String token = ds.createSession(admin.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/jobs/" + job.id + "/applications", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("cv_admin_view.pdf"));
        assertTrue(body.contains(ta.email));
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

    private Job addJob(DataService ds, User mo, String title) {
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
}
