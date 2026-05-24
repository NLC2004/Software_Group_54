package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AdminHandler;
import com.bupt.tarecruit.model.PasswordResetRequest;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers administrator authorization and configuration boundary cases that
 * could otherwise permit unintended privileged actions.
 */
class AdminBoundaryCoverageTest {

    @TempDir
    Path tempDir;

    /**
     * Calls an administrator statistics endpoint as a TA and confirms that
     * non-administrators cannot view system-level recruitment information.
     */
    @Test
    void nonAdminShouldBeDeniedAdminStatistics() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addUser(ds, "not_admin", "TA");
        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange stats = new TestHttpExchange("GET", "/api/admin/stats", null, null);
        stats.setBearerToken(ds.createSession(ta.id));

        handler.handle(stats);

        assertEquals(403, stats.getResponseCode());
        assertTrue(stats.getResponseBodyAsString().contains("Admin only"));
    }

    /**
     * Demonstrates standard-admin read visibility while confirming destructive
     * user deletion remains reserved for the super administrator.
     */
    @Test
    void standardAdminShouldReadUsersButNotDeleteAccounts() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User standard = addUser(ds, "standard_reader", "ADMIN");
        User target = addUser(ds, "protected_account", "TA");
        String token = ds.createSession(standard.id);
        AdminHandler handler = new AdminHandler(ds);

        TestHttpExchange list = new TestHttpExchange("GET", "/api/admin/users", "search=protected_account", null);
        list.setBearerToken(token);
        handler.handle(list);
        assertEquals(200, list.getResponseCode());
        assertTrue(list.getResponseBodyAsString().contains("protected_account"));

        TestHttpExchange delete = new TestHttpExchange("DELETE", "/api/admin/users/" + target.id, null, null);
        delete.setBearerToken(token);
        handler.handle(delete);
        assertEquals(403, delete.getResponseCode());
        assertNotNull(ds.getUserById(target.id));
    }

    /**
     * Attempts to assign an unknown role-template identifier when creating an
     * administrator and confirms invalid privilege metadata is rejected.
     */
    @Test
    void superAdminShouldRejectUnknownAdminRoleTemplateAssignment() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User admin = ds.getUserByUsername("admin");
        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange create = new TestHttpExchange("POST", "/api/admin/users", null,
                "{\"username\":\"template_admin\",\"password\":\"pass123\",\"role\":\"ADMIN\","
                        + "\"fullName\":\"Template Admin\",\"email\":\"template@example.com\","
                        + "\"adminRoleTemplateId\":\"missing-template\"}");
        create.setBearerToken(ds.createSession(admin.id));

        handler.handle(create);

        assertEquals(400, create.getResponseCode());
        assertNull(ds.getUserByUsername("template_admin"));
    }

    /**
     * Configures an active reset template with a custom initial password and
     * verifies approved resets honor that configured value.
     */
    @Test
    void activePasswordResetTemplateShouldControlApprovedInitialPassword() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        Map<String, String> settings = new HashMap<>(ds.getSettings());
        settings.put("emailTemplatesJson", "[{\"active\":true,\"defaultPassword\":\"Reset#2026\"}]");
        ds.updateSettings(settings);
        User ta = addUser(ds, "template_reset_ta", "TA");
        ta.studentId = "SID-TEMPLATE-RESET";
        ta.email = "";
        ta.password = "old-password";
        ds.updateUser(ta);
        PasswordResetRequest request = new PasswordResetRequest();
        request.studentId = ta.studentId;
        request.role = "TA";
        request.fullName = ta.fullName;
        request = ds.addPasswordReset(request);

        User admin = ds.getUserByUsername("admin");
        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange approve = new TestHttpExchange("PUT",
                "/api/admin/password-resets/" + request.id, null, "{\"action\":\"APPROVE\"}");
        approve.setBearerToken(ds.createSession(admin.id));
        handler.handle(approve);

        assertEquals(200, approve.getResponseCode());
        assertEquals("Reset#2026", ds.getUserById(ta.id).password);
    }

    /**
     * Broadcasts to TAs containing active and inactive accounts and verifies
     * only active recipients receive notices while an evidence file is saved.
     */
    @Test
    void bulkNotificationShouldSkipInactiveRecipientsAndCreateEvidenceFile() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User active = addUser(ds, "announce_active", "TA");
        User inactive = addUser(ds, "announce_inactive", "TA");
        inactive.active = false;
        ds.updateUser(inactive);

        User admin = ds.getUserByUsername("admin");
        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange send = new TestHttpExchange("POST", "/api/admin/bulk-notifications", null,
                "{\"title\":\"Deadline Update\",\"message\":\"Review the new date\",\"roles\":[\"TA\"]}");
        send.setBearerToken(ds.createSession(admin.id));
        handler.handle(send);

        assertEquals(200, send.getResponseCode());
        assertTrue(ds.getNotificationsByUser(active.id).stream().anyMatch(n -> "Deadline Update".equals(n.title)));
        assertFalse(ds.getNotificationsByUser(inactive.id).stream().anyMatch(n -> "Deadline Update".equals(n.title)));
        assertTrue(send.getResponseBodyAsString().contains("\"fileName\""));
        try (Stream<Path> files = Files.list(ds.getUploadsDir())) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().contains("bulk_notification")));
        }
    }

    /**
     * Confirms that a standard administrator cannot approve a TA recovery
     * request or alter the applicant's stored password.
     */
    @Test
    void standardAdminShouldNotProcessPasswordResetRequest() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User standard = addUser(ds, "standard_reset_admin", "ADMIN");
        User ta = addUser(ds, "reset_protected_ta", "TA");
        PasswordResetRequest request = new PasswordResetRequest();
        request.studentId = ta.studentId;
        request.role = "TA";
        request.fullName = ta.fullName;
        request = ds.addPasswordReset(request);

        AdminHandler handler = new AdminHandler(ds);
        TestHttpExchange approve = new TestHttpExchange("PUT",
                "/api/admin/password-resets/" + request.id, null, "{\"action\":\"APPROVE\"}");
        approve.setBearerToken(ds.createSession(standard.id));
        handler.handle(approve);

        assertEquals(403, approve.getResponseCode());
        assertEquals("PENDING", ds.getPasswordResetById(request.id).status);
        assertEquals("pass123", ds.getUserById(ta.id).password);
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
