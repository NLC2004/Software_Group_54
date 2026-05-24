package com.bupt.tarecruit.model;

/**
 * Represents the password reset request component of the TA recruitment system.
 */
public class PasswordResetRequest {
    /**
     * Stores the id value.
     */
    public String id;
    /**
     * Stores the student id value.
     */
    public String studentId;
    /**
     * Stores the role value.
     */
    public String role; // TA or MO — disambiguates when ID numbers overlap across portals
    /**
     * Stores the full name value.
     */
    public String fullName;
    /**
     * Stores the email value.
     */
    public String email;
    /**
     * Stores the phone value.
     */
    public String phone;
    /**
     * Stores the notes value.
     */
    public String notes;
    /**
     * Stores the status value.
     */
    public String status = "PENDING"; // PENDING, APPROVED, REJECTED
    /**
     * Stores the reason value.
     */
    public String reason;
    /**
     * Stores the created at value.
     */
    public long createdAt;
    /**
     * Stores the processed at value.
     */
    public long processedAt;

    /**
     * Creates a new password reset request instance.
     */
    public PasswordResetRequest() {}
}
