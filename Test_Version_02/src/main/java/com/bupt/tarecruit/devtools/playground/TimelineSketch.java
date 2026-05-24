package com.bupt.tarecruit.devtools.playground;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the timeline sketch component of the TA recruitment system.
 */
public final class TimelineSketch {
    private String title;
    private final List<Milestone> milestones;

    /**
     * Creates a new timeline sketch instance.
     */
    public TimelineSketch(String title) {
        this.title = requireText(title, "title");
        this.milestones = new ArrayList<>();
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
    public TimelineSketch rename(String title) {
        this.title = requireText(title, "title");
        return this;
    }

    /**
     * Handles the add milestone operation.
     */
    public TimelineSketch addMilestone(String name, LocalDate day, String detail) {
        milestones.add(new Milestone(name, day, detail));
        milestones.sort(Comparator.comparing(Milestone::getDay).thenComparing(Milestone::getName));
        return this;
    }

    /**
     * Handles the add milestone operation.
     */
    public TimelineSketch addMilestone(Milestone milestone) {
        Milestone copy = Objects.requireNonNull(milestone, "milestone").copy();
        milestones.add(copy);
        milestones.sort(Comparator.comparing(Milestone::getDay).thenComparing(Milestone::getName));
        return this;
    }

    /**
     * Handles the get milestones operation.
     */
    public List<Milestone> getMilestones() {
        List<Milestone> copy = new ArrayList<>();
        for (Milestone milestone : milestones) {
            copy.add(milestone.copy());
        }
        return Collections.unmodifiableList(copy);
    }

    /**
     * Handles the remove milestone operation.
     */
    public boolean removeMilestone(String name) {
        for (int i = 0; i < milestones.size(); i++) {
            if (milestones.get(i).getName().equalsIgnoreCase(safeText(name))) {
                milestones.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the shift milestone operation.
     */
    public TimelineSketch shiftMilestone(String name, int days) {
        for (Milestone milestone : milestones) {
            if (milestone.getName().equalsIgnoreCase(safeText(name))) {
                milestone.shift(days);
                milestones.sort(Comparator.comparing(Milestone::getDay).thenComparing(Milestone::getName));
                break;
            }
        }
        return this;
    }

    /**
     * Handles the shift all operation.
     */
    public TimelineSketch shiftAll(int days) {
        for (Milestone milestone : milestones) {
            milestone.shift(days);
        }
        milestones.sort(Comparator.comparing(Milestone::getDay).thenComparing(Milestone::getName));
        return this;
    }

    /**
     * Handles the between operation.
     */
    public List<Milestone> between(LocalDate start, LocalDate end) {
        TimelineWindow window = new TimelineWindow(start, end);
        List<Milestone> result = new ArrayList<>();
        for (Milestone milestone : milestones) {
            if (window.contains(milestone.getDay())) {
                result.add(milestone.copy());
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Handles the upcoming operation.
     */
    public List<Milestone> upcoming(LocalDate fromDay, int limit) {
        List<Milestone> result = new ArrayList<>();
        for (Milestone milestone : milestones) {
            if (!milestone.getDay().isBefore(fromDay)) {
                result.add(milestone.copy());
                if (result.size() == Math.max(0, limit)) {
                    break;
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Handles the span days operation.
     */
    public long spanDays() {
        if (milestones.size() < 2) {
            return 0;
        }
        LocalDate start = milestones.get(0).getDay();
        LocalDate end = milestones.get(milestones.size() - 1).getDay();
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Handles the earliest operation.
     */
    public Milestone earliest() {
        if (milestones.isEmpty()) {
            return null;
        }
        return milestones.get(0).copy();
    }

    /**
     * Handles the latest operation.
     */
    public Milestone latest() {
        if (milestones.isEmpty()) {
            return null;
        }
        return milestones.get(milestones.size() - 1).copy();
    }

    /**
     * Handles the collision days operation.
     */
    public Set<LocalDate> collisionDays() {
        Map<LocalDate, Integer> counts = new LinkedHashMap<>();
        for (Milestone milestone : milestones) {
            counts.put(milestone.getDay(), counts.getOrDefault(milestone.getDay(), 0) + 1);
        }
        Set<LocalDate> result = new LinkedHashSet<>();
        for (Map.Entry<LocalDate, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                result.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Handles the group by month operation.
     */
    public Map<String, List<Milestone>> groupByMonth() {
        Map<String, List<Milestone>> result = new LinkedHashMap<>();
        for (Milestone milestone : milestones) {
            String key = milestone.getDay().getYear() + "-" + String.format("%02d", milestone.getDay().getMonthValue());
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(milestone.copy());
        }
        for (Map.Entry<String, List<Milestone>> entry : result.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Handles the summary lines operation.
     */
    public List<String> summaryLines() {
        List<String> lines = new ArrayList<>();
        for (Milestone milestone : milestones) {
            lines.add(milestone.toLine());
        }
        return Collections.unmodifiableList(lines);
    }

    /**
     * Handles the copy operation.
     */
    public TimelineSketch copy() {
        TimelineSketch copy = new TimelineSketch(title);
        for (Milestone milestone : milestones) {
            copy.milestones.add(milestone.copy());
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

    /**
     * Represents the milestone component of the TA recruitment system.
     */
    public static final class Milestone {
        private String name;
        private LocalDate day;
        private String detail;
        private final Set<String> labels;

        /**
         * Creates a new milestone instance.
         */
        public Milestone(String name, LocalDate day, String detail) {
            this.name = requireText(name, "name");
            this.day = Objects.requireNonNull(day, "day");
            this.detail = safeText(detail);
            this.labels = new LinkedHashSet<>();
        }

        /**
         * Handles the get name operation.
         */
        public String getName() {
            return name;
        }

        /**
         * Handles the rename operation.
         */
        public Milestone rename(String name) {
            this.name = requireText(name, "name");
            return this;
        }

        /**
         * Handles the get day operation.
         */
        public LocalDate getDay() {
            return day;
        }

        /**
         * Handles the move to operation.
         */
        public Milestone moveTo(LocalDate day) {
            this.day = Objects.requireNonNull(day, "day");
            return this;
        }

        /**
         * Handles the shift operation.
         */
        public Milestone shift(int days) {
            this.day = this.day.plusDays(days);
            return this;
        }

        /**
         * Handles the get detail operation.
         */
        public String getDetail() {
            return detail;
        }

        /**
         * Handles the set detail operation.
         */
        public Milestone setDetail(String detail) {
            this.detail = safeText(detail);
            return this;
        }

        /**
         * Handles the add label operation.
         */
        public Milestone addLabel(String label) {
            String normalized = safeText(label).toLowerCase();
            if (!normalized.isEmpty()) {
                labels.add(normalized);
            }
            return this;
        }

        /**
         * Handles the add labels operation.
         */
        public Milestone addLabels(List<String> values) {
            if (values == null) {
                return this;
            }
            for (String value : values) {
                addLabel(value);
            }
            return this;
        }

        /**
         * Handles the get labels operation.
         */
        public List<String> getLabels() {
            return Collections.unmodifiableList(new ArrayList<>(labels));
        }

        /**
         * Handles the copy operation.
         */
        public Milestone copy() {
            Milestone copy = new Milestone(name, day, detail);
            copy.labels.addAll(labels);
            return copy;
        }

        /**
         * Handles the to line operation.
         */
        public String toLine() {
            if (labels.isEmpty()) {
                return day + " | " + name + " | " + detail;
            }
            return day + " | " + name + " | " + detail + " | " + String.join(", ", labels);
        }
    }

    /**
     * Represents the timeline window component of the TA recruitment system.
     */
    public static final class TimelineWindow {
        private final LocalDate start;
        private final LocalDate end;

        /**
         * Creates a new timeline window instance.
         */
        public TimelineWindow(LocalDate start, LocalDate end) {
            this.start = Objects.requireNonNull(start, "start");
            this.end = Objects.requireNonNull(end, "end");
            if (end.isBefore(start)) {
                throw new IllegalArgumentException("end must not be before start");
            }
        }

        /**
         * Handles the get start operation.
         */
        public LocalDate getStart() {
            return start;
        }

        /**
         * Handles the get end operation.
         */
        public LocalDate getEnd() {
            return end;
        }

        /**
         * Handles the contains operation.
         */
        public boolean contains(LocalDate day) {
            return !day.isBefore(start) && !day.isAfter(end);
        }
    }
}
