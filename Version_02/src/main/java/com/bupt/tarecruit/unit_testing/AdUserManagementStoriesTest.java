package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AdminHandler;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AdUserManagementStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void ad02_superAdminShouldCreateUser() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/admin/users",
                null,
                "{\"username\":\"new_mo_user\",\"password\":\"pass123\",\"role\":\"MO\",\"fullName\":\"New MO\",\"email\":\"new_mo@example.com\",\"studentId\":\"MO-NEW-001\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(201, ex.getResponseCode());
        User created = ds.getUserByUsername("new_mo_user");
        assertNotNull(created);
        assertEquals("MO", created.role);
    }

    @Test
    void ad02_nonSuperAdminShouldNotCreateUser() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin2 = addAdmin(ds, "admin2");
        String token = ds.createSession(admin2.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/admin/users",
                null,
                "{\"username\":\"blocked_user\",\"password\":\"pass123\",\"role\":\"TA\",\"fullName\":\"Blocked\",\"email\":\"blocked@example.com\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(403, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Only super admin"));
        assertNull(ds.getUserByUsername("blocked_user"));
    }

    @Test
    void ad09_superAdminShouldUpdateRoleAndActiveStatus() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User ta = addTa(ds, "role_update_ta");
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
    void ad11_userListShouldSupportSearchAndRoleFilter() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        addTa(ds, "alice_ta");
        addMo(ds, "bob_mo");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/users", "search=alice&role=TA", null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("alice_ta"));
        assertFalse(body.contains("bob_mo"));
    }

    @Test
    void ad13_shouldViewIndividualUserDetails() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User mo = addMo(ds, "detail_mo");
        mo.fullName = "Detail User";
        mo.email = "detail@example.com";
        ds.updateUser(mo);
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/users/" + mo.id, null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("detail_mo"));
        assertTrue(body.contains("Detail User"));
        assertTrue(body.contains("detail@example.com"));
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

    private User addAdmin(DataService ds, String username) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = "ADMIN";
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = username;
        return ds.addUser(user);
    }
}
