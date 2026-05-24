package com.bupt.tarecruit.model;

import java.util.ArrayList;
import java.util.List;

/** Optional administrative role label for ADMIN accounts (separate from TA/MO/ADMIN). */
/**
 * Represents the admin role template component of the TA recruitment system.
 */
public class AdminRoleTemplate {
    /**
     * Stores the id value.
     */
    public String id;
    /**
     * Stores the name value.
     */
    public String name;
    /**
     * Stores the description value.
     */
    public String description;
    /**
     * Stores the tags value.
     */
    public List<String> tags = new ArrayList<>();
    /**
     * Stores the created at value.
     */
    public long createdAt;

    /**
     * Creates a new admin role template instance.
     */
    public AdminRoleTemplate() {}
}
