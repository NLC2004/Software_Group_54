package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AuthHandler;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the Teaching Assistant authentication stories and the privacy rules
 * applied when credentials are submitted through role-specific login portals.
 * Each test uses isolated storage so account changes do not leak between cases.
 */
class TaAuthStoriesTest {

    @TempDir
    Path tempDir;

    /**
     * Confirms that a TA can authenticate through the TA portal using a valid
     * student identifier and receives a session-bearing TA response.
     */
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

    /**
     * Confirms that a shared identifier can represent both a TA and an MO
     * while the selected portal resolves only the intended account.
     */
    @Test
    void ta02_loginShouldAllowTaWhenMoSharesSameStudentId() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = new User();
        ta.username = "shared_id_ta";
        ta.password = "ta-pass";
        ta.role = "TA";
        ta.fullName = "Shared TA";
        ta.email = "shared_ta@example.com";
        ta.studentId = "2026110123";
        ds.addUser(ta);

        User mo = new User();
        mo.username = "shared_id_mo";
        mo.password = "mo-pass";
        mo.role = "MO";
        mo.fullName = "Shared MO";
        mo.email = "shared_mo@example.com";
        mo.studentId = "2026110123";
        ds.addUser(mo);

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange taLogin = new TestHttpExchange(
                "POST",
                "/api/auth/login",
                null,
                "{\"identifier\":\"2026110123\",\"password\":\"ta-pass\",\"portalRole\":\"TA\"}"
        );
        handler.handle(taLogin);
        assertEquals(200, taLogin.getResponseCode());
        assertTrue(taLogin.getResponseBodyAsString().contains("\"role\":\"TA\""));

        TestHttpExchange moLogin = new TestHttpExchange(
                "POST",
                "/api/auth/login",
                null,
                "{\"identifier\":\"2026110123\",\"password\":\"mo-pass\",\"portalRole\":\"MO\"}"
        );
        handler.handle(moLogin);
        assertEquals(200, moLogin.getResponseCode());
        assertTrue(moLogin.getResponseBodyAsString().contains("\"role\":\"MO\""));
    }

    /**
     * Ensures a wrong-portal login returns a generic failure, rather than
     * revealing that the identifier belongs to a valid account of another role.
     */
    @Test
    void ta02_loginShouldNotRevealAccountExistsOnAnotherPortal() throws Exception {
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

        assertEquals(401, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Invalid teacher ID, email, or password"));
    }

    /**
     * Exercises the successful password-change path and verifies that the new
     * password replaces the current value in persisted user data.
     */
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

    /**
     * Protects account ownership by rejecting password changes made with an
     * incorrect current password and preserving the original credential.
     */
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

    /**
     * Confirms that complete TA registration stores the correct role and
     * student identity and immediately returns an authenticated token.
     */
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

    /**
     * Enforces account-name uniqueness by refusing to register a second TA
     * under an existing username.
     */
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
