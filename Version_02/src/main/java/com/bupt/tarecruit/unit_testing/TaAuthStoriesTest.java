package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AuthHandler;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TaAuthStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void ta02_loginShouldAllowTaUsingStudentId() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = new User();
        ta.username = "ta_login";
        ta.password = "pass123";
        ta.role = "TA";
        ta.fullName = "Login TA";
        ta.email = "ta_login@example.com";
        ta.studentId = "BUPT-LOGIN-001";
        ds.addUser(ta);

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/login",
                null,
                "{\"identifier\":\"BUPT-LOGIN-001\",\"password\":\"pass123\",\"portalRole\":\"TA\"}"
        );

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("\"token\""));
        assertTrue(ex.getResponseBodyAsString().contains("\"role\":\"TA\""));
    }

    @Test
    void ta02_loginShouldRejectWrongPortalRole() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = new User();
        ta.username = "ta_portal_role";
        ta.password = "pass123";
        ta.role = "TA";
        ta.fullName = "Portal TA";
        ta.email = "ta_portal_role@example.com";
        ta.studentId = "BUPT-LOGIN-002";
        ds.addUser(ta);

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/login",
                null,
                "{\"identifier\":\"BUPT-LOGIN-002\",\"password\":\"pass123\",\"portalRole\":\"MO\"}"
        );

        handler.handle(ex);

        assertEquals(403, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("This portal is for MO accounts only"));
    }

    @Test
    void ta04_changePasswordShouldUpdateStoredPassword() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = createTa(ds, "ta_change_pwd", "old-pass");
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
        assertTrue(ex.getResponseBodyAsString().contains("Password updated"));
        assertEquals("new-pass-123", ds.getUserById(ta.id).password);
    }

    @Test
    void ta04_changePasswordShouldRejectWrongOldPassword() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = createTa(ds, "ta_change_pwd_fail", "old-pass");
        String token = ds.createSession(ta.id);

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
        assertEquals("old-pass", ds.getUserById(ta.id).password);
    }

    @Test
    void ta06_registerShouldCreateTaAccountAndReturnToken() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/register",
                null,
                "{\"username\":\"new_ta\",\"password\":\"pass123\",\"role\":\"TA\",\"fullName\":\"New TA\",\"email\":\"new_ta@example.com\",\"studentId\":\"BUPT-REG-001\"}"
        );

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("\"token\""));
        User stored = ds.getUserByUsername("new_ta");
        assertNotNull(stored);
        assertEquals("TA", stored.role);
        assertEquals("BUPT-REG-001", stored.studentId);
    }

    @Test
    void ta06_registerShouldRejectDuplicateUsername() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        createTa(ds, "duplicate_ta", "pass123");

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/register",
                null,
                "{\"username\":\"duplicate_ta\",\"password\":\"pass123\",\"role\":\"TA\",\"fullName\":\"Dup TA\",\"email\":\"dup@example.com\",\"studentId\":\"BUPT-REG-002\"}"
        );

        handler.handle(ex);

        assertEquals(409, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Username already exists"));
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
}
