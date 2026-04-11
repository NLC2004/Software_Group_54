package com.bupt.tarecruit.model;

import java.util.ArrayList;
import java.util.List;

/** Optional administrative role label for ADMIN accounts (separate from TA/MO/ADMIN). */
public class AdminRoleTemplate {
    public String id;
    public String name;
    public String description;
    public List<String> tags = new ArrayList<>();
    public long createdAt;

    public AdminRoleTemplate() {}
}
