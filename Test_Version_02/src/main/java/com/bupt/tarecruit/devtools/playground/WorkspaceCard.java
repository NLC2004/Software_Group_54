package com.bupt.tarecruit.devtools.playground;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WorkspaceCard {
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum State {
        TODO,
        ACTIVE,
        BLOCKED,
        DONE,
        ARCHIVED
    }

    private final String id;
    private String title;
    private String summary;
    private String owner;
    private Priority priority;
    private State state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Set<String> tags;
    private final Map<String, String> attributes;
    private final List<ChecklistItem> checklist;

    public WorkspaceCard(String id, String title) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        this.summary = "";
        this.owner = "Unassigned";
        this.priority = Priority.MEDIUM;
        this.state = State.TODO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.tags = new LinkedHashSet<>();
        this.attributes = new LinkedHashMap<>();
        this.checklist = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public WorkspaceCard rename(String newTitle) {
        this.title = requireText(newTitle, "newTitle");
        touch();
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public WorkspaceCard updateSummary(String newSummary) {
        this.summary = safeText(newSummary);
        touch();
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public WorkspaceCard assignOwner(String newOwner) {
        this.owner = safeText(newOwner).isEmpty() ? "Unassigned" : newOwner.trim();
        touch();
        return this;
    }

    public Priority getPriority() {
        return priority;
    }

    public WorkspaceCard setPriority(Priority priority) {
        this.priority = Objects.requireNonNull(priority, "priority");
        touch();
        return this;
    }

    public State getState() {
        return state;
    }

    public WorkspaceCard setState(State state) {
        this.state = Objects.requireNonNull(state, "state");
        touch();
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public WorkspaceCard setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (updatedAt.isBefore(this.createdAt)) {
            updatedAt = this.createdAt;
        }
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public WorkspaceCard setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        return this;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(new ArrayList<>(tags));
    }

    public WorkspaceCard addTag(String tag) {
        String normalized = safeText(tag).toLowerCase();
        if (!normalized.isEmpty()) {
            tags.add(normalized);
            touch();
        }
        return this;
    }

    public WorkspaceCard addTags(List<String> values) {
        if (values == null) {
            return this;
        }
        for (String value : values) {
            addTag(value);
        }
        return this;
    }

    public WorkspaceCard removeTag(String tag) {
        String normalized = safeText(tag).toLowerCase();
        if (tags.remove(normalized)) {
            touch();
        }
        return this;
    }

    public WorkspaceCard clearTags() {
        if (!tags.isEmpty()) {
            tags.clear();
            touch();
        }
        return this;
    }

    public boolean hasTag(String tag) {
        return tags.contains(safeText(tag).toLowerCase());
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public WorkspaceCard putAttribute(String key, String value) {
        String normalizedKey = requireText(key, "key");
        attributes.put(normalizedKey, safeText(value));
        touch();
        return this;
    }

    public WorkspaceCard putAttributes(Map<String, String> values) {
        if (values == null) {
            return this;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            putAttribute(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public WorkspaceCard removeAttribute(String key) {
        if (key != null && attributes.remove(key.trim()) != null) {
            touch();
        }
        return this;
    }

    public List<ChecklistItem> getChecklist() {
        return Collections.unmodifiableList(new ArrayList<>(checklist));
    }

    public WorkspaceCard addChecklistItem(String label) {
        checklist.add(new ChecklistItem(label));
        touch();
        return this;
    }

    public WorkspaceCard addChecklistItem(String label, boolean done, String note) {
        ChecklistItem item = new ChecklistItem(label);
        if (done) {
            item.markDone();
        }
        item.setNote(note);
        checklist.add(item);
        touch();
        return this;
    }

    public WorkspaceCard removeChecklistItem(int index) {
        if (index >= 0 && index < checklist.size()) {
            checklist.remove(index);
            touch();
        }
        return this;
    }

    public WorkspaceCard markChecklistItemDone(int index) {
        if (index >= 0 && index < checklist.size()) {
            checklist.get(index).markDone();
            touch();
        }
        return this;
    }

    public WorkspaceCard reopenChecklistItem(int index) {
        if (index >= 0 && index < checklist.size()) {
            checklist.get(index).reopen();
            touch();
        }
        return this;
    }

    public double getCompletionPercent() {
        if (checklist.isEmpty()) {
            return 0.0;
        }
        int done = 0;
        for (ChecklistItem item : checklist) {
            if (item.isDone()) {
                done++;
            }
        }
        return done * 100.0 / checklist.size();
    }

    public boolean matchesKeyword(String keyword) {
        String probe = safeText(keyword).toLowerCase();
        if (probe.isEmpty()) {
            return true;
        }
        if (title.toLowerCase().contains(probe)) {
            return true;
        }
        if (summary.toLowerCase().contains(probe)) {
            return true;
        }
        if (owner.toLowerCase().contains(probe)) {
            return true;
        }
        for (String tag : tags) {
            if (tag.contains(probe)) {
                return true;
            }
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().toLowerCase().contains(probe) || entry.getValue().toLowerCase().contains(probe)) {
                return true;
            }
        }
        for (ChecklistItem item : checklist) {
            if (item.getLabel().toLowerCase().contains(probe) || item.getNote().toLowerCase().contains(probe)) {
                return true;
            }
        }
        return false;
    }

    public WorkspaceCard copy() {
        WorkspaceCard copy = new WorkspaceCard(id, title);
        copy.summary = summary;
        copy.owner = owner;
        copy.priority = priority;
        copy.state = state;
        copy.createdAt = createdAt;
        copy.updatedAt = updatedAt;
        copy.tags.addAll(tags);
        copy.attributes.putAll(attributes);
        for (ChecklistItem item : checklist) {
            copy.checklist.add(item.copy());
        }
        return copy;
    }

    public String toSummaryLine() {
        return "[" + id + "] " + title + " | " + state + " | " + priority + " | " + owner;
    }

    private void touch() {
        updatedAt = LocalDateTime.now();
    }

    private static String requireText(String value, String field) {
        String normalized = safeText(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class ChecklistItem {
        private String label;
        private boolean done;
        private String note;

        public ChecklistItem(String label) {
            this.label = requireText(label, "label");
            this.done = false;
            this.note = "";
        }

        public String getLabel() {
            return label;
        }

        public ChecklistItem rename(String newLabel) {
            this.label = requireText(newLabel, "newLabel");
            return this;
        }

        public boolean isDone() {
            return done;
        }

        public ChecklistItem markDone() {
            this.done = true;
            return this;
        }

        public ChecklistItem reopen() {
            this.done = false;
            return this;
        }

        public String getNote() {
            return note;
        }

        public ChecklistItem setNote(String note) {
            this.note = safeText(note);
            return this;
        }

        public ChecklistItem copy() {
            ChecklistItem copy = new ChecklistItem(label);
            copy.done = done;
            copy.note = note;
            return copy;
        }

        public String toDisplayLine() {
            String marker = done ? "[x] " : "[ ] ";
            if (note.isEmpty()) {
                return marker + label;
            }
            return marker + label + " (" + note + ")";
        }
    }
}
