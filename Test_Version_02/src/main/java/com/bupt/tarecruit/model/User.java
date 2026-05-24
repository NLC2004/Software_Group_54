package com.bupt.tarecruit.model;

/**
 * Represents the user component of the TA recruitment system.
 */
public class User {
    /**
     * Stores the id value.
     */
    public String id;
    /**
     * Stores the username value.
     */
    public String username;
    /**
     * Stores the password value.
     */
    public String password;
    /**
     * Stores the role value.
     */
    public String role; // TA, MO, ADMIN
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
     * Stores the gender value.
     */
    public String gender;
    /**
     * Stores the student id value.
     */
    public String studentId;
    /**
     * Stores the school value.
     */
    public String school;
    /**
     * Stores the supervisor value.
     */
    public String supervisor;
    /**
     * Stores the degree value.
     */
    public String degree;
    /**
     * Stores the year of study value.
     */
    public String yearOfStudy;
    /**
     * Stores the active value.
     */
    public boolean active = true;
    /**
     * Stores the created at value.
     */
    public long createdAt;
    // Optional template for ADMIN fine-grained role labeling.
    /**
     * Stores the admin role template id value.
     */
    public String adminRoleTemplateId = "";
    /**
     * Stores the ai match used count value.
     */
    public int aiMatchUsedCount = 0;

    /**
     * Creates a new user instance.
     */
    public User() {}

    /**
     * Creates a new user instance.
     */
    public User(String id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
    }
}
