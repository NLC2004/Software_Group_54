package com.bupt.tarecruit.devtools.playground;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the workspace archive component of the TA recruitment system.
 */
public final class WorkspaceArchive {
    private WorkspaceArchive() {
    }

    /**
     * Handles the export workspace operation.
     */
    public static List<String> exportWorkspace(PlaygroundWorkspace workspace) {
        List<String> lines = new ArrayList<>();
        lines.add("WORKSPACE|" + escape(workspace.getTitle()));

        for (Map.Entry<String, String> entry : workspace.getMetadata().entrySet()) {
            lines.add("META|" + escape(entry.getKey()) + "|" + escape(entry.getValue()));
        }

        for (String note : workspace.getNotes()) {
            lines.add("NOTE|" + escape(note));
        }

        for (Map.Entry<String, WorkspaceLane> entry : workspace.snapshotLanes().entrySet()) {
            WorkspaceLane lane = entry.getValue();
            lines.add("LANE|" + escape(lane.getName()) + "|" + escape(lane.getDescription()) + "|" + lane.getLimit());
            for (WorkspaceCard card : lane.getCards()) {
                lines.add("CARD|"
                        + escape(lane.getName()) + "|"
                        + escape(card.getId()) + "|"
                        + escape(card.getTitle()) + "|"
                        + escape(card.getSummary()) + "|"
                        + escape(card.getOwner()) + "|"
                        + card.getPriority() + "|"
                        + card.getState() + "|"
                        + escape(String.join(",", card.getTags())));

                for (Map.Entry<String, String> attribute : card.getAttributes().entrySet()) {
                    lines.add("ATTR|" + escape(card.getId()) + "|" + escape(attribute.getKey()) + "|" + escape(attribute.getValue()));
                }

                for (WorkspaceCard.ChecklistItem item : card.getChecklist()) {
                    lines.add("CHECK|"
                            + escape(card.getId()) + "|"
                            + escape(item.getLabel()) + "|"
                            + item.isDone() + "|"
                            + escape(item.getNote()));
                }
            }
        }

        for (TimelineSketch.Milestone milestone : workspace.getTimeline().getMilestones()) {
            lines.add("MILESTONE|"
                    + escape(milestone.getName()) + "|"
                    + milestone.getDay() + "|"
                    + escape(milestone.getDetail()) + "|"
                    + escape(String.join(",", milestone.getLabels())));
        }

        for (String seriesName : workspace.getNotebook().getSeriesNames()) {
            MetricNotebook.MetricSeries series = workspace.getNotebook().getSeries(seriesName);
            if (series != null) {
                for (MetricNotebook.MetricPoint point : series.getPoints()) {
                    lines.add("METRIC|"
                            + escape(seriesName) + "|"
                            + point.getDay() + "|"
                            + point.getValue());
                }
            }
        }

        return Collections.unmodifiableList(lines);
    }

    /**
     * Handles the import workspace operation.
     */
    public static PlaygroundWorkspace importWorkspace(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }

        PlaygroundWorkspace workspace = null;
        Map<String, WorkspaceCard> cardsById = new LinkedHashMap<>();

        for (String rawLine : lines) {
            if (rawLine == null || rawLine.isBlank()) {
                continue;
            }
            List<String> parts = split(rawLine);
            String type = parts.get(0);

            if ("WORKSPACE".equals(type)) {
                workspace = new PlaygroundWorkspace(unescape(parts.get(1)));
                continue;
            }

            if (workspace == null) {
                throw new IllegalStateException("WORKSPACE record must come first");
            }

            switch (type) {
                case "META" -> workspace.putMetadata(unescape(parts.get(1)), unescape(parts.get(2)));
                case "NOTE" -> workspace.addNote(unescape(parts.get(1)));
                case "LANE" -> {
                    WorkspaceLane lane = new WorkspaceLane(unescape(parts.get(1)));
                    lane.setDescription(unescape(parts.get(2)));
                    lane.setLimit(Integer.parseInt(parts.get(3)));
                    workspace.addLane(lane);
                }
                case "CARD" -> {
                    WorkspaceCard card = new WorkspaceCard(unescape(parts.get(2)), unescape(parts.get(3)));
                    card.updateSummary(unescape(parts.get(4)));
                    card.assignOwner(unescape(parts.get(5)));
                    card.setPriority(WorkspaceCard.Priority.valueOf(parts.get(6)));
                    card.setState(WorkspaceCard.State.valueOf(parts.get(7)));
                    String tags = unescape(parts.get(8));
                    if (!tags.isBlank()) {
                        for (String tag : tags.split(",")) {
                            card.addTag(tag);
                        }
                    }
                    workspace.addCardToLane(unescape(parts.get(1)), card);
                    cardsById.put(card.getId(), card);
                }
                case "ATTR" -> {
                    WorkspaceCard card = cardsById.get(unescape(parts.get(1)));
                    if (card != null) {
                        card.putAttribute(unescape(parts.get(2)), unescape(parts.get(3)));
                        syncCard(workspace, card);
                    }
                }
                case "CHECK" -> {
                    WorkspaceCard card = cardsById.get(unescape(parts.get(1)));
                    if (card != null) {
                        card.addChecklistItem(unescape(parts.get(2)), Boolean.parseBoolean(parts.get(3)), unescape(parts.get(4)));
                        syncCard(workspace, card);
                    }
                }
                case "MILESTONE" -> {
                    TimelineSketch.Milestone milestone = new TimelineSketch.Milestone(
                            unescape(parts.get(1)),
                            LocalDate.parse(parts.get(2)),
                            unescape(parts.get(3))
                    );
                    String labels = unescape(parts.get(4));
                    if (!labels.isBlank()) {
                        for (String label : labels.split(",")) {
                            milestone.addLabel(label);
                        }
                    }
                    workspace.getTimeline().addMilestone(milestone);
                    workspace.addMilestone(milestone.getName(), milestone.getDay(), milestone.getDetail());
                }
                case "METRIC" -> workspace.addMetricPoint(unescape(parts.get(1)), LocalDate.parse(parts.get(2)), Double.parseDouble(parts.get(3)));
                default -> {
                }
            }
        }

        return workspace;
    }

    /**
     * Handles the export bundle operation.
     */
    public static ArchiveBundle exportBundle(PlaygroundWorkspace workspace) {
        return new ArchiveBundle(exportWorkspace(workspace), TextSnapshotRenderer.renderWorkspace(workspace));
    }

    private static void syncCard(PlaygroundWorkspace workspace, WorkspaceCard card) {
        PlaygroundWorkspace.CardLocation location = workspace.locateCard(card.getId());
        if (location == null) {
            return;
        }
        workspace.removeCard(card.getId());
        workspace.addCardToLane(location.getLaneName(), card);
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private static String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (escaped) {
                if (current == 'n') {
                    builder.append('\n');
                } else {
                    builder.append(current);
                }
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private static List<String> split(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (escaped) {
                builder.append('\\').append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '|') {
                parts.add(unescape(builder.toString()));
                builder.setLength(0);
            } else {
                builder.append(current);
            }
        }
        parts.add(unescape(builder.toString()));
        return parts;
    }

    /**
     * Immutable data record for archive bundle.
     */
    public record ArchiveBundle(List<String> lines, String preview) {
    }
}
