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

public final class TimelineSketch {
    private String title;
    private final List<Milestone> milestones;

    public TimelineSketch(String title) {
        this.title = requireText(title, "title");
        this.milestones = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public TimelineSketch rename(String title) {
        this.title = requireText(title, "title");
        return this;
    }

    public TimelineSketch addMilestone(String name, LocalDate day, String detail) {
        milestones.add(new Milestone(name, day, detail));
        milestones.sort(Comparator.comparing(Milestone::getDay).thenComparing(Milestone::getName));
        return this;
    }

    public TimelineSketch addMilestone(Milestone milestone) {
        Milestone copy = Objects.requireNonNull(milestone, "milestone").copy();
        milestones.add(copy);
        milestones.sort(Comparator.comparing(Milestone::getDay).thenComparing(Milestone::getName));
        return this;
    }

    public List<Milestone> getMilestones() {
        List<Milestone> copy = new ArrayList<>();
        for (Milestone milestone : milestones) {
            copy.add(milestone.copy());
        }
        return Collections.unmodifiableList(copy);
    }

    public boolean removeMilestone(String name) {
        for (int i = 0; i < milestones.size(); i++) {
            if (milestones.get(i).getName().equalsIgnoreCase(safeText(name))) {
                milestones.remove(i);
                return true;
            }
        }
        return false;
    }

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

    public TimelineSketch shiftAll(int days) {
        for (Milestone milestone : milestones) {
            milestone.shift(days);
        }
        milestones.sort(Comparator.comparing(Milestone::getDay).thenComparing(Milestone::getName));
        return this;
    }

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

    public long spanDays() {
        if (milestones.size() < 2) {
            return 0;
        }
        LocalDate start = milestones.get(0).getDay();
        LocalDate end = milestones.get(milestones.size() - 1).getDay();
        return ChronoUnit.DAYS.between(start, end);
    }

    public Milestone earliest() {
        if (milestones.isEmpty()) {
            return null;
        }
        return milestones.get(0).copy();
    }

    public Milestone latest() {
        if (milestones.isEmpty()) {
            return null;
        }
        return milestones.get(milestones.size() - 1).copy();
    }

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

    public List<String> summaryLines() {
        List<String> lines = new ArrayList<>();
        for (Milestone milestone : milestones) {
            lines.add(milestone.toLine());
        }
        return Collections.unmodifiableList(lines);
    }

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

    public static final class Milestone {
        private String name;
        private LocalDate day;
        private String detail;
        private final Set<String> labels;

        public Milestone(String name, LocalDate day, String detail) {
            this.name = requireText(name, "name");
            this.day = Objects.requireNonNull(day, "day");
            this.detail = safeText(detail);
            this.labels = new LinkedHashSet<>();
        }

        public String getName() {
            return name;
        }

        public Milestone rename(String name) {
            this.name = requireText(name, "name");
            return this;
        }

        public LocalDate getDay() {
            return day;
        }

        public Milestone moveTo(LocalDate day) {
            this.day = Objects.requireNonNull(day, "day");
            return this;
        }

        public Milestone shift(int days) {
            this.day = this.day.plusDays(days);
            return this;
        }

        public String getDetail() {
            return detail;
        }

        public Milestone setDetail(String detail) {
            this.detail = safeText(detail);
            return this;
        }

        public Milestone addLabel(String label) {
            String normalized = safeText(label).toLowerCase();
            if (!normalized.isEmpty()) {
                labels.add(normalized);
            }
            return this;
        }

        public Milestone addLabels(List<String> values) {
            if (values == null) {
                return this;
            }
            for (String value : values) {
                addLabel(value);
            }
            return this;
        }

        public List<String> getLabels() {
            return Collections.unmodifiableList(new ArrayList<>(labels));
        }

        public Milestone copy() {
            Milestone copy = new Milestone(name, day, detail);
            copy.labels.addAll(labels);
            return copy;
        }

        public String toLine() {
            if (labels.isEmpty()) {
                return day + " | " + name + " | " + detail;
            }
            return day + " | " + name + " | " + detail + " | " + String.join(", ", labels);
        }
    }

    public static final class TimelineWindow {
        private final LocalDate start;
        private final LocalDate end;

        public TimelineWindow(LocalDate start, LocalDate end) {
            this.start = Objects.requireNonNull(start, "start");
            this.end = Objects.requireNonNull(end, "end");
            if (end.isBefore(start)) {
                throw new IllegalArgumentException("end must not be before start");
            }
        }

        public LocalDate getStart() {
            return start;
        }

        public LocalDate getEnd() {
            return end;
        }

        public boolean contains(LocalDate day) {
            return !day.isBefore(start) && !day.isAfter(end);
        }
    }
}
