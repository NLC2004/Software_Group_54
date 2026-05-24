package com.bupt.tarecruit.model;

/**
 * Represents the notification component of the TA recruitment system.
 */
public class Notification {
    /**
     * Stores the id value.
     */
    public String id;
    /**
     * Stores the user id value.
     */
    public String userId;
    /**
     * Stores the title value.
     */
    public String title;
    /**
     * Stores the content value.
     */
    public String content;
    /**
     * Stores the type value.
     */
    public String type; // SYSTEM, APPLICATION, DEADLINE, PASSWORD_RESET
    /**
     * Stores the read value.
     */
    public boolean read = false;
    /**
     * Stores the created at value.
     */
    public long createdAt;

    /**
     * Creates a new notification instance.
     */
    public Notification() {}
}
