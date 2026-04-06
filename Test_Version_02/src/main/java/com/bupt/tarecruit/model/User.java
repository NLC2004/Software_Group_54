package com.bupt.tarecruit.model;

public class User {
    public String id;
    public String username;
    public String password;
    public String role; // TA, MO, ADMIN
    public String fullName;
    public String email;
    public String phone;
    public String gender;
    public String studentId;
    public String school;
    public String supervisor;
    public String degree;
    public String yearOfStudy;
    public boolean active = true;
    public long createdAt;
    // Optional template for ADMIN fine-grained role labeling.
    public String adminRoleTemplateId = "";

    public User() {}

    public User(String id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
    }
}
