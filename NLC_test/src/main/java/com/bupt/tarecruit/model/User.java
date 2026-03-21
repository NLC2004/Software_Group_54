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
    public boolean active = true;
    public long createdAt;

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
