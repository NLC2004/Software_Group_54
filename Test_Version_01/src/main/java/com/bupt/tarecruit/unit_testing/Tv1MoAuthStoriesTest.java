package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AuthHandler;
import com.bupt.tarecruit.model.PasswordResetRequest;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Tv1MoAuthStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void mo03_registerShouldCreateMoAccount() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/register",
                null,
                "{\"username\":\"tv1_new_mo\",\"password\":\"pass123\",\"role\":\"MO\",\"studentId\":\"MO-001\",\"fullName\":\"New MO\",\"email\":\"tv1_new_mo@example.com\"}"
        );

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("\"role\":\"MO\""));
        User stored = ds.getUserByUsername("tv1_new_mo");
        assertNotNull(stored);
        assertEquals("MO", stored.role);
    }

    @Test
    void mo03_loginShouldAllowMoAccount() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        createMo(ds, "tv1_login_mo", "pass123");

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/login",
                null,
                "{\"username\":\"tv1_login_mo\",\"password\":\"pass123\"}"
        );

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("\"token\""));
        assertTrue(ex.getResponseBodyAsString().contains("\"role\":\"MO\""));
    }

    @Test
    void mo08_changePasswordShouldUpdateStoredPassword() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = createMo(ds, "tv1_change_mo", "old-pass");
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
        assertEquals("new-pass-123", ds.getUserById(mo.id).password);
    }

    @Test
    void mo09_forgotPasswordShouldCreatePendingRequest() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = createMo(ds, "tv1_forgot_mo", "pass123");

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/forgot-password",
                null,
                "{\"username\":\"tv1_forgot_mo\",\"fullName\":\"Forgot MO\",\"email\":\"forgot.mo@example.com\",\"phone\":\"18800003333\",\"reason\":\"Forgot password\"}"
        );

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        assertEquals(1, ds.getAllPasswordResetRequests().size());
        PasswordResetRequest req = ds.getAllPasswordResetRequests().get(0);
        assertEquals(mo.id, req.userId);
        assertEquals("MO", req.role);
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
