package com.bupt.tarecruit.model;

public class AdminAuditLog {
    public String id;
    public String adminUserId;
    public String adminUsername;
    public String action;
    public String targetType;
    public String targetId;
    public String detail;
    public long createdAt;

    public AdminAuditLog() {}
}
