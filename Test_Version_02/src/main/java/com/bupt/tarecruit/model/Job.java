package com.bupt.tarecruit.model;

import java.util.ArrayList;
import java.util.List;

public class Job {
    public String id;
    public String postedBy;
    public String title;
    public String type; // COURSE_TA, LAB_TA, FINAL_EXAM_TA, CLASS_TEST_TA (legacy: COURSE, ACTIVITY)
    public String courseName;
    public String description;
    public List<String> requirements = new ArrayList<>();
    public int quota;
    public String schedule;
    public double weeklyHours;
    public String deadline;

    // Course TA specific
    public String courseScheduleGrid; // JSON string: {"Mon":[1,2],"Tue":[3]...}
    public int courseWeekStart;
    public int courseWeekEnd;

    // Lab TA specific
    public int labSessionCount;
    public String labTime;
    public String labLocation;
    public String labSessions; // JSON string for per-session week/location/time selections

    // Final Exam TA specific
    public String examDateTime; // ISO-like string from datetime-local
    public double examDuration; // hours
    public String examLocation;

    // Class Test TA specific
    public String testScheduleType; // IN_CLASS, AFTER_CLASS, FIXED_WEEKS
    public String testScheduleDetail;

    public String status = "OPEN"; // OPEN, CLOSED
    public long createdAt;

    public Job() {}
}
