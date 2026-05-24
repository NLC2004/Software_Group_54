package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AuthHandler;
import com.bupt.tarecruit.model.PasswordResetRequest;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies Module Organiser account operations exposed through the organiser
 * portal, including registration, login and account-recovery credentials.
 */
class MoAuthStoriesTest {

    @TempDir
    Path tempDir;

    /**
     * Registers an MO identity and confirms that the stored user is assigned
     * the organiser role expected by job-management endpoints.
     */
    @Test
    void mo03_registerShouldCreateMoAccount() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/register",
                null,
                "{\"username\":\"teacher_new\",\"password\":\"pass123\",\"role\":\"MO\",\"fullName\":\"Teacher New\",\"email\":\"teacher_new@example.com\",\"studentId\":\"teacher_new\"}"
        );

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("\"role\":\"MO\""));
        User stored = ds.getUserByUsername("teacher_new");
        assertNotNull(stored);
        assertEquals("MO", stored.role);
    }

    /**
     * Logs in with valid organiser credentials and confirms successful access
     * through the role-specific MO portal.
     */
    @Test
    void mo03_loginShouldAllowMoPortalAccess() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        createMo(ds, "teacher_login", "pass123");
        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/login",
                null,
                "{\"identifier\":\"teacher_login@example.com\",\"password\":\"pass123\",\"portalRole\":\"MO\"}"
        );

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("\"token\""));
        assertTrue(ex.getResponseBodyAsString().contains("\"role\":\"MO\""));
    }

    /**
     * Changes an MO password with a valid current password and verifies that
     * the new credential is persisted.
     */
    @Test
    void mo08_changePasswordShouldUpdateStoredPassword() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = createMo(ds, "teacher_pwd", "old-pass");
        String token = ds.createSession(mo.id);

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/auth/password",
                null,
                "{\"oldPassword\":\"old-pass\",\"newPassword\":\"new-pass-123\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Password updated"));
        assertEquals("new-pass-123", ds.getUserById(mo.id).password);
    }

    /**
     * Attempts an MO password change with an invalid current password and
     * confirms the existing credential remains unchanged.
     */
    @Test
    void mo08_changePasswordShouldRejectWrongOldPassword() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = createMo(ds, "teacher_pwd_fail", "old-pass");
        String token = ds.createSession(mo.id);

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/auth/password",
                null,
                "{\"oldPassword\":\"wrong-old\",\"newPassword\":\"new-pass-123\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(400, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Current password is incorrect"));
        assertEquals("old-pass", ds.getUserById(mo.id).password);
    }

    /**
     * Submits an MO recovery request and verifies that it enters the reset
     * approval workflow for administrator processing.
     */
    @Test
    void mo09_forgotPasswordShouldCreateResetRequest() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/password-reset",
                null,
                "{\"studentId\":\"teacher_reset\",\"fullName\":\"Teacher Reset\",\"email\":\"teacher_reset@example.com\",\"phone\":\"18800002222\",\"notes\":\"Need urgent recovery\"}"
        );

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Password reset request submitted"));
        assertEquals(1, ds.getAllPasswordResets().size());
        PasswordResetRequest req = ds.getAllPasswordResets().get(0);
        assertEquals("teacher_reset", req.studentId);
        assertEquals("PENDING", req.status);
    }

    private User createMo(DataService ds, String username, String password) {
        User mo = new User();
        mo.username = username;
        mo.password = password;
        mo.role = "MO";
        mo.fullName = username + " Full";
        mo.email = username + "@example.com";
        mo.studentId = username;
        return ds.addUser(mo);
    }
}
