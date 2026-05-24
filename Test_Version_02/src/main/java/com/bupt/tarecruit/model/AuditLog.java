package com.bupt.tarecruit.model;

/**
 * Represents the audit log component of the TA recruitment system.
 */
public class AuditLog {
    /**
     * Stores the id value.
     */
    public String id;
    /**
     * Stores the user id value.
     */
    public String userId;
    /**
     * Stores the username value.
     */
    public String username;
    /**
     * Stores the action value.
     */
    public String action;
    /**
     * Stores the detail value.
     */
    public String detail;
    /**
     * Stores the ip value.
     */
    public String ip;
    /**
     * Stores the created at value.
     */
    public long createdAt;

    /**
     * Creates a new audit log instance.
     */
    public AuditLog() {}
}
