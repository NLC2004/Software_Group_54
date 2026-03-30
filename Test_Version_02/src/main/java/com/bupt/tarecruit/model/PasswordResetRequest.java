package com.bupt.tarecruit.model;

public class PasswordResetRequest {
    public String id;
    public String studentId;
    public String fullName;
    public String email;
    public String phone;
    public String notes;
    /** TA or MO — from applicant portal */
    public String role = "";
    public String status = "PENDING"; // PENDING, APPROVED, REJECTED
    public String reason;
    public long createdAt;
    public long processedAt;

    public PasswordResetRequest() {}
}
