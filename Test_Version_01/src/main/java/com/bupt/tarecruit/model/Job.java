package com.bupt.tarecruit.model;

import java.util.ArrayList;
import java.util.List;

public class Job {
    public String id;
    public String postedBy;
    public String title;
    public String type; // COURSE, ACTIVITY
    public String courseName;
    public String description;
    public List<String> requirements = new ArrayList<>();
    public int quota;
    public String schedule;
    public String deadline;
    public String salary;
    public double weeklyHours;
    public String status = "OPEN"; // OPEN, CLOSED
    public long createdAt;

    public Job() {}
}
