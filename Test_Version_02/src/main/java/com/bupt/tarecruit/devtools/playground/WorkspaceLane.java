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

/**
 * Represents the workspace lane component of the TA recruitment system.
 */
public final class WorkspaceLane {
    private final String name;
    private String description;
    private int limit;
    private final List<WorkspaceCard> cards;

    /**
     * Creates a new workspace lane instance.
     */
    public WorkspaceLane(String name) {
        this.name = requireText(name, "name");
        this.description = "";
        this.limit = 0;
        this.cards = new ArrayList<>();
    }

    /**
     * Handles the get name operation.
     */
    public String getName() {
        return name;
    }

    /**
     * Handles the get description operation.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Handles the set description operation.
     */
    public WorkspaceLane setDescription(String description) {
        this.description = safeText(description);
        return this;
    }

    /**
     * Handles the get limit operation.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Handles the set limit operation.
     */
    public WorkspaceLane setLimit(int limit) {
        this.limit = Math.max(0, limit);
        return this;
    }

    /**
     * Handles the get cards operation.
     */
    public List<WorkspaceCard> getCards() {
        List<WorkspaceCard> copies = new ArrayList<>();
        for (WorkspaceCard card : cards) {
            copies.add(card.copy());
        }
        return Collections.unmodifiableList(copies);
    }

    /**
     * Handles the add card operation.
     */
    public WorkspaceLane addCard(WorkspaceCard card) {
        cards.add(Objects.requireNonNull(card, "card").copy());
        return this;
    }

    /**
     * Handles the insert card operation.
     */
    public WorkspaceLane insertCard(int index, WorkspaceCard card) {
        int safeIndex = Math.max(0, Math.min(index, cards.size()));
        cards.add(safeIndex, Objects.requireNonNull(card, "card").copy());
        return this;
    }

    /**
     * Handles the extract card operation.
     */
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

    /**
     * Handles the remove card by id operation.
     */
    public boolean removeCardById(String cardId) {
        return extractCard(cardId) != null;
    }

    /**
     * Handles the find card operation.
     */
    public WorkspaceCard findCard(String cardId) {
        for (WorkspaceCard card : cards) {
            if (card.getId().equals(cardId)) {
                return card.copy();
            }
        }
        return null;
    }

    /**
     * Handles the contains card operation.
     */
    public boolean containsCard(String cardId) {
        return findCard(cardId) != null;
    }

    /**
     * Handles the size operation.
     */
    public int size() {
        return cards.size();
    }

    /**
     * Handles the is over limit operation.
     */
    public boolean isOverLimit() {
        return limit > 0 && cards.size() > limit;
    }

    /**
     * Handles the remaining capacity operation.
     */
    public int remainingCapacity() {
        if (limit <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, limit - cards.size());
    }

    /**
     * Handles the move card to front operation.
     */
    public WorkspaceLane moveCardToFront(String cardId) {
        WorkspaceCard card = extractCard(cardId);
        if (card != null) {
            cards.add(0, card);
        }
        return this;
    }

    /**
     * Handles the move card to back operation.
     */
    public WorkspaceLane moveCardToBack(String cardId) {
        WorkspaceCard card = extractCard(cardId);
        if (card != null) {
            cards.add(card);
        }
        return this;
    }

    /**
     * Handles the sort by priority operation.
     */
    public WorkspaceLane sortByPriority() {
        cards.sort(Comparator.comparing(WorkspaceCard::getPriority).reversed());
        return this;
    }

    /**
     * Handles the sort by updated time operation.
     */
    public WorkspaceLane sortByUpdatedTime() {
        cards.sort(Comparator.comparing(WorkspaceCard::getUpdatedAt).reversed());
        return this;
    }

    /**
     * Handles the sort by title operation.
     */
    public WorkspaceLane sortByTitle() {
        cards.sort(Comparator.comparing(WorkspaceCard::getTitle, String.CASE_INSENSITIVE_ORDER));
        return this;
    }

    /**
     * Handles the filter by state operation.
     */
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

    /**
     * Handles the all tags operation.
     */
    public Set<String> allTags() {
        Set<String> result = new LinkedHashSet<>();
        for (WorkspaceCard card : cards) {
            result.addAll(card.getTags());
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Handles the owners operation.
     */
    public Set<String> owners() {
        Set<String> result = new LinkedHashSet<>();
        for (WorkspaceCard card : cards) {
            result.add(card.getOwner());
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Handles the average completion percent operation.
     */
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

    /**
     * Handles the snapshot titles operation.
     */
    public List<String> snapshotTitles() {
        List<String> titles = new ArrayList<>();
        for (WorkspaceCard card : cards) {
            titles.add(card.getTitle());
        }
        return Collections.unmodifiableList(titles);
    }

    /**
     * Handles the export summary operation.
     */
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

    /**
     * Handles the copy operation.
     */
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
