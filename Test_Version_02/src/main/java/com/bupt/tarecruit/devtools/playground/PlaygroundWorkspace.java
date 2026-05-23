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

public final class PlaygroundWorkspace {
    private String title;
    private final LocalDateTime createdAt;
    private final LinkedHashMap<String, WorkspaceLane> lanes;
    private final MetricNotebook notebook;
    private final TimelineSketch timeline;
    private final List<String> notes;
    private final Map<String, String> metadata;

    public PlaygroundWorkspace(String title) {
        this.title = requireText(title, "title");
        this.createdAt = LocalDateTime.now();
        this.lanes = new LinkedHashMap<>();
        this.notebook = new MetricNotebook();
        this.timeline = new TimelineSketch(title + " Timeline");
        this.notes = new ArrayList<>();
        this.metadata = new LinkedHashMap<>();
    }

    public String getTitle() {
        return title;
    }

    public PlaygroundWorkspace rename(String title) {
        this.title = requireText(title, "title");
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public PlaygroundWorkspace createLane(String laneName) {
        lanes.putIfAbsent(requireText(laneName, "laneName"), new WorkspaceLane(laneName));
        return this;
    }

    public PlaygroundWorkspace addLane(WorkspaceLane lane) {
        WorkspaceLane copy = Objects.requireNonNull(lane, "lane").copy();
        lanes.put(copy.getName(), copy);
        return this;
    }

    public WorkspaceLane getLane(String laneName) {
        WorkspaceLane lane = lanes.get(safeText(laneName));
        return lane == null ? null : lane.copy();
    }

    public List<String> getLaneNames() {
        return Collections.unmodifiableList(new ArrayList<>(lanes.keySet()));
    }

    public PlaygroundWorkspace addCardToLane(String laneName, WorkspaceCard card) {
        WorkspaceLane lane = lanes.computeIfAbsent(requireText(laneName, "laneName"), WorkspaceLane::new);
        lane.addCard(Objects.requireNonNull(card, "card"));
        return this;
    }

    public PlaygroundWorkspace moveCard(String fromLane, String toLane, String cardId) {
        WorkspaceLane source = lanes.get(safeText(fromLane));
        if (source == null) {
            return this;
        }
        WorkspaceCard card = source.extractCard(cardId);
        if (card == null) {
            return this;
        }
        lanes.computeIfAbsent(requireText(toLane, "toLane"), WorkspaceLane::new).addCard(card);
        return this;
    }

    public WorkspaceCard findCard(String cardId) {
        for (WorkspaceLane lane : lanes.values()) {
            WorkspaceCard card = lane.findCard(cardId);
            if (card != null) {
                return card;
            }
        }
        return null;
    }

    public CardLocation locateCard(String cardId) {
        for (Map.Entry<String, WorkspaceLane> entry : lanes.entrySet()) {
            WorkspaceCard card = entry.getValue().findCard(cardId);
            if (card != null) {
                return new CardLocation(entry.getKey(), card);
            }
        }
        return null;
    }

    public boolean removeCard(String cardId) {
        for (WorkspaceLane lane : lanes.values()) {
            if (lane.removeCardById(cardId)) {
                return true;
            }
        }
        return false;
    }

    public int totalCards() {
        int total = 0;
        for (WorkspaceLane lane : lanes.values()) {
            total += lane.size();
        }
        return total;
    }

    public Map<WorkspaceCard.State, Integer> totalByState() {
        Map<WorkspaceCard.State, Integer> counts = new LinkedHashMap<>();
        for (WorkspaceCard.State state : WorkspaceCard.State.values()) {
            counts.put(state, 0);
        }
        for (WorkspaceLane lane : lanes.values()) {
            Map<WorkspaceCard.State, Integer> laneCounts = lane.countByState();
            for (WorkspaceCard.State state : WorkspaceCard.State.values()) {
                counts.put(state, counts.get(state) + laneCounts.get(state));
            }
        }
        return Collections.unmodifiableMap(counts);
    }

    public Map<String, Integer> tagFrequency() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (WorkspaceLane lane : lanes.values()) {
            for (WorkspaceCard card : lane.getCards()) {
                for (String tag : card.getTags()) {
                    counts.put(tag, counts.getOrDefault(tag, 0) + 1);
                }
            }
        }
        return Collections.unmodifiableMap(counts);
    }

    public Map<String, Integer> ownerLoad() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (WorkspaceLane lane : lanes.values()) {
            for (WorkspaceCard card : lane.getCards()) {
                counts.put(card.getOwner(), counts.getOrDefault(card.getOwner(), 0) + 1);
            }
        }
        return Collections.unmodifiableMap(counts);
    }

    public List<WorkspaceCard> search(String keyword) {
        List<WorkspaceCard> result = new ArrayList<>();
        for (WorkspaceLane lane : lanes.values()) {
            for (WorkspaceCard card : lane.getCards()) {
                if (card.matchesKeyword(keyword)) {
                    result.add(card);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Set<String> allTags() {
        Set<String> result = new LinkedHashSet<>();
        for (WorkspaceLane lane : lanes.values()) {
            result.addAll(lane.allTags());
        }
        return Collections.unmodifiableSet(result);
    }

    public PlaygroundWorkspace addNote(String note) {
        String normalized = safeText(note);
        if (!normalized.isEmpty()) {
            notes.add(normalized);
        }
        return this;
    }

    public List<String> getNotes() {
        return Collections.unmodifiableList(new ArrayList<>(notes));
    }

    public PlaygroundWorkspace putMetadata(String key, String value) {
        metadata.put(requireText(key, "key"), safeText(value));
        return this;
    }

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    public MetricNotebook getNotebook() {
        return notebook.copy();
    }

    public TimelineSketch getTimeline() {
        return timeline.copy();
    }

    public PlaygroundWorkspace addMetricPoint(String seriesName, java.time.LocalDate day, double value) {
        notebook.addPoint(seriesName, day, value);
        return this;
    }

    public PlaygroundWorkspace addMilestone(String name, java.time.LocalDate day, String detail) {
        timeline.addMilestone(name, day, detail);
        return this;
    }

    public String summaryLine() {
        return title + " | lanes=" + lanes.size() + " | cards=" + totalCards() + " | notes=" + notes.size();
    }

    public Map<String, WorkspaceLane> snapshotLanes() {
        Map<String, WorkspaceLane> copy = new LinkedHashMap<>();
        for (Map.Entry<String, WorkspaceLane> entry : lanes.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return Collections.unmodifiableMap(copy);
    }

    public PlaygroundWorkspace copy() {
        PlaygroundWorkspace copy = new PlaygroundWorkspace(title);
        for (Map.Entry<String, WorkspaceLane> entry : lanes.entrySet()) {
            copy.lanes.put(entry.getKey(), entry.getValue().copy());
        }
        copy.notes.addAll(notes);
        copy.metadata.putAll(metadata);
        copy.notebook.merge(notebook);
        for (TimelineSketch.Milestone milestone : timeline.getMilestones()) {
            copy.timeline.addMilestone(milestone);
        }
        return copy;
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

    public static final class CardLocation {
        private final String laneName;
        private final WorkspaceCard card;

        public CardLocation(String laneName, WorkspaceCard card) {
            this.laneName = requireText(laneName, "laneName");
            this.card = Objects.requireNonNull(card, "card").copy();
        }

        public String getLaneName() {
            return laneName;
        }

        public WorkspaceCard getCard() {
            return card.copy();
        }
    }
}
