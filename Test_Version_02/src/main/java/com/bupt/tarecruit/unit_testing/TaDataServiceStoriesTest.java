package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.DraftHandler;
import com.bupt.tarecruit.handler.UploadHandler;
import com.bupt.tarecruit.model.ApplicationDraft;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.PasswordResetRequest;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.Base64;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TaDataServiceStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void ta01_profileUpdateShouldPersistPersonalInformation() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = new User();
        ta.username = "ta_profile";
        ta.password = "pass123";
        ta.role = "TA";
        ta.fullName = "Before Name";
        ta.email = "before@example.com";
        ta.studentId = "BUPT0001";
        ta = ds.addUser(ta);

        ta.fullName = "Alice Zhang";
        ta.phone = "18800001111";
        ta.gender = "Female";
        ta.school = "School of Computer Science";
        ta.supervisor = "Dr. Wang";
        ta.degree = "Master";
        ta.yearOfStudy = "Year 2";
        ds.updateUser(ta);

        User saved = ds.getUserById(ta.id);
        assertNotNull(saved);
        assertEquals("Alice Zhang", saved.fullName);
        assertEquals("18800001111", saved.phone);
        assertEquals("Female", saved.gender);
        assertEquals("School of Computer Science", saved.school);
        assertEquals("Dr. Wang", saved.supervisor);
        assertEquals("Master", saved.degree);
        assertEquals("Year 2", saved.yearOfStudy);
    }

    @Test
    void ta05_passwordResetRequestShouldBeStoredAsPending() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        PasswordResetRequest req = new PasswordResetRequest();
        req.studentId = "BUPT2025001";
        req.fullName = "Reset User";
        req.email = "reset@example.com";
        req.phone = "17712345678";
        req.notes = "Lost access to previous email";

        PasswordResetRequest saved = ds.addPasswordReset(req);

        assertNotNull(saved.id);
        assertFalse(saved.id.isBlank());
        assertEquals("PENDING", saved.status);
        assertTrue(saved.createdAt > 0);

        PasswordResetRequest stored = ds.getPasswordResetById(saved.id);
        assertNotNull(stored);
        assertEquals("Reset User", stored.fullName);
        assertEquals("PENDING", stored.status);
    }

    @Test
    void ta09_saveDraftShouldOverwriteExistingDraftForSameUserAndJob() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        ApplicationDraft first = new ApplicationDraft();
        first.userId = "ta001";
        first.jobId = "job001";
        first.confirmFullName = "Alice";
        first.confirmStudentId = "SID001";
        first.coverLetter = "first draft";
        first.priority = 1;
        first.resumeDraftFileName = "resume_v1.pdf";
        first = ds.saveApplicationDraft(first);

        ApplicationDraft second = new ApplicationDraft();
        second.userId = "ta001";
        second.jobId = " job001 ";
        second.confirmFullName = "Alice Updated";
        second.confirmStudentId = "SID001";
        second.coverLetter = "updated draft";
        second.priority = 2;
        second.resumeDraftFileName = "resume_v2.pdf";
        ds.saveApplicationDraft(second);

        ApplicationDraft stored = ds.getApplicationDraft("ta001", "job001");
        assertNotNull(stored);
        assertEquals("Alice Updated", stored.confirmFullName);
        assertEquals("updated draft", stored.coverLetter);
        assertEquals(2, stored.priority);
        assertEquals("resume_v2.pdf", stored.resumeDraftFileName);
        assertEquals(1, ds.getAllApplicationDrafts().stream().filter(d -> "ta001".equals(d.userId)).count());
        assertNotNull(stored.id);
        assertFalse(stored.id.isBlank());
    }

    @Test
    void ta09_deleteDraftShouldRespectNormalizedJobId() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        ApplicationDraft draft = new ApplicationDraft();
        draft.userId = "ta002";
        draft.jobId = " job-delete ";
        draft.coverLetter = "temporary";
        ds.saveApplicationDraft(draft);

        assertNotNull(ds.getApplicationDraft("ta002", "job-delete"));

        ds.deleteApplicationDraft("ta002", " job-delete ");

        assertNull(ds.getApplicationDraft("ta002", "job-delete"));
    }

    @Test
    void ta09_dashboardShouldListOnlyCurrentUsersDraftsWithJobInfo() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "draft_owner");
        User otherTa = addTa(ds, "draft_other");
        User mo = addMo(ds, "draft_mo");
        Job job = addJob(ds, mo, "Dashboard Draft Job");

        ApplicationDraft own = new ApplicationDraft();
        own.userId = ta.id;
        own.jobId = job.id;
        own.coverLetter = "unfinished";
        own.priority = 2;
        ds.saveApplicationDraft(own);

        ApplicationDraft other = new ApplicationDraft();
        other.userId = otherTa.id;
        other.jobId = job.id;
        other.coverLetter = "hidden";
        ds.saveApplicationDraft(other);

        String token = ds.createSession(ta.id);
        DraftHandler handler = new DraftHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/drafts/applications", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("Dashboard Draft Job"));
        assertTrue(body.contains("\"priority\":2"));
        assertFalse(body.contains("hidden"));
    }

    @Test
    void ta03_uploadStorageShouldSanitizeFileNameAndKeepBytes() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        byte[] content = "resume-pdf-content".getBytes();

        String savedName = ds.saveUpload("EBU6304 Intro?.pdf", content);

        assertTrue(savedName.endsWith("EBU6304_Intro_.pdf") || savedName.endsWith("EBU6304_Intro_.pdf".replace(" ", "_")) || savedName.contains("EBU6304_Intro_.pdf"));
        assertArrayEquals(content, ds.getUpload(savedName));
    }

    @Test
    void ta03_uploadApiShouldRejectNonPdfFile() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "upload_non_pdf");
        String token = ds.createSession(ta.id);
        String body = "{\"fileName\":\"resume.docx\",\"data\":\"" + Base64.getEncoder().encodeToString("doc".getBytes()) + "\"}";

        UploadHandler handler = new UploadHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("POST", "/api/upload", null, body);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("only PDF files are allowed"));
    }

    @Test
    void ta03_uploadApiShouldRejectInvalidBase64() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "upload_invalid_base64");
        String token = ds.createSession(ta.id);

        UploadHandler handler = new UploadHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("POST", "/api/upload", null, "{\"fileName\":\"resume.pdf\",\"data\":\"not-base64%%%\"}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Upload failed"));
    }

    @Test
    void ta03_uploadApiShouldRejectEmptyFile() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "upload_empty");
        String token = ds.createSession(ta.id);

        UploadHandler handler = new UploadHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("POST", "/api/upload", null, "{\"fileName\":\"resume.pdf\",\"data\":\"\"}");
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("file content is empty"));
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

    private Job addJob(DataService ds, User mo, String title) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = title;
        job.type = "COURSE_TA";
        job.courseName = title + " Course";
        job.description = "desc";
        job.quota = 1;
        job.deadline = "2099-12-31";
        return ds.addJob(job);
    }
}
