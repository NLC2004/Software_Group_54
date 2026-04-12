package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.ApplicationHandler;
import com.bupt.tarecruit.handler.NotificationHandler;
import com.bupt.tarecruit.model.Application;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.Notification;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TaApplicationAccessAndNotificationTest {

    @TempDir
    Path tempDir;

    @Test
    void ta07_listApplicationsShouldReturnOnlyCurrentTasOwnApplications() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta1 = addTa(ds, "ta_list_owner");
        User ta2 = addTa(ds, "ta_list_other");
        User mo = addMo(ds, "mo_list");
        Job job1 = addOpenJob(ds, mo, "Algorithms");
        Job job2 = addOpenJob(ds, mo, "Databases");

        Application own = ds.addApplication(buildApplication(ta1.id, job1.id, 1));
        Application other = ds.addApplication(buildApplication(ta2.id, job2.id, 2));
        String token = ds.createSession(ta1.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/applications", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains(own.id));
        assertFalse(body.contains(other.id));
        assertTrue(body.contains("Algorithms"));
    }

    @Test
    void ta08_getApplicationDetailShouldAllowOwnApplication() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_detail_owner");
        User mo = addMo(ds, "mo_detail_owner");
        Job job = addOpenJob(ds, mo, "Operating Systems");
        Application app = ds.addApplication(buildApplication(ta.id, job.id, 1));
        String token = ds.createSession(ta.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/applications/" + app.id, null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains(app.id));
        assertTrue(body.contains("Operating Systems"));
        assertTrue(body.contains(ta.email));
    }

    @Test
    void ta08_getApplicationDetailShouldRejectOtherTasApplication() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User owner = addTa(ds, "ta_detail_real_owner");
        User viewer = addTa(ds, "ta_detail_forbidden");
        User mo = addMo(ds, "mo_detail_forbidden");
        Job job = addOpenJob(ds, mo, "Networks");
        Application app = ds.addApplication(buildApplication(owner.id, job.id, 1));
        String token = ds.createSession(viewer.id);

        ApplicationHandler handler = new ApplicationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("GET", "/api/applications/" + app.id, null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(403, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("Not your application"));
    }

    @Test
    void ta10_notificationsShouldListOnlyUsersNotificationsAndMarkSingleAsRead() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_notif_single");
        User other = addTa(ds, "ta_notif_other");
        String token = ds.createSession(ta.id);

        Notification oldNotif = new Notification();
        oldNotif.userId = ta.id;
        oldNotif.title = "Old Notification";
        oldNotif.content = "old";
        oldNotif.type = "SYSTEM";
        oldNotif = ds.addNotification(oldNotif);
        oldNotif.createdAt = 1000L;
        ds.updateNotification(oldNotif);

        Notification newNotif = new Notification();
        newNotif.userId = ta.id;
        newNotif.title = "New Notification";
        newNotif.content = "new";
        newNotif.type = "APPLICATION";
        newNotif = ds.addNotification(newNotif);
        newNotif.createdAt = 2000L;
        ds.updateNotification(newNotif);

        Notification foreignNotif = new Notification();
        foreignNotif.userId = other.id;
        foreignNotif.title = "Foreign Notification";
        foreignNotif.content = "foreign";
        foreignNotif.type = "SYSTEM";
        ds.addNotification(foreignNotif);

        NotificationHandler handler = new NotificationHandler(ds);
        TestHttpExchange listEx = new TestHttpExchange("GET", "/api/notifications", null, null);
        listEx.setBearerToken(token);
        handler.handle(listEx);

        assertEquals(200, listEx.getResponseCode());
        String listBody = listEx.getResponseBodyAsString();
        assertTrue(listBody.contains("New Notification"));
        assertTrue(listBody.contains("Old Notification"));
        assertFalse(listBody.contains("Foreign Notification"));
        assertTrue(listBody.indexOf("New Notification") < listBody.indexOf("Old Notification"));

        TestHttpExchange markOneEx = new TestHttpExchange("PUT", "/api/notifications/" + newNotif.id + "/read", null, null);
        markOneEx.setBearerToken(token);
        handler.handle(markOneEx);

        assertEquals(200, markOneEx.getResponseCode());
        assertTrue(markOneEx.getResponseBodyAsString().contains("Marked as read"));
        assertTrue(ds.getNotificationById(newNotif.id).read);
        assertFalse(ds.getNotificationById(oldNotif.id).read);
    }

    @Test
    void ta10_notificationsShouldMarkAllAsRead() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addTa(ds, "ta_notif_all");
        String token = ds.createSession(ta.id);

        Notification n1 = new Notification();
        n1.userId = ta.id;
        n1.title = "N1";
        n1.content = "c1";
        n1.type = "SYSTEM";
        ds.addNotification(n1);

        Notification n2 = new Notification();
        n2.userId = ta.id;
        n2.title = "N2";
        n2.content = "c2";
        n2.type = "APPLICATION";
        ds.addNotification(n2);

        NotificationHandler handler = new NotificationHandler(ds);
        TestHttpExchange ex = new TestHttpExchange("PUT", "/api/notifications/read-all", null, null);
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        assertTrue(ex.getResponseBodyAsString().contains("All marked as read"));
        assertTrue(ds.getNotificationsByUser(ta.id).stream().allMatch(notification -> notification.read));
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
        user.studentId = "MO_" + username;
        return ds.addUser(user);
    }

    private Job addOpenJob(DataService ds, User mo, String title) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = title;
        job.type = "TA";
        job.courseName = title + " Course";
        job.description = "desc";
        job.quota = 1;
        job.weeklyHours = 5;
        job.schedule = "Mon";
        job.deadline = "2099-12-31";
        return ds.addJob(job);
    }

    private Application buildApplication(String applicantId, String jobId, int priority) {
        Application app = new Application();
        app.applicantId = applicantId;
        app.jobId = jobId;
        app.priority = priority;
        app.coverLetter = "cover";
        app.cvFileName = "cv.pdf";
        return app;
    }
}
