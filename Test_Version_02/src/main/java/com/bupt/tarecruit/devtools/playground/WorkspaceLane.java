package com.bupt.tarecruit.devtools.playground;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WorkspaceLane {
    private final String name;
    private String description;
    private int limit;
    private final List<WorkspaceCard> cards;

    public WorkspaceLane(String name) {
        this.name = requireText(name, "name");
        this.description = "";
        this.limit = 0;
        this.cards = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public WorkspaceLane setDescription(String description) {
        this.description = safeText(description);
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public WorkspaceLane setLimit(int limit) {
        this.limit = Math.max(0, limit);
        return this;
    }

    public List<WorkspaceCard> getCards() {
        List<WorkspaceCard> copies = new ArrayList<>();
        for (WorkspaceCard card : cards) {
            copies.add(card.copy());
        }
        return Collections.unmodifiableList(copies);
    }

    public WorkspaceLane addCard(WorkspaceCard card) {
        cards.add(Objects.requireNonNull(card, "card").copy());
        return this;
    }

    public WorkspaceLane insertCard(int index, WorkspaceCard card) {
        int safeIndex = Math.max(0, Math.min(index, cards.size()));
        cards.add(safeIndex, Objects.requireNonNull(card, "card").copy());
        return this;
    }

    public WorkspaceCard extractCard(String cardId) {
        for (int i = 0; i < cards.size(); i++) {
            WorkspaceCard card = cards.get(i);
            if (card.getId().equals(cardId)) {
                cards.remove(i);
                return card;
            }
        }
        return null;
    }

    public boolean removeCardById(String cardId) {
        return extractCard(cardId) != null;
    }

    public WorkspaceCard findCard(String cardId) {
        for (WorkspaceCard card : cards) {
            if (card.getId().equals(cardId)) {
                return card.copy();
            }
        }
        return null;
    }

    public boolean containsCard(String cardId) {
        return findCard(cardId) != null;
    }

    public int size() {
        return cards.size();
    }

    public boolean isOverLimit() {
        return limit > 0 && cards.size() > limit;
    }

    public int remainingCapacity() {
        if (limit <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, limit - cards.size());
    }

    public WorkspaceLane moveCardToFront(String cardId) {
        WorkspaceCard card = extractCard(cardId);
        if (card != null) {
            cards.add(0, card);
        }
        return this;
    }

    public WorkspaceLane moveCardToBack(String cardId) {
        WorkspaceCard card = extractCard(cardId);
        if (card != null) {
            cards.add(card);
        }
        return this;
    }

    public WorkspaceLane sortByPriority() {
        cards.sort(Comparator.comparing(WorkspaceCard::getPriority).reversed());
        return this;
    }

    public WorkspaceLane sortByUpdatedTime() {
        cards.sort(Comparator.comparing(WorkspaceCard::getUpdatedAt).reversed());
        return this;
    }

    public WorkspaceLane sortByTitle() {
        cards.sort(Comparator.comparing(WorkspaceCard::getTitle, String.CASE_INSENSITIVE_ORDER));
        return this;
    }

    public List<WorkspaceCard> filterByState(WorkspaceCard.State state) {
        List<WorkspaceCard> result = new ArrayList<>();
        for (WorkspaceCard card : cards) {
            if (card.getState() == state) {
                result.add(card.copy());
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Map<WorkspaceCard.State, Integer> countByState() {
        Map<WorkspaceCard.State, Integer> counts = new LinkedHashMap<>();
        for (WorkspaceCard.State state : WorkspaceCard.State.values()) {
            counts.put(state, 0);
        }
        for (WorkspaceCard card : cards) {
            counts.put(card.getState(), counts.get(card.getState()) + 1);
        }
        return Collections.unmodifiableMap(counts);
    }

    public Set<String> allTags() {
        Set<String> result = new LinkedHashSet<>();
        for (WorkspaceCard card : cards) {
            result.addAll(card.getTags());
        }
        return Collections.unmodifiableSet(result);
    }

    public Set<String> owners() {
        Set<String> result = new LinkedHashSet<>();
        for (WorkspaceCard card : cards) {
            result.add(card.getOwner());
        }
        return Collections.unmodifiableSet(result);
    }

    public double averageCompletionPercent() {
        if (cards.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (WorkspaceCard card : cards) {
            total += card.getCompletionPercent();
        }
        return total / cards.size();
    }

    public List<String> snapshotTitles() {
        List<String> titles = new ArrayList<>();
        for (WorkspaceCard card : cards) {
            titles.add(card.getTitle());
        }
        return Collections.unmodifiableList(titles);
    }

    public Map<String, String> exportSummary() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("description", description);
        result.put("limit", String.valueOf(limit));
        result.put("size", String.valueOf(cards.size()));
        result.put("overLimit", String.valueOf(isOverLimit()));
        result.put("owners", String.join(", ", owners()));
        result.put("tags", String.join(", ", allTags()));
        return Collections.unmodifiableMap(result);
    }

    public WorkspaceLane copy() {
        WorkspaceLane copy = new WorkspaceLane(name);
        copy.description = description;
        copy.limit = limit;
        for (WorkspaceCard card : cards) {
            copy.cards.add(card.copy());
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
}
