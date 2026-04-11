package com.bupt.tarecruit.model;

public class AuditLog {
    public String id;
    public String userId;
    public String username;
    public String action;
    public String detail;
    public String ip;
    public long createdAt;

    public AuditLog() {}
}
