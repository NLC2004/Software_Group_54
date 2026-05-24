package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AdminHandler;
import com.bupt.tarecruit.handler.AuthHandler;
import com.bupt.tarecruit.handler.DraftHandler;
import com.bupt.tarecruit.handler.NotificationHandler;
import com.bupt.tarecruit.handler.UploadHandler;
import com.bupt.tarecruit.model.Notification;
import com.bupt.tarecruit.model.PasswordResetRequest;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedAuthenticationAndAccessTest {

    @TempDir
    Path tempDir;

    @Test
    void loginShouldRejectDeactivatedAccountWithoutIssuingAccess() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addUser(ds, "inactive_ta", "TA", "SID-INACTIVE");
        ta.active = false;
        ds.updateUser(ta);

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange login = new TestHttpExchange("POST", "/api/auth/login", null,
                "{\"identifier\":\"SID-INACTIVE\",\"password\":\"pass123\",\"portalRole\":\"TA\"}");
        handler.handle(login);

        assertEquals(403, login.getResponseCode());
        assertTrue(login.getResponseBodyAsString().contains("deactivated"));
        assertFalse(login.getResponseBodyAsString().contains("\"token\""));
    }

    @Test
    void moRegistrationShouldPersistTeacherIdAndExposeItFromCurrentUser() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange register = new TestHttpExchange("POST", "/api/auth/register", null,
                "{\"username\":\"mo_teacher_id\",\"password\":\"pass123\",\"role\":\"MO\","
                        + "\"fullName\":\"Registered MO\",\"email\":\"registered.mo@example.com\",\"teacherId\":\"T-9021\"}");

        handler.handle(register);

        assertEquals(201, register.getResponseCode());
        User stored = ds.getUserByUsername("mo_teacher_id");
        assertNotNull(stored);
        assertEquals("T-9021", stored.studentId);
        assertTrue(register.getResponseBodyAsString().contains("\"teacherId\":\"T-9021\""));
    }

    @Test
    void passwordResetRoleShouldTargetMoWhenTaAndMoShareIdentifier() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addUser(ds, "shared_reset_ta", "TA", "SHARED-110");
        User mo = addUser(ds, "shared_reset_mo", "MO", "SHARED-110");
        ta.password = "ta-old";
        mo.password = "mo-old";
        ta.email = "";
        mo.email = "";
        ds.updateUser(ta);
        ds.updateUser(mo);

        AuthHandler auth = new AuthHandler(ds);
        TestHttpExchange request = new TestHttpExchange("POST", "/api/auth/password-reset", null,
                "{\"role\":\"MO\",\"teacherId\":\"SHARED-110\",\"fullName\":\"Shared MO\"}");
        auth.handle(request);

        assertEquals(201, request.getResponseCode());
        PasswordResetRequest pending = ds.getAllPasswordResets().stream()
                .filter(r -> "MO".equals(r.role) && "SHARED-110".equals(r.studentId))
                .findFirst().orElse(null);
        assertNotNull(pending);

        User admin = ds.getUserByUsername("admin");
        AdminHandler adminHandler = new AdminHandler(ds);
        TestHttpExchange approve = new TestHttpExchange("PUT",
                "/api/admin/password-resets/" + pending.id, null, "{\"action\":\"APPROVE\"}");
        approve.setBearerToken(ds.createSession(admin.id));
        adminHandler.handle(approve);

        assertEquals(200, approve.getResponseCode());
        assertEquals("ta-old", ds.getUserById(ta.id).password);
        assertEquals("123456", ds.getUserById(mo.id).password);
    }

    @Test
    void notificationShouldRejectReadingAnotherUsersItem() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User owner = addUser(ds, "notice_owner", "TA", "SID-NO");
        User other = addUser(ds, "notice_other", "TA", "SID-NX");
        Notification notification = new Notification();
        notification.userId = owner.id;
        notification.title = "Private result";
        notification.content = "Only owner may acknowledge this.";
        notification = ds.addNotification(notification);

        NotificationHandler handler = new NotificationHandler(ds);
        TestHttpExchange read = new TestHttpExchange("PUT",
                "/api/notifications/" + notification.id + "/read", null, null);
        read.setBearerToken(ds.createSession(other.id));
        handler.handle(read);

        assertEquals(403, read.getResponseCode());
        assertFalse(ds.getNotificationById(notification.id).read);
    }

    @Test
    void draftEndpointsShouldBeRestrictedToTaUsers() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addUser(ds, "draft_mo", "MO", "T-DRAFT");

        DraftHandler handler = new DraftHandler(ds);
        TestHttpExchange save = new TestHttpExchange("PUT", "/api/drafts/application", null,
                "{\"jobId\":\"job-private\",\"coverLetter\":\"not allowed\"}");
        save.setBearerToken(ds.createSession(mo.id));
        handler.handle(save);

        assertEquals(403, save.getResponseCode());
        assertNull(ds.getApplicationDraft(mo.id, "job-private"));
    }

    @Test
    void uploadAndDownloadShouldRequireAuthentication() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        UploadHandler handler = new UploadHandler(ds);

        TestHttpExchange upload = new TestHttpExchange("POST", "/api/upload", null,
                "{\"fileName\":\"secret.pdf\",\"data\":\"JVBERg==\"}");
        handler.handle(upload);
        assertEquals(401, upload.getResponseCode());

        String saved = ds.saveUpload("stored.pdf", "%PDF".getBytes());
        TestHttpExchange download = new TestHttpExchange("GET", "/api/upload/" + saved, null, null);
        handler.handle(download);
        assertEquals(401, download.getResponseCode());
    }

    private User addUser(DataService ds, String username, String role, String idNumber) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = role;
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = idNumber;
        return ds.addUser(user);
    }
}
