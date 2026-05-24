package com.bupt.tarecruit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the job component of the TA recruitment system.
 */
public class Job {
    /**
     * Stores the id value.
     */
    public String id;
    /**
     * Stores the posted by value.
     */
    public String postedBy;
    /**
     * Stores the title value.
     */
    public String title;
    /**
     * Stores the type value.
     */
    public String type; // COURSE_TA, LAB_TA, FINAL_EXAM_TA, CLASS_TEST_TA (legacy: COURSE, ACTIVITY)
    /**
     * Stores the course name value.
     */
    public String courseName;
    /**
     * Stores the description value.
     */
    public String description;
    /**
     * Stores the requirements value.
     */
    public List<String> requirements = new ArrayList<>();
    /**
     * Stores the quota value.
     */
    public int quota;
    /**
     * Stores the schedule value.
     */
    public String schedule;
    /**
     * Stores the weekly hours value.
     */
    public double weeklyHours;
    /**
     * Stores the deadline value.
     */
    public String deadline;

    // Course TA specific
    /**
     * Stores the course schedule grid value.
     */
    public String courseScheduleGrid; // JSON string: {"Mon":[1,2],"Tue":[3]...}
    /**
     * Stores the course week start value.
     */
    public int courseWeekStart;
    /**
     * Stores the course week end value.
     */
    public int courseWeekEnd;

    // Lab TA specific
    /**
     * Stores the lab session count value.
     */
    public int labSessionCount;
    /**
     * Stores the lab time value.
     */
    public String labTime;
    /**
     * Stores the lab location value.
     */
    public String labLocation;
    /**
     * Stores the lab sessions value.
     */
    public String labSessions; // JSON string for per-session week/location/time selections

    // Final Exam TA specific
    /**
     * Stores the exam date time value.
     */
    public String examDateTime; // ISO-like string from datetime-local
    /**
     * Stores the exam duration value.
     */
    public double examDuration; // hours
    /**
     * Stores the exam location value.
     */
    public String examLocation;

    // Class Test TA specific
    /**
     * Stores the test schedule type value.
     */
    public String testScheduleType; // IN_CLASS, AFTER_CLASS, FIXED_WEEKS
    /**
     * Stores the test schedule detail value.
     */
    public String testScheduleDetail;

    /**
     * Stores the status value.
     */
    public String status = "OPEN"; // OPEN, CLOSED
    /**
     * Stores the created at value.
     */
    public long createdAt;

    /**
     * Creates a new job instance.
     */
    public Job() {}
}
