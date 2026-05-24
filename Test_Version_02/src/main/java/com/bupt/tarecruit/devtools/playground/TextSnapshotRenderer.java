package com.bupt.tarecruit.devtools.playground;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the text snapshot renderer component of the TA recruitment system.
 */
public final class TextSnapshotRenderer {
    private TextSnapshotRenderer() {
    }

    /**
     * Handles the render workspace operation.
     */
    public static String renderWorkspace(PlaygroundWorkspace workspace) {
        StringBuilder builder = new StringBuilder();
        builder.append(boxTitle("Playground Workspace"));
        builder.append("Title: ").append(workspace.getTitle()).append(System.lineSeparator());
        builder.append("Created: ").append(workspace.getCreatedAt()).append(System.lineSeparator());
        builder.append("Summary: ").append(workspace.summaryLine()).append(System.lineSeparator());
        builder.append(System.lineSeparator());

        builder.append(boxTitle("Metadata"));
        if (workspace.getMetadata().isEmpty()) {
            builder.append("(none)").append(System.lineSeparator());
        } else {
            for (Map.Entry<String, String> entry : workspace.getMetadata().entrySet()) {
                builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(System.lineSeparator());
            }
        }
        builder.append(System.lineSeparator());

        builder.append(boxTitle("Notes"));
        if (workspace.getNotes().isEmpty()) {
            builder.append("(none)").append(System.lineSeparator());
        } else {
            for (String note : workspace.getNotes()) {
                builder.append("- ").append(note).append(System.lineSeparator());
            }
        }
        builder.append(System.lineSeparator());

        builder.append(boxTitle("Lanes"));
        for (Map.Entry<String, WorkspaceLane> entry : workspace.snapshotLanes().entrySet()) {
            builder.append(renderLane(entry.getValue()));
            builder.append(System.lineSeparator());
        }

        builder.append(boxTitle("Timeline"));
        builder.append(renderTimeline(workspace.getTimeline()));
        builder.append(System.lineSeparator());

        builder.append(boxTitle("Metrics"));
        builder.append(renderMetrics(workspace.getNotebook()));
        builder.append(System.lineSeparator());

        builder.append(boxTitle("Validation"));
        builder.append(renderValidation(WorkspaceRules.validateWorkspace(workspace)));
        return builder.toString();
    }

    /**
     * Handles the render lane operation.
     */
    public static String renderLane(WorkspaceLane lane) {
        StringBuilder builder = new StringBuilder();
        builder.append(lane.getName())
                .append(" (")
                .append(lane.size())
                .append(" card");
        if (lane.size() != 1) {
            builder.append("s");
        }
        builder.append(")");
        if (lane.getLimit() > 0) {
            builder.append(" / limit ").append(lane.getLimit());
        }
        builder.append(System.lineSeparator());

        if (!lane.getDescription().isBlank()) {
            builder.append("  ").append(lane.getDescription()).append(System.lineSeparator());
        }
        builder.append("  avg completion: ").append(format(lane.averageCompletionPercent())).append("%").append(System.lineSeparator());
        builder.append("  owners: ").append(joinOrPlaceholder(new ArrayList<>(lane.owners()))).append(System.lineSeparator());
        builder.append("  tags: ").append(joinOrPlaceholder(new ArrayList<>(lane.allTags()))).append(System.lineSeparator());
        for (WorkspaceCard card : lane.getCards()) {
            builder.append(renderCard(card));
        }
        return builder.toString();
    }

    /**
     * Handles the render card operation.
     */
    public static String renderCard(WorkspaceCard card) {
        StringBuilder builder = new StringBuilder();
        builder.append("  - ").append(card.toSummaryLine()).append(System.lineSeparator());
        if (!card.getSummary().isBlank()) {
            builder.append("    summary: ").append(card.getSummary()).append(System.lineSeparator());
        }
        if (!card.getTags().isEmpty()) {
            builder.append("    tags: ").append(String.join(", ", card.getTags())).append(System.lineSeparator());
        }
        if (!card.getAttributes().isEmpty()) {
            builder.append("    attributes: ").append(card.getAttributes()).append(System.lineSeparator());
        }
        if (!card.getChecklist().isEmpty()) {
            builder.append("    checklist:").append(System.lineSeparator());
            for (WorkspaceCard.ChecklistItem item : card.getChecklist()) {
                builder.append("      ").append(item.toDisplayLine()).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    /**
     * Handles the render timeline operation.
     */
    public static String renderTimeline(TimelineSketch timeline) {
        StringBuilder builder = new StringBuilder();
        builder.append("Name: ").append(timeline.getTitle()).append(System.lineSeparator());
        builder.append("Span: ").append(timeline.spanDays()).append(" day(s)").append(System.lineSeparator());
        if (timeline.getMilestones().isEmpty()) {
            builder.append("(no milestones)").append(System.lineSeparator());
            return builder.toString();
        }
        for (TimelineSketch.Milestone milestone : timeline.getMilestones()) {
            builder.append("- ").append(milestone.toLine()).append(System.lineSeparator());
        }
        if (!timeline.collisionDays().isEmpty()) {
            builder.append("Collision days: ").append(timeline.collisionDays()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    /**
     * Handles the render metrics operation.
     */
    public static String renderMetrics(MetricNotebook notebook) {
        StringBuilder builder = new StringBuilder();
        if (notebook.getSeriesNames().isEmpty()) {
            builder.append("(no series)").append(System.lineSeparator());
            return builder.toString();
        }
        for (String seriesName : notebook.getSeriesNames()) {
            MetricNotebook.MetricSummary summary = notebook.buildSummary(seriesName);
            builder.append("- ").append(seriesName)
                    .append(": count=").append(summary.getCount())
                    .append(", min=").append(format(summary.getMin()))
                    .append(", max=").append(format(summary.getMax()))
                    .append(", avg=").append(format(summary.getAverage()))
                    .append(", delta=").append(format(summary.getDelta()))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    /**
     * Handles the render validation operation.
     */
    public static String renderValidation(WorkspaceRules.ValidationResult validationResult) {
        if (validationResult.isValid()) {
            return "Workspace passes all local playground checks." + System.lineSeparator();
        }
        StringBuilder builder = new StringBuilder();
        for (WorkspaceRules.RuleIssue issue : validationResult.getIssues()) {
            builder.append("- ").append(issue.field()).append(": ").append(issue.message()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    /**
     * Handles the render table operation.
     */
    public static List<String> renderTable(Map<String, String> values) {
        List<String> lines = new ArrayList<>();
        int width = 0;
        for (String key : values.keySet()) {
            width = Math.max(width, key.length());
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            lines.add(pad(entry.getKey(), width) + " : " + entry.getValue());
        }
        return lines;
    }

    /**
     * Handles the box title operation.
     */
    public static String boxTitle(String title) {
        String body = " " + title + " ";
        String edge = repeat("=", body.length());
        return edge + System.lineSeparator() + body + System.lineSeparator() + edge + System.lineSeparator();
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.max(0, count); i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private static String pad(String value, int width) {
        StringBuilder builder = new StringBuilder(value);
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private static String joinOrPlaceholder(List<String> values) {
        if (values.isEmpty()) {
            return "(none)";
        }
        return String.join(", ", values);
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }
}
