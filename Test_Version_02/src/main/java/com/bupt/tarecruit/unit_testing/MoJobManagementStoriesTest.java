package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.JobHandler;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MoJobManagementStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void mo01_createJobShouldAllowMoToPostTask() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "teacher_post");
        String token = ds.createSession(mo.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/jobs",
                null,
                "{\"title\":\"TA Lab Support\",\"type\":\"COURSE\",\"courseName\":\"Software Engineering\",\"description\":\"Need weekly lab support\",\"quota\":2,\"schedule\":\"Tue 10:00\",\"weeklyHours\":6,\"deadline\":\"2099-12-31\",\"requirements\":[\"Java\",\"Communication\"]}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        Job stored = ds.getAllJobs().stream()
                .filter(job -> mo.id.equals(job.postedBy) && "TA Lab Support".equals(job.title))
                .findFirst()
                .orElse(null);
        assertNotNull(stored);
        assertEquals(mo.id, stored.postedBy);
        assertEquals("TA Lab Support", stored.title);
        assertEquals("OPEN", stored.status);
    }

    @Test
    void mo01_updateJobShouldAllowOwnerToEditPostedTask() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "teacher_edit");
        Job job = addJob(ds, mo, "Old Title", "COURSE");
        String token = ds.createSession(mo.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/jobs/" + job.id,
                null,
                "{\"title\":\"Updated Title\",\"weeklyHours\":8,\"status\":\"CLOSED\",\"type\":\"ACTIVITY\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        Job updated = ds.getJobById(job.id);
        assertEquals("Updated Title", updated.title);
        assertEquals(8.0, updated.weeklyHours);
        assertEquals("CLOSED", updated.status);
        assertEquals("ACTIVITY", updated.type);
    }

    @Test
    void mo01_deleteJobShouldRejectNonOwner() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User owner = addMo(ds, "teacher_owner");
        User intruder = addMo(ds, "teacher_intruder");
        Job job = addJob(ds, owner, "Protected Job", "COURSE");
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
    void mo01_listJobsShouldSupportPostedByAndSearchFiltering() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo1 = addMo(ds, "teacher_filter_1");
        User mo2 = addMo(ds, "teacher_filter_2");
        addJob(ds, mo1, "Database TA", "COURSE");
        addJob(ds, mo2, "Networks TA", "COURSE");

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/jobs", "postedBy=" + mo1.id + "&search=database", null);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("Database TA"));
        assertFalse(body.contains("Networks TA"));
    }

    @Test
    void mo11_createJobShouldSupportDifferentCourseTypes() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addMo(ds, "teacher_types");
        String token = ds.createSession(mo.id);

        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/jobs",
                null,
                "{\"title\":\"Seminar Assistant\",\"type\":\"ACTIVITY\",\"courseName\":\"Research Seminar\",\"description\":\"Event support\",\"quota\":1,\"schedule\":\"Fri\",\"weeklyHours\":3,\"deadline\":\"2099-12-31\"}"
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

    private Job addJob(DataService ds, User mo, String title, String type) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = title;
        job.type = type;
        job.courseName = title + " Course";
        job.description = "desc";
        job.quota = 1;
        job.schedule = "Mon";
        job.weeklyHours = 5;
        job.deadline = "2099-12-31";
        return ds.addJob(job);
    }
}
