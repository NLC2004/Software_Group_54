package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.JobHandler;
import com.bupt.tarecruit.model.Application;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiMatchingStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void aiMatchShouldExplainMatchedAndMissingSkillsForTa() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ai_ta");
        User mo = addMo(ds, "ai_mo");
        Job job = addSkillJob(ds, mo);

        String token = ds.createSession(ta.id);
        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/jobs/" + job.id + "/match",
                null,
                "{\"coverLetter\":\"I have Java programming experience and strong communication skills.\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("\"matchedSkills\""));
        assertTrue(body.contains("programming"));
        assertTrue(body.contains("communication"));
        assertTrue(body.contains("\"missingSkills\""));
        assertTrue(body.contains("laboratory"));
        assertTrue(body.contains("\"recommendation\""));
    }

    @Test
    void moApplicationListShouldIncludeAiMatchSummary() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ai_applicant");
        User mo = addMo(ds, "ai_owner");
        Job job = addSkillJob(ds, mo);
        Application app = new Application();
        app.applicantId = ta.id;
        app.jobId = job.id;
        app.priority = 1;
        app.coverLetter = "Java programming, communication, and tutoring experience.";
        ds.addApplication(app);

        String token = ds.createSession(mo.id);
        JobHandler handler = new JobHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/jobs/" + job.id + "/applications", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("\"aiMatch\""));
        assertTrue(body.contains("\"score\""));
        assertTrue(body.contains("\"workloadRisk\""));
    }

    private User addTa(DataService ds, String username) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = "TA";
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.phone = "18800001111";
        user.studentId = "SID_" + username;
        user.school = "School of Computer Science";
        user.degree = "Master";
        user.yearOfStudy = "Year 2";
        user.gender = "Female";
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

    private Job addSkillJob(DataService ds, User mo) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = "Programming Lab Assistant";
        job.type = "LAB_TA";
        job.courseName = "Software Engineering Lab";
        job.description = "Support students in coding labs and answer student questions.";
        job.requirements = List.of("Java programming ability", "Good communication", "Laboratory experience");
        job.quota = 1;
        job.weeklyHours = 4;
        job.deadline = "2099-12-31";
        return ds.addJob(job);
    }
}
