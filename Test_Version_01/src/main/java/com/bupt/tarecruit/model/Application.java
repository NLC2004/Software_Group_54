package com.bupt.tarecruit.model;

public class Application {
    public String id;
    public String jobId;
    public String applicantId;
    public String cvFileName;
    public String coverLetter;
    public String status = "PENDING"; // PENDING, APPROVED, REJECTED, WITHDRAWN
    public int priority;
    public long createdAt;
    public long updatedAt;

    public Application() {}
}
