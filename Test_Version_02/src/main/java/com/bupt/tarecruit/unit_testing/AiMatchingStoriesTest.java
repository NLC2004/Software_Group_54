package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.JobHandler;
import com.bupt.tarecruit.model.Application;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies deterministic AI-assisted matching behavior, including explanation
 * output, persistence visibility, model validation and per-TA usage limits.
 */
class AiMatchingStoriesTest {

    @TempDir
    Path tempDir;

    /**
     * Enables deterministic mock matching so unit tests make no network calls
     * and receive stable model responses suitable for assertions.
     */
    @BeforeEach
    void enableMockAiApi() {
        System.setProperty("ta.ai.mockApi", "true");
    }

    /**
     * Removes the mock-system property after each test so it cannot influence
     * other test classes executed in the same JVM.
     */
    @AfterEach
    void clearMockAiApi() {
        System.clearProperty("ta.ai.mockApi");
    }

    /**
     * Requests a TA match result and confirms that the response explains
     * matched skills, gaps, scoring and recommendation content.
     */
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

    /**
     * Verifies that an MO applicant list contains no artificial AI summary
     * before matching, then displays the saved analysis after matching runs.
     */
    @Test
    void moApplicationListShouldOnlyIncludeSavedAiMatchAfterRun() throws Exception {
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
        assertFalse(ex.getResponseBodyAsString().contains("\"aiMatch\""));

        TestHttpExchange matchEx = new TestHttpExchange(
                "POST",
                "/api/jobs/" + job.id + "/match",
                null,
                "{\"applicationId\":\"" + app.id + "\",\"model\":\"gpt-5-mini\"}"
        );
        matchEx.setBearerToken(token);
        handler.handle(matchEx);
        assertEquals(200, matchEx.getResponseCode());
        assertNotNull(ds.getApplicationById(app.id).aiMatchJson);

        TestHttpExchange listAfterMatch = new TestHttpExchange("GET", "/api/jobs/" + job.id + "/applications", null, null);
        listAfterMatch.setBearerToken(token);
        handler.handle(listAfterMatch);

        assertEquals(200, listAfterMatch.getResponseCode());
        String body = listAfterMatch.getResponseBodyAsString();
        assertTrue(body.contains("\"aiMatch\""));
        assertTrue(body.contains("\"score\""));
        assertTrue(body.contains("\"workloadRisk\""));
    }

    /**
     * Invokes matching repeatedly as one TA and verifies that only three
     * successful uses are allowed before quota enforcement occurs.
     */
    @Test
    void taAiMatchShouldBeLimitedToThreeSuccessfulCalls() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ai_limited_ta");
        User mo = addMo(ds, "ai_limited_mo");
        Job job = addSkillJob(ds, mo);
        String token = ds.createSession(ta.id);
        JobHandler handler = new JobHandler(ds);

        for (int i = 0; i < 3; i++) {
            TestHttpExchange ok = new TestHttpExchange(
                    "POST",
                    "/api/jobs/" + job.id + "/match",
                    null,
                    "{\"coverLetter\":\"Java programming and communication.\",\"model\":\"qwen-plus\"}"
            );
            ok.setBearerToken(token);
            handler.handle(ok);
            assertEquals(200, ok.getResponseCode());
        }

        TestHttpExchange blocked = new TestHttpExchange(
                "POST",
                "/api/jobs/" + job.id + "/match",
                null,
                "{\"coverLetter\":\"Try one more time.\",\"model\":\"qwen-plus\"}"
        );
        blocked.setBearerToken(token);
        handler.handle(blocked);

        assertEquals(429, blocked.getResponseCode());
        assertTrue(blocked.getResponseBodyAsString().contains("AI match limit reached"));
    }

    /**
     * Requests matching through an unsupported model value and verifies that
     * model validation rejects it before any result is accepted.
     */
    @Test
    void aiMatchShouldRejectUnsupportedModel() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ai_bad_model_ta");
        User mo = addMo(ds, "ai_bad_model_mo");
        Job job = addSkillJob(ds, mo);
        String token = ds.createSession(ta.id);

        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/jobs/" + job.id + "/match",
                null,
                "{\"coverLetter\":\"test\",\"model\":\"deepseek-r1\"}"
        );
        ex.setBearerToken(token);
        new JobHandler(ds).handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Invalid AI model"));
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
