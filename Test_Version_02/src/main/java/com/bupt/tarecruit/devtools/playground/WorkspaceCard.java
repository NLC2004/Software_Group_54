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

/**
 * Represents the workspace card component of the TA recruitment system.
 */
public final class WorkspaceCard {
    /**
     * Enumerates the supported priority values.
     */
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Enumerates the supported state values.
     */
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

    /**
     * Creates a new workspace card instance.
     */
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

    /**
     * Handles the get id operation.
     */
    public String getId() {
        return id;
    }

    /**
     * Handles the get title operation.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Handles the rename operation.
     */
    public WorkspaceCard rename(String newTitle) {
        this.title = requireText(newTitle, "newTitle");
        touch();
        return this;
    }

    /**
     * Handles the get summary operation.
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Handles the update summary operation.
     */
    public WorkspaceCard updateSummary(String newSummary) {
        this.summary = safeText(newSummary);
        touch();
        return this;
    }

    /**
     * Handles the get owner operation.
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Handles the assign owner operation.
     */
    public WorkspaceCard assignOwner(String newOwner) {
        this.owner = safeText(newOwner).isEmpty() ? "Unassigned" : newOwner.trim();
        touch();
        return this;
    }

    /**
     * Handles the get priority operation.
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Handles the set priority operation.
     */
    public WorkspaceCard setPriority(Priority priority) {
        this.priority = Objects.requireNonNull(priority, "priority");
        touch();
        return this;
    }

    /**
     * Handles the get state operation.
     */
    public State getState() {
        return state;
    }

    /**
     * Handles the set state operation.
     */
    public WorkspaceCard setState(State state) {
        this.state = Objects.requireNonNull(state, "state");
        touch();
        return this;
    }

    /**
     * Handles the get created at operation.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Handles the set created at operation.
     */
    public WorkspaceCard setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (updatedAt.isBefore(this.createdAt)) {
            updatedAt = this.createdAt;
        }
        return this;
    }

    /**
     * Handles the get updated at operation.
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Handles the set updated at operation.
     */
    public WorkspaceCard setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        return this;
    }

    /**
     * Handles the get tags operation.
     */
    public List<String> getTags() {
        return Collections.unmodifiableList(new ArrayList<>(tags));
    }

    /**
     * Handles the add tag operation.
     */
    public WorkspaceCard addTag(String tag) {
        String normalized = safeText(tag).toLowerCase();
        if (!normalized.isEmpty()) {
            tags.add(normalized);
            touch();
        }
        return this;
    }

    /**
     * Handles the add tags operation.
     */
    public WorkspaceCard addTags(List<String> values) {
        if (values == null) {
            return this;
        }
        for (String value : values) {
            addTag(value);
        }
        return this;
    }

    /**
     * Handles the remove tag operation.
     */
    public WorkspaceCard removeTag(String tag) {
        String normalized = safeText(tag).toLowerCase();
        if (tags.remove(normalized)) {
            touch();
        }
        return this;
    }

    /**
     * Handles the clear tags operation.
     */
    public WorkspaceCard clearTags() {
        if (!tags.isEmpty()) {
            tags.clear();
            touch();
        }
        return this;
    }

    /**
     * Handles the has tag operation.
     */
    public boolean hasTag(String tag) {
        return tags.contains(safeText(tag).toLowerCase());
    }

    /**
     * Handles the get attributes operation.
     */
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    /**
     * Handles the put attribute operation.
     */
    public WorkspaceCard putAttribute(String key, String value) {
        String normalizedKey = requireText(key, "key");
        attributes.put(normalizedKey, safeText(value));
        touch();
        return this;
    }

    /**
     * Handles the put attributes operation.
     */
    public WorkspaceCard putAttributes(Map<String, String> values) {
        if (values == null) {
            return this;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            putAttribute(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Handles the remove attribute operation.
     */
    public WorkspaceCard removeAttribute(String key) {
        if (key != null && attributes.remove(key.trim()) != null) {
            touch();
        }
        return this;
    }

    /**
     * Handles the get checklist operation.
     */
    public List<ChecklistItem> getChecklist() {
        return Collections.unmodifiableList(new ArrayList<>(checklist));
    }

    /**
     * Handles the add checklist item operation.
     */
    public WorkspaceCard addChecklistItem(String label) {
        checklist.add(new ChecklistItem(label));
        touch();
        return this;
    }

    /**
     * Handles the add checklist item operation.
     */
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

    /**
     * Handles the remove checklist item operation.
     */
    public WorkspaceCard removeChecklistItem(int index) {
        if (index >= 0 && index < checklist.size()) {
            checklist.remove(index);
            touch();
        }
        return this;
    }

    /**
     * Handles the mark checklist item done operation.
     */
    public WorkspaceCard markChecklistItemDone(int index) {
        if (index >= 0 && index < checklist.size()) {
            checklist.get(index).markDone();
            touch();
        }
        return this;
    }

    /**
     * Handles the reopen checklist item operation.
     */
    public WorkspaceCard reopenChecklistItem(int index) {
        if (index >= 0 && index < checklist.size()) {
            checklist.get(index).reopen();
            touch();
        }
        return this;
    }

    /**
     * Handles the get completion percent operation.
     */
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

    /**
     * Handles the matches keyword operation.
     */
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

    /**
     * Handles the copy operation.
     */
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

    /**
     * Handles the to summary line operation.
     */
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

    /**
     * Represents the checklist item component of the TA recruitment system.
     */
    public static final class ChecklistItem {
        private String label;
        private boolean done;
        private String note;

        /**
         * Creates a new checklist item instance.
         */
        public ChecklistItem(String label) {
            this.label = requireText(label, "label");
            this.done = false;
            this.note = "";
        }

        /**
         * Handles the get label operation.
         */
        public String getLabel() {
            return label;
        }

        /**
         * Handles the rename operation.
         */
        public ChecklistItem rename(String newLabel) {
            this.label = requireText(newLabel, "newLabel");
            return this;
        }

        /**
         * Handles the is done operation.
         */
        public boolean isDone() {
            return done;
        }

        /**
         * Handles the mark done operation.
         */
        public ChecklistItem markDone() {
            this.done = true;
            return this;
        }

        /**
         * Handles the reopen operation.
         */
        public ChecklistItem reopen() {
            this.done = false;
            return this;
        }

        /**
         * Handles the get note operation.
         */
        public String getNote() {
            return note;
        }

        /**
         * Handles the set note operation.
         */
        public ChecklistItem setNote(String note) {
            this.note = safeText(note);
            return this;
        }

        /**
         * Handles the copy operation.
         */
        public ChecklistItem copy() {
            ChecklistItem copy = new ChecklistItem(label);
            copy.done = done;
            copy.note = note;
            return copy;
        }

        /**
         * Handles the to display line operation.
         */
        public String toDisplayLine() {
            String marker = done ? "[x] " : "[ ] ";
            if (note.isEmpty()) {
                return marker + label;
            }
            return marker + label + " (" + note + ")";
        }
    }
}
