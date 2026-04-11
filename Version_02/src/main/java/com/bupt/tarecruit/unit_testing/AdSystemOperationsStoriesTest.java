package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AdminHandler;
import com.bupt.tarecruit.model.ExportTask;
import com.bupt.tarecruit.model.PasswordResetRequest;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AdSystemOperationsStoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void ad05_exportShouldCreateExportTaskAndCsvFile() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/export", "dateRange=All%20time", null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertFalse(ds.getAllExportTasks().isEmpty());
        ExportTask task = ds.getAllExportTasks().get(0);
        assertEquals("COMPLETED", task.status);
        assertNotNull(task.fileName);
        assertTrue(Files.exists(ds.getUploadsDir().resolve(task.fileName)));
    }

    @Test
    void ad06_auditLogsShouldSupportSearch() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        ds.addAuditLog(admin.id, admin.username, "USER_CREATE", "Created user: search_target");
        ds.addAuditLog(admin.id, admin.username, "LOGIN", "Another log");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/admin/audit-logs", "search=search_target", null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("search_target"));
        assertFalse(body.contains("Another log"));
    }

    @Test
    void ad07_bulkNotificationShouldSendToTargetRoles() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User ta = addUser(ds, "bulk_ta", "TA");
        User mo = addUser(ds, "bulk_mo", "MO");
        addUser(ds, "bulk_admin2", "ADMIN");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/admin/bulk-notifications",
                null,
                "{\"title\":\"System Notice\",\"message\":\"Please review the new policy\",\"roles\":[\"TA\",\"MO\"]}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertTrue(ds.getNotificationsByUser(ta.id).stream().anyMatch(n -> "System Notice".equals(n.title)));
        assertTrue(ds.getNotificationsByUser(mo.id).stream().anyMatch(n -> "System Notice".equals(n.title)));
    }

    @Test
    void ad08_superAdminShouldUpdateSystemSettings() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/admin/settings",
                null,
                "{\"maxWeeklyHours\":\"18\",\"applicationStartDate\":\"2026-01-01\",\"applicationEndDate\":\"2026-02-01\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertEquals("18", ds.getSettings().get("maxWeeklyHours"));
        assertEquals("2026-01-01", ds.getSettings().get("applicationStartDate"));
    }

    @Test
    void ad10_approvePasswordResetShouldResetPasswordAndNotifyUser() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User ta = addUser(ds, "reset_ta", "TA");
        ta.studentId = "SID_RESET_TA";
        ta.password = "old-pass";
        ds.updateUser(ta);

        PasswordResetRequest req = new PasswordResetRequest();
        req.studentId = ta.studentId;
        req.fullName = ta.fullName;
        req.email = ta.email;
        req = ds.addPasswordReset(req);
        String token = ds.createSession(admin.id);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/admin/password-resets/" + req.id,
                null,
                "{\"action\":\"APPROVE\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertEquals("APPROVED", ds.getPasswordResetById(req.id).status);
        assertEquals("123456", ds.getUserById(ta.id).password);
        assertTrue(ds.getNotificationsByUser(ta.id).stream().anyMatch(n -> "Password Reset Approved".equals(n.title)));
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
