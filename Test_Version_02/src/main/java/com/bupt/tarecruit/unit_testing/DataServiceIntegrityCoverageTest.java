package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.model.Application;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataServiceIntegrityCoverageTest {

    @TempDir
    Path tempDir;

    @Test
    void notificationReconciliationShouldBackfillApplicationEventsOnlyOnce() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User mo = addUser(ds, "reconcile_mo", "MO");
        User ta = addUser(ds, "reconcile_ta", "TA");
        Job job = addJob(ds, mo, "Reconciliation Course");
        Application application = new Application();
        application.jobId = job.id;
        application.applicantId = ta.id;
        application.priority = 1;
        application = ds.addApplication(application);
        application.status = "APPROVED";
        ds.updateApplication(application);

        ds.reconcileNotifications();
        ds.reconcileNotifications();

        assertEquals(1, ds.getNotificationsByUser(mo.id).stream()
                .filter(n -> "New Application".equals(n.title) && n.content.contains(job.title)).count());
        assertEquals(1, ds.getNotificationsByUser(ta.id).stream()
                .filter(n -> "Application Approved".equals(n.title) && n.content.contains(job.title)).count());
    }

    @Test
    void structuredScheduleShouldCalculateHoursPerWeekFromSelectedPeriods() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        Job job = new Job();
        job.type = "COURSE_TA";
        job.courseScheduleGrid = "[{\"week\":2,\"selection\":{\"Mon\":[1,2],\"Tue\":[3]}},"
                + "{\"week\":3,\"selection\":{\"Fri\":[4]}}]";

        Map<Integer, Double> hours = ds.getJobWeeklyHours(job);

        assertEquals(2.25, hours.get(2), 0.001);
        assertEquals(0.75, hours.get(3), 0.001);
    }

    @Test
    void finalExamWorkloadShouldUseExamDurationAsSingleWorkloadBucket() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        Job job = new Job();
        job.type = "FINAL_EXAM_TA";
        job.examDuration = 2.5;

        Map<Integer, Double> hours = ds.getJobWeeklyHours(job);

        assertEquals(1, hours.size());
        assertEquals(2.5, hours.get(0), 0.001);
    }

    @Test
    void invalidWorkloadSettingShouldFallBackToDefaultLimit() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        Map<String, String> settings = new HashMap<>(ds.getSettings());
        settings.put("maxWeeklyHours", "not-a-number");
        ds.updateSettings(settings);

        assertEquals(20.0, ds.getWeeklyWorkloadLimit(), 0.001);
    }

    private User addUser(DataService ds, String username, String role) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = role;
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = "ID_" + username;
        return ds.addUser(user);
    }

    private Job addJob(DataService ds, User mo, String title) {
        Job job = new Job();
        job.postedBy = mo.id;
        job.title = title;
        job.type = "COURSE_TA";
        job.courseName = title;
        job.quota = 2;
        job.weeklyHours = 1;
        job.deadline = "2099-12-31";
        return ds.addJob(job);
    }
}
