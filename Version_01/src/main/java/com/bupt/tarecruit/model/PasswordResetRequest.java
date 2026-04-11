package com.bupt.tarecruit.model;

public class PasswordResetRequest {
    public String id;
    public String userId;
    public String username;
    public String role;
    public String fullName;
    public String email;
    public String phone;
    public String reason;
    public String status = "PENDING"; // PENDING, APPROVED, REJECTED
    public String reviewComment;
    public String reviewedBy;
    public long createdAt;
    public long updatedAt;

    public PasswordResetRequest() {}
}
