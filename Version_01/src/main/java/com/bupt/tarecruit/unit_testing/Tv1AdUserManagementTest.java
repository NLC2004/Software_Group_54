package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AdminHandler;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Tv1AdUserManagementTest {

    @TempDir
    Path tempDir;

    @Test
    void ad02_userListShouldSupportSearch() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        addUser(ds, "search_ta", "TA");
        addUser(ds, "another_mo", "MO");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/users", "search=search_ta", null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("search_ta"));
        assertFalse(body.contains("another_mo"));
    }

    @Test
    void ad09_updateUserShouldChangeRoleAndActiveStatus() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User ta = addUser(ds, "role_target", "TA");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/admin/users/" + ta.id,
                null,
                "{\"role\":\"MO\",\"active\":false}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        User updated = ds.getUserById(ta.id);
        assertEquals("MO", updated.role);
        assertFalse(updated.active);
    }

    @Test
    void ad13_deleteUserShouldRemoveAccount() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User ta = addUser(ds, "delete_target", "TA");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("DELETE", "/api/admin/users/" + ta.id, null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertNull(ds.getUserById(ta.id));
    }

    @Test
    void ad13_updateUserPasswordShouldPersistAndCreateAuditLog() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addUser(ds, "password_target", "MO");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/admin/users/" + mo.id,
                null,
                "{\"password\":\"reset-123456\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertEquals("reset-123456", ds.getUserById(mo.id).password);
        assertTrue(ds.getAllAuditLogs().stream().anyMatch(log -> "RESET_PASSWORD".equals(log.action) && mo.id.equals(log.targetId)));
    }

    private User addUser(DataService ds, String username, String role) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = role;
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = "SID_" + username;
        return ds.addUser(user);
    }
}
