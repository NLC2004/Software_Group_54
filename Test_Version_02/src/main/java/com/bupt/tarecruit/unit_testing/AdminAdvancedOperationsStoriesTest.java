package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AdminHandler;
import com.bupt.tarecruit.model.AdminRoleTemplate;
import com.bupt.tarecruit.model.ExportTask;
import com.bupt.tarecruit.model.PasswordResetRequest;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises advanced administrative functions that extend the original
 * dashboard stories: templates, asynchronous file tasks and reset escalation.
 */
class AdminAdvancedOperationsStoriesTest {

    @TempDir
    Path tempDir;

    /**
     * Runs the full administrator-role-template lifecycle and confirms that
     * the detail response reports how many admin accounts use the template.
     */
    @Test
    void adminRoleTemplatesShouldSupportCreateListDetailUpdateDeleteAndAssignedCount() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);

        TestHttpExchange create = new TestHttpExchange("POST", "/api/admin/role-templates", null,
                "{\"name\":\"Recruitment Auditor\",\"description\":\"Can audit recruitment data\",\"tags\":[\"audit\",\"readonly\"]}");
        create.setBearerToken(token);
        handler.handle(create);

        assertEquals(201, create.getResponseCode());
        AdminRoleTemplate template = ds.getAllAdminRoleTemplates().stream()
                .filter(t -> "Recruitment Auditor".equals(t.name))
                .findFirst()
                .orElse(null);
        assertNotNull(template);

        User assigned = addUser(ds, "assigned_admin", "ADMIN");
        assigned.adminRoleTemplateId = template.id;
        ds.updateUser(assigned);

        TestHttpExchange detail = new TestHttpExchange("GET", "/api/admin/role-templates/" + template.id, null, null);
        detail.setBearerToken(token);
        handler.handle(detail);

        assertEquals(200, detail.getResponseCode());
        assertTrue(detail.getResponseBodyAsString().contains("\"assignedCount\":1"));

        TestHttpExchange update = new TestHttpExchange("PUT", "/api/admin/role-templates/" + template.id, null,
                "{\"name\":\"Senior Recruitment Auditor\",\"tags\":[\"audit\",\"senior\"]}");
        update.setBearerToken(token);
        handler.handle(update);

        assertEquals(200, update.getResponseCode());
        assertEquals("Senior Recruitment Auditor", ds.getAdminRoleTemplateById(template.id).name);

        TestHttpExchange list = new TestHttpExchange("GET", "/api/admin/role-templates", null, null);
        list.setBearerToken(token);
        handler.handle(list);

        assertEquals(200, list.getResponseCode());
        assertTrue(list.getResponseBodyAsString().contains("Senior Recruitment Auditor"));

        TestHttpExchange delete = new TestHttpExchange("DELETE", "/api/admin/role-templates/" + template.id, null, null);
        delete.setBearerToken(token);
        handler.handle(delete);

        assertEquals(200, delete.getResponseCode());
        assertNull(ds.getAdminRoleTemplateById(template.id));
    }

    /**
     * Creates, lists and downloads a backup ZIP, then simulates a failed task
     * and verifies that retry returns it to a completed state.
     */
    @Test
    void backupTasksShouldCreateListDownloadAndRetryBackupZip() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);

        TestHttpExchange create = new TestHttpExchange("POST", "/api/admin/backup-tasks", null, null);
        create.setBearerToken(token);
        handler.handle(create);

        assertEquals(201, create.getResponseCode());
        ExportTask backup = ds.getAllExportTasks().stream()
                .filter(t -> "BACKUP".equals(t.taskType))
                .findFirst()
                .orElse(null);
        assertNotNull(backup);
        assertEquals("COMPLETED", backup.status);
        assertTrue(Files.exists(ds.getUploadsDir().resolve(backup.fileName)));

        TestHttpExchange list = new TestHttpExchange("GET", "/api/admin/backup-tasks", "status=COMPLETED&page=1&pageSize=5", null);
        list.setBearerToken(token);
        handler.handle(list);

        assertEquals(200, list.getResponseCode());
        assertTrue(list.getResponseBodyAsString().contains("\"total\":1"));

        TestHttpExchange download = new TestHttpExchange("GET", "/api/admin/backup-tasks/" + backup.id + "/download", null, null);
        download.setBearerToken(token);
        handler.handle(download);

        assertEquals(200, download.getResponseCode());
        assertEquals("application/zip", download.getResponseHeaders().getFirst("Content-Type"));
        assertTrue(download.getRecordedResponseLength() > 0);

        backup.status = "FAILED";
        backup.fileName = "";
        ds.updateExportTask(backup);
        TestHttpExchange retry = new TestHttpExchange("POST", "/api/admin/backup-tasks/" + backup.id + "/retry", null, null);
        retry.setBearerToken(token);
        handler.handle(retry);

        assertEquals(200, retry.getResponseCode());
        assertEquals("COMPLETED", ds.getExportTaskById(backup.id).status);
    }

    /**
     * Starts with a failed export task and verifies filtering, retry generation
     * and download of the newly completed CSV evidence file.
     */
    @Test
    void exportTasksShouldListDownloadAndRetryFailedExports() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);

        ExportTask failed = new ExportTask();
        failed.dataSubject = "Retry Export";
        failed.taskType = "EXPORT";
        failed.format = "CSV";
        failed.status = "FAILED";
        failed.generatorId = admin.id;
        failed.generatorName = admin.username;
        failed = ds.addExportTask(failed);

        TestHttpExchange list = new TestHttpExchange("GET", "/api/admin/export-tasks", "status=FAILED&search=Retry&page=1&pageSize=5", null);
        list.setBearerToken(token);
        handler.handle(list);

        assertEquals(200, list.getResponseCode());
        assertTrue(list.getResponseBodyAsString().contains("\"total\":1"));

        TestHttpExchange retry = new TestHttpExchange("POST", "/api/admin/export-tasks/" + failed.id + "/retry", null, null);
        retry.setBearerToken(token);
        handler.handle(retry);

        assertEquals(200, retry.getResponseCode());
        ExportTask completed = ds.getExportTaskById(failed.id);
        assertEquals("COMPLETED", completed.status);
        assertTrue(Files.exists(ds.getUploadsDir().resolve(completed.fileName)));

        TestHttpExchange download = new TestHttpExchange("GET", "/api/admin/export-tasks/" + failed.id + "/download", null, null);
        download.setBearerToken(token);
        handler.handle(download);

        assertEquals(200, download.getResponseCode());
        assertTrue(download.getResponseHeaders().getFirst("Content-Type").contains("text/csv"));
        assertTrue(download.getResponseBodyAsString().contains("=== USERS ==="));
    }

    /**
     * Rejects one recovery request with a reason, then escalates another,
     * verifying persistence of the reason and notification to super admin.
     */
    @Test
    void passwordResetRejectAndEscalateShouldPersistReasonAndNotifySuperAdmin() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        User ta = addUser(ds, "reject_reset_ta", "TA");
        PasswordResetRequest req = new PasswordResetRequest();
        req.studentId = ta.studentId;
        req.fullName = ta.fullName;
        req.email = ta.email;
        req = ds.addPasswordReset(req);
        String token = ds.createSession(admin.id);
        AdminHandler handler = new AdminHandler(ds);

        TestHttpExchange reject = new TestHttpExchange("PUT", "/api/admin/password-resets/" + req.id, null,
                "{\"action\":\"REJECT\",\"reason\":\"Identity mismatch\"}");
        reject.setBearerToken(token);
        handler.handle(reject);

        assertEquals(200, reject.getResponseCode());
        assertEquals("REJECTED", ds.getPasswordResetById(req.id).status);
        assertEquals("Identity mismatch", ds.getPasswordResetById(req.id).reason);
        assertTrue(ds.getNotificationsByUser(ta.id).stream().anyMatch(n -> n.title.contains("Rejected")));

        PasswordResetRequest pending = new PasswordResetRequest();
        pending.studentId = ta.studentId;
        pending.fullName = ta.fullName;
        pending.email = ta.email;
        pending = ds.addPasswordReset(pending);

        TestHttpExchange escalate = new TestHttpExchange("POST", "/api/admin/password-resets/" + pending.id + "/escalate", null, null);
        escalate.setBearerToken(token);
        handler.handle(escalate);

        assertEquals(200, escalate.getResponseCode());
        assertTrue(ds.getNotificationsByUser(admin.id).stream().anyMatch(n -> n.title.contains("Escalation")));
    }

    /**
     * Confirms an ordinary administrator cannot alter system settings or send
     * bulk notifications reserved for the named super-administrator account.
     */
    @Test
    void standardAdminShouldBeBlockedFromSuperAdminOperations() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User standardAdmin = addUser(ds, "standard_admin", "ADMIN");
        String token = ds.createSession(standardAdmin.id);
        AdminHandler handler = new AdminHandler(ds);

        TestHttpExchange settings = new TestHttpExchange("PUT", "/api/admin/settings", null, "{\"maxWeeklyHours\":\"10\"}");
        settings.setBearerToken(token);
        handler.handle(settings);

        assertEquals(403, settings.getResponseCode());

        TestHttpExchange bulk = new TestHttpExchange("POST", "/api/admin/bulk-notifications", null,
                "{\"title\":\"Notice\",\"message\":\"Message\"}");
        bulk.setBearerToken(token);
        handler.handle(bulk);

        assertEquals(403, bulk.getResponseCode());
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
