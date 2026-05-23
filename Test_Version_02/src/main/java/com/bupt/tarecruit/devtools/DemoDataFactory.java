package com.bupt.tarecruit.devtools;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Standalone helper for generating generic showcase data during local experiments.
 * This class is intentionally not wired into the application runtime.
 */
public final class DemoDataFactory {
    private static final List<String> ADJECTIVES = List.of(
            "Bright", "Quiet", "Swift", "Open", "Fresh",
            "Clear", "Bold", "Calm", "Silver", "Amber"
    );

    private static final List<String> NOUNS = List.of(
            "Canvas", "Harbor", "Signal", "Stream", "Orbit",
            "Sketch", "Bridge", "Pattern", "Notebook", "Beacon"
    );

    private static final List<String> TAGS = List.of(
            "draft", "review", "backup", "priority", "focus",
            "archive", "snapshot", "planning", "sample", "preview"
    );

    private DemoDataFactory() {
    }

    public static DemoBundle createBundle(int cardCount, int dayCount, long seed) {
        int safeCardCount = Math.max(0, cardCount);
        int safeDayCount = Math.max(0, dayCount);
        Random random = new Random(seed);

        List<DemoCard> cards = new ArrayList<>(safeCardCount);
        for (int i = 0; i < safeCardCount; i++) {
            cards.add(createCard(i, random));
        }

        List<MetricPoint> trend = new ArrayList<>(safeDayCount);
        LocalDate startDate = LocalDate.now().minusDays(safeDayCount);
        int currentValue = 40 + random.nextInt(20);
        for (int i = 0; i < safeDayCount; i++) {
            currentValue += random.nextInt(9) - 3;
            currentValue = Math.max(10, currentValue);
            trend.add(new MetricPoint(startDate.plusDays(i), currentValue));
        }

        Map<String, Integer> tagWeights = new LinkedHashMap<>();
        for (String tag : TAGS) {
            tagWeights.put(tag, 1 + random.nextInt(9));
        }

        return new DemoBundle(cards, trend, tagWeights);
    }

    public static List<String> createHeadlines(int count, long seed) {
        int safeCount = Math.max(0, count);
        Random random = new Random(seed);
        List<String> headlines = new ArrayList<>(safeCount);
        for (int i = 0; i < safeCount; i++) {
            headlines.add(buildName(random) + " Update " + (i + 1));
        }
        return Collections.unmodifiableList(headlines);
    }

    private static DemoCard createCard(int index, Random random) {
        String title = buildName(random);
        String owner = "Owner-" + (100 + random.nextInt(900));
        LocalDate createdOn = LocalDate.now().minusDays(random.nextInt(30));
        List<String> tags = new ArrayList<>();
        tags.add(TAGS.get(random.nextInt(TAGS.size())));
        tags.add(TAGS.get(random.nextInt(TAGS.size())));
        return new DemoCard(index + 1, title, owner, createdOn, tags);
    }

    private static String buildName(Random random) {
        return ADJECTIVES.get(random.nextInt(ADJECTIVES.size()))
                + " "
                + NOUNS.get(random.nextInt(NOUNS.size()));
    }

    public static final class DemoBundle {
        private final List<DemoCard> cards;
        private final List<MetricPoint> trend;
        private final Map<String, Integer> tagWeights;

        public DemoBundle(List<DemoCard> cards, List<MetricPoint> trend, Map<String, Integer> tagWeights) {
            this.cards = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(cards, "cards")));
            this.trend = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(trend, "trend")));
            this.tagWeights = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(tagWeights, "tagWeights")));
        }

        public List<DemoCard> getCards() {
            return cards;
        }

        public List<MetricPoint> getTrend() {
            return trend;
        }

        public Map<String, Integer> getTagWeights() {
            return tagWeights;
        }
    }

    public static final class DemoCard {
        private final int id;
        private final String title;
        private final String owner;
        private final LocalDate createdOn;
        private final List<String> tags;

        public DemoCard(int id, String title, String owner, LocalDate createdOn, List<String> tags) {
            this.id = id;
            this.title = Objects.requireNonNull(title, "title");
            this.owner = Objects.requireNonNull(owner, "owner");
            this.createdOn = Objects.requireNonNull(createdOn, "createdOn");
            this.tags = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(tags, "tags")));
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getOwner() {
            return owner;
        }

        public LocalDate getCreatedOn() {
            return createdOn;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    public static final class MetricPoint {
        private final LocalDate day;
        private final int value;

        public MetricPoint(LocalDate day, int value) {
            this.day = Objects.requireNonNull(day, "day");
            this.value = value;
        }

        public LocalDate getDay() {
            return day;
        }

        public int getValue() {
            return value;
        }
    }
}
