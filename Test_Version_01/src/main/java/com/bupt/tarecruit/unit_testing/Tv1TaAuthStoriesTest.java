package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AuthHandler;
import com.bupt.tarecruit.model.PasswordResetRequest;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Tv1TaAuthStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void ta02_loginShouldAllowActiveTa() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = createTa(ds, "tv1_login_ta", "pass123");
        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/login",
                null,
                "{\"username\":\"tv1_login_ta\",\"password\":\"pass123\"}"
        );

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("\"token\""));
        assertTrue(ex.getResponseBodyAsString().contains("\"role\":\"TA\""));
        assertEquals(ta.id, ds.getSessionUser(extractToken(ex.getResponseBodyAsString())).id);
    }

    @Test
    void ta02_loginShouldRejectDeactivatedAccount() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = createTa(ds, "tv1_login_disabled", "pass123");
        ta.active = false;
        ds.updateUser(ta);

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/login",
                null,
                "{\"username\":\"tv1_login_disabled\",\"password\":\"pass123\"}"
        );

        handler.handle(ex);

        assertEquals(403, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("deactivated"));
    }

    @Test
    void ta04_changePasswordShouldUpdateStoredPassword() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = createTa(ds, "tv1_change_pwd", "old-pass");
        String token = ds.createSession(ta.id);

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
        assertEquals("new-pass-123", ds.getUserById(ta.id).password);
    }

    @Test
    void ta05_forgotPasswordShouldCreatePendingRequest() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = createTa(ds, "tv1_forgot_ta", "pass123");

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/forgot-password",
                null,
                "{\"username\":\"tv1_forgot_ta\",\"fullName\":\"Forgot TA\",\"email\":\"forgot@example.com\",\"phone\":\"18800001111\",\"reason\":\"Forgot password\"}"
        );

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Reset request submitted"));
        assertEquals(1, ds.getAllPasswordResetRequests().size());
        PasswordResetRequest req = ds.getAllPasswordResetRequests().get(0);
        assertEquals(ta.id, req.userId);
        assertEquals("PENDING", req.status);
    }

    @Test
    void ta06_registerShouldCreateTaAccountAndReturnToken() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/register",
                null,
                "{\"username\":\"tv1_new_ta\",\"password\":\"pass123\",\"role\":\"TA\",\"studentId\":\"BUPT-001\",\"fullName\":\"New TA\",\"email\":\"tv1_new_ta@example.com\",\"phone\":\"18800000000\",\"gender\":\"F\"}"
        );

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("\"token\""));
        User stored = ds.getUserByUsername("tv1_new_ta");
        assertNotNull(stored);
        assertEquals("TA", stored.role);
        assertEquals("BUPT-001", stored.studentId);
    }

    private User createTa(DataService ds, String username, String password) {
        User ta = new User();
        ta.username = username;
        ta.password = password;
        ta.role = "TA";
        ta.fullName = username + " Full";
        ta.email = username + "@example.com";
        ta.studentId = "SID_" + username;
        return ds.addUser(ta);
    }

    private String extractToken(String body) {
        int keyIndex = body.indexOf("\"token\":\"");
        int start = keyIndex + 9;
        int end = body.indexOf('"', start);
        return body.substring(start, end);
    }
}
