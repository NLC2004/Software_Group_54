package com.bupt.tarecruit.devtools.playground;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

public final class WorkspaceSeedFactory {
    private static final List<String> ADJECTIVES = List.of(
            "Quiet", "Curious", "Bright", "Open", "Warm", "Crisp", "Clear", "Soft", "Brisk", "Vivid"
    );

    private static final List<String> NOUNS = List.of(
            "Canvas", "Notebook", "Studio", "Harbor", "Pattern", "Signal", "Archive", "Orbit", "Bridge", "Trail"
    );

    private static final List<String> TAGS = List.of(
            "alpha", "review", "backup", "draft", "sample", "focus", "ideas", "staging", "notes", "weekly"
    );

    private static final List<String> OWNERS = List.of(
            "Alex", "Rin", "Maya", "Owen", "Chris", "Jamie", "Taylor", "Sky", "Morgan", "Avery"
    );

    private static final List<String> LANE_NAMES = List.of(
            "Inbox", "Sketching", "Refining", "Ready", "Archive"
    );

    private WorkspaceSeedFactory() {
    }

    public static PlaygroundWorkspace createWorkspace(long seed) {
        Random random = new Random(seed);
        PlaygroundWorkspace workspace = new PlaygroundWorkspace(buildTitle(random));

        for (String laneName : LANE_NAMES) {
            workspace.createLane(laneName);
        }

        workspace.putMetadata("seed", String.valueOf(seed));
        workspace.putMetadata("profile", "playground");
        workspace.putMetadata("generator", "WorkspaceSeedFactory");
        workspace.addNote("Standalone playground data. Not connected to application runtime.");
        workspace.addNote("Useful for local experiments, text rendering, and archive export.");

        populateCards(workspace, random);
        populateTimeline(workspace, random);
        populateMetrics(workspace, random);
        return workspace;
    }

    public static PlaygroundWorkspace createWorkspace(long seed, int cardsPerLane) {
        PlaygroundWorkspace workspace = createWorkspace(seed);
        Random random = new Random(seed * 31 + 7);

        for (String laneName : workspace.getLaneNames()) {
            int existing = workspace.getLane(laneName).size();
            for (int i = existing; i < Math.max(existing, cardsPerLane); i++) {
                workspace.addCardToLane(laneName, buildCard(random, laneName, i));
            }
        }

        return workspace;
    }

    public static String renderSeedPreview(long seed) {
        PlaygroundWorkspace workspace = createWorkspace(seed);
        return TextSnapshotRenderer.renderWorkspace(workspace);
    }

    private static void populateCards(PlaygroundWorkspace workspace, Random random) {
        int idCounter = 1;
        for (String laneName : workspace.getLaneNames()) {
            int cardCount = laneName.equals("Archive") ? 2 : 4;
            for (int i = 0; i < cardCount; i++) {
                WorkspaceCard card = buildCard(random, laneName, idCounter++);
                if ("Ready".equals(laneName)) {
                    card.setState(WorkspaceCard.State.DONE);
                    card.markChecklistItemDone(0).markChecklistItemDone(1);
                } else if ("Archive".equals(laneName)) {
                    card.setState(WorkspaceCard.State.ARCHIVED);
                } else if ("Refining".equals(laneName) && i % 2 == 0) {
                    card.setState(WorkspaceCard.State.ACTIVE);
                } else if ("Sketching".equals(laneName) && i == 0) {
                    card.setState(WorkspaceCard.State.BLOCKED);
                }
                workspace.addCardToLane(laneName, card);
            }
        }
    }

    private static WorkspaceCard buildCard(Random random, String laneName, int index) {
        WorkspaceCard card = new WorkspaceCard("PG-" + String.format("%03d", index), buildTitle(random));
        card.updateSummary("A generic playground card for local experiments in the " + laneName + " lane.");
        card.assignOwner(pick(OWNERS, random));
        card.setPriority(WorkspaceCard.Priority.values()[random.nextInt(WorkspaceCard.Priority.values().length)]);
        card.addTag(pick(TAGS, random));
        card.addTag(pick(TAGS, random));
        card.putAttribute("lane_hint", laneName.toLowerCase());
        card.putAttribute("seed_group", String.valueOf(index % 4));
        card.addChecklistItem("Capture a starting note");
        card.addChecklistItem("Review the card snapshot");
        card.addChecklistItem("Decide whether to keep or archive");
        return card;
    }

    private static void populateTimeline(PlaygroundWorkspace workspace, Random random) {
        LocalDate start = LocalDate.now().minusDays(5);
        workspace.addMilestone("Kickoff", start, "Create the initial playground board.");
        workspace.addMilestone("Snapshot Review", start.plusDays(3), "Render a text snapshot for a quick read-through.");
        workspace.addMilestone("Archive Export", start.plusDays(6), "Export the workspace into a flat line-based archive.");
        workspace.addMilestone("Metric Check", start.plusDays(8), "Inspect overall movement in the metric notebook.");
        workspace.addMilestone("Cleanup", start.plusDays(10 + random.nextInt(4)), "Leave the playground package self-contained.");
    }

    private static void populateMetrics(PlaygroundWorkspace workspace, Random random) {
        LocalDate start = LocalDate.now().minusDays(6);
        double focus = 55 + random.nextInt(15);
        double clarity = 48 + random.nextInt(12);
        double backlog = 20 + random.nextInt(10);

        for (int i = 0; i < 7; i++) {
            focus += random.nextInt(7) - 2;
            clarity += random.nextInt(5) - 1;
            backlog += random.nextInt(5) - 2;

            workspace.addMetricPoint("Focus Score", start.plusDays(i), focus);
            workspace.addMetricPoint("Clarity Score", start.plusDays(i), clarity);
            workspace.addMetricPoint("Open Items", start.plusDays(i), backlog);
        }
    }

    private static String buildTitle(Random random) {
        return pick(ADJECTIVES, random) + " " + pick(NOUNS, random);
    }

    private static String pick(List<String> values, Random random) {
        return values.get(random.nextInt(values.size()));
    }
}
