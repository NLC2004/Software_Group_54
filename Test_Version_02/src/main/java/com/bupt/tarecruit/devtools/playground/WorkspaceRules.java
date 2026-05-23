package com.bupt.tarecruit.devtools.playground;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class WorkspaceRules {
    private WorkspaceRules() {
    }

    public static WorkspaceCard sanitizeCard(WorkspaceCard source) {
        WorkspaceCard card = source.copy();
        card.rename(normalizeTitle(card.getTitle()));
        card.updateSummary(normalizeParagraph(card.getSummary()));
        card.assignOwner(normalizeOwner(card.getOwner()));

        List<String> tags = card.getTags();
        card.clearTags();
        for (String tag : tags) {
            String normalized = normalizeTag(tag);
            if (!normalized.isEmpty()) {
                card.addTag(normalized);
            }
        }

        Map<String, String> attributes = card.getAttributes();
        for (String key : new ArrayList<>(attributes.keySet())) {
            card.removeAttribute(key);
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if (!key.isEmpty()) {
                card.putAttribute(key, normalizeParagraph(entry.getValue()));
            }
        }
        return card;
    }

    public static WorkspaceLane sanitizeLane(WorkspaceLane source) {
        WorkspaceLane lane = new WorkspaceLane(normalizeTitle(source.getName()));
        lane.setDescription(normalizeParagraph(source.getDescription()));
        lane.setLimit(source.getLimit());
        for (WorkspaceCard card : source.getCards()) {
            lane.addCard(sanitizeCard(card));
        }
        return lane;
    }

    public static PlaygroundWorkspace sanitizeWorkspace(PlaygroundWorkspace source) {
        PlaygroundWorkspace workspace = new PlaygroundWorkspace(normalizeTitle(source.getTitle()));
        for (Map.Entry<String, WorkspaceLane> entry : source.snapshotLanes().entrySet()) {
            workspace.addLane(sanitizeLane(entry.getValue()));
        }
        for (String note : source.getNotes()) {
            workspace.addNote(normalizeParagraph(note));
        }
        for (Map.Entry<String, String> entry : source.getMetadata().entrySet()) {
            workspace.putMetadata(normalizeKey(entry.getKey()), normalizeParagraph(entry.getValue()));
        }
        for (String seriesName : source.getNotebook().getSeriesNames()) {
            MetricNotebook.MetricSeries series = source.getNotebook().getSeries(seriesName);
            if (series != null) {
                for (MetricNotebook.MetricPoint point : series.getPoints()) {
                    workspace.addMetricPoint(normalizeTitle(seriesName), point.getDay(), point.getValue());
                }
            }
        }
        for (TimelineSketch.Milestone milestone : source.getTimeline().getMilestones()) {
            workspace.addMilestone(normalizeTitle(milestone.getName()), milestone.getDay(), normalizeParagraph(milestone.getDetail()));
        }
        return workspace;
    }

    public static ValidationResult validateCard(WorkspaceCard card) {
        ValidationResult result = new ValidationResult();
        if (card.getTitle().isBlank()) {
            result.addIssue("card.title", "Title must not be blank");
        }
        if (card.getId().isBlank()) {
            result.addIssue("card.id", "Id must not be blank");
        }
        if (card.getOwner().isBlank()) {
            result.addIssue("card.owner", "Owner should be present");
        }
        if (card.getTitle().length() > 80) {
            result.addIssue("card.title", "Title is longer than 80 characters");
        }
        if (card.getSummary().length() > 240) {
            result.addIssue("card.summary", "Summary is longer than 240 characters");
        }
        if (card.getTags().size() > 8) {
            result.addIssue("card.tags", "Card has more than 8 tags");
        }
        for (String tag : card.getTags()) {
            if (!tag.equals(normalizeTag(tag))) {
                result.addIssue("card.tags", "Tag is not normalized: " + tag);
            }
        }
        int doneCount = 0;
        for (WorkspaceCard.ChecklistItem item : card.getChecklist()) {
            if (item.getLabel().isBlank()) {
                result.addIssue("card.checklist", "Checklist label must not be blank");
            }
            if (item.isDone()) {
                doneCount++;
            }
        }
        if (!card.getChecklist().isEmpty() && doneCount == card.getChecklist().size() && card.getState() != WorkspaceCard.State.DONE) {
            result.addIssue("card.state", "Checklist is fully done while state is not DONE");
        }
        if (card.getState() == WorkspaceCard.State.ARCHIVED && card.getPriority() == WorkspaceCard.Priority.CRITICAL) {
            result.addIssue("card.priority", "Archived cards should not stay CRITICAL");
        }
        return result;
    }

    public static ValidationResult validateLane(WorkspaceLane lane) {
        ValidationResult result = new ValidationResult();
        if (lane.getName().isBlank()) {
            result.addIssue("lane.name", "Lane name must not be blank");
        }
        if (lane.getLimit() < 0) {
            result.addIssue("lane.limit", "Lane limit must not be negative");
        }
        if (lane.isOverLimit()) {
            result.addIssue("lane.limit", "Lane exceeds limit");
        }
        List<String> ids = new ArrayList<>();
        for (WorkspaceCard card : lane.getCards()) {
            if (ids.contains(card.getId())) {
                result.addIssue("lane.cards", "Duplicate card id inside lane: " + card.getId());
            } else {
                ids.add(card.getId());
            }
            result.merge(validateCard(card));
        }
        return result;
    }

    public static ValidationResult validateWorkspace(PlaygroundWorkspace workspace) {
        ValidationResult result = new ValidationResult();
        if (workspace.getTitle().isBlank()) {
            result.addIssue("workspace.title", "Workspace title must not be blank");
        }
        if (workspace.snapshotLanes().isEmpty()) {
            result.addIssue("workspace.lanes", "Workspace should contain at least one lane");
        }
        List<String> seenIds = new ArrayList<>();
        for (WorkspaceLane lane : workspace.snapshotLanes().values()) {
            result.merge(validateLane(lane));
            for (WorkspaceCard card : lane.getCards()) {
                if (seenIds.contains(card.getId())) {
                    result.addIssue("workspace.cards", "Duplicate card id across lanes: " + card.getId());
                } else {
                    seenIds.add(card.getId());
                }
            }
        }
        for (Map.Entry<String, String> entry : workspace.getMetadata().entrySet()) {
            if (entry.getKey().isBlank()) {
                result.addIssue("workspace.metadata", "Metadata key must not be blank");
            }
        }
        return result;
    }

    public static String normalizeTitle(String value) {
        String normalized = collapseSpaces(value);
        if (normalized.isEmpty()) {
            return "Untitled";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    public static String normalizeParagraph(String value) {
        return collapseSpaces(value).replace(" .", ".").replace(" ,", ",");
    }

    public static String normalizeTag(String value) {
        return collapseSpaces(value).toLowerCase().replace(' ', '-');
    }

    public static String normalizeOwner(String value) {
        String normalized = collapseSpaces(value);
        return normalized.isEmpty() ? "Unassigned" : normalized;
    }

    public static String normalizeKey(String value) {
        return collapseSpaces(value).toLowerCase().replace(' ', '_');
    }

    private static String collapseSpaces(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public static final class ValidationResult {
        private final List<RuleIssue> issues;

        public ValidationResult() {
            this.issues = new ArrayList<>();
        }

        public ValidationResult addIssue(String field, String message) {
            issues.add(new RuleIssue(field, message));
            return this;
        }

        public ValidationResult merge(ValidationResult other) {
            if (other != null) {
                issues.addAll(other.issues);
            }
            return this;
        }

        public boolean isValid() {
            return issues.isEmpty();
        }

        public List<RuleIssue> getIssues() {
            return Collections.unmodifiableList(new ArrayList<>(issues));
        }

        public String toMultilineText() {
            if (issues.isEmpty()) {
                return "No issues";
            }
            StringBuilder builder = new StringBuilder();
            for (RuleIssue issue : issues) {
                if (builder.length() > 0) {
                    builder.append(System.lineSeparator());
                }
                builder.append(issue.field()).append(": ").append(issue.message());
            }
            return builder.toString();
        }
    }

    public record RuleIssue(String field, String message) {
    }
}
