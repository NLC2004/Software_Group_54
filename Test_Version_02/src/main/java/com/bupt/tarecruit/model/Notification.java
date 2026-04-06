package com.bupt.tarecruit.model;

public class Notification {
    public String id;
    public String userId;
    public String title;
    public String content;
    public String type; // SYSTEM, APPLICATION, DEADLINE, PASSWORD_RESET
    public boolean read = false;
    public long createdAt;

    public Notification() {}
}
