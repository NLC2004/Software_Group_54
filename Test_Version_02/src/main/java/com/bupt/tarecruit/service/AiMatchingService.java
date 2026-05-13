package com.bupt.tarecruit.service;

import com.bupt.tarecruit.model.Application;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class AiMatchingService {
    private static final double HOURS_PER_PERIOD = 0.75;

    private final DataService ds;
    private final Gson gson = new Gson();

    private static final Map<String, List<String>> SKILL_ALIASES = createSkillAliases();

    public AiMatchingService(DataService ds) {
        this.ds = ds;
    }

    public Map<String, Object> match(Job job, User applicant, String coverLetter, Application currentApplication) {
        Set<String> required = inferRequiredSkills(job);
        Set<String> applicantSkills = inferApplicantSkills(applicant, coverLetter);
        Set<String> matched = new TreeSet<>(required);
        matched.retainAll(applicantSkills);
        Set<String> missing = new TreeSet<>(required);
        missing.removeAll(applicantSkills);

        double skillScore;
        if (required.isEmpty()) {
            skillScore = applicantSkills.isEmpty() ? 45 : 65;
        } else {
            skillScore = (matched.size() * 100.0) / required.size();
        }

        double profileScore = profileCompleteness(applicant);
        WorkloadSignal workload = workloadSignal(applicant, job, currentApplication);
        int finalScore = (int) Math.round(skillScore * 0.70 + profileScore * 0.15 + workload.score * 0.15);
        finalScore = Math.max(0, Math.min(100, finalScore));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", finalScore);
        result.put("label", labelForScore(finalScore, workload.risk));
        result.put("requiredSkills", new ArrayList<>(required));
        result.put("matchedSkills", new ArrayList<>(matched));
        result.put("missingSkills", new ArrayList<>(missing));
        result.put("applicantSkills", new ArrayList<>(applicantSkills));
        result.put("profileCompleteness", Math.round(profileScore));
        result.put("workloadRisk", workload.risk);
        result.put("currentPeakWeeklyHours", workload.currentPeak);
        result.put("projectedPeakWeeklyHours", workload.projectedPeak);
        result.put("maxWeeklyHours", workload.maxWeeklyHours);
        result.put("recommendation", recommendation(finalScore, missing, workload));
        result.put("explanation", explanation(required, matched, missing, workload));
        return result;
    }

    private Set<String> inferRequiredSkills(Job job) {
        StringBuilder text = new StringBuilder();
        if (job != null) {
            append(text, job.title);
            append(text, job.courseName);
            append(text, job.description);
            append(text, job.schedule);
            if (job.requirements != null) {
                for (String r : job.requirements) append(text, r);
            }
            String tv = normalizeJobType(job.type);
            if ("LAB_TA".equals(tv)) append(text, "lab experiment equipment technical");
            if ("FINAL_EXAM_TA".equals(tv) || "CLASS_TEST_TA".equals(tv)) append(text, "invigilation exam supervision responsibility");
            if ("COURSE_TA".equals(tv)) append(text, "teaching communication tutorial");
        }
        return detectSkills(text.toString());
    }

    private Set<String> inferApplicantSkills(User applicant, String coverLetter) {
        StringBuilder text = new StringBuilder();
        if (applicant != null) {
            append(text, applicant.fullName);
            append(text, applicant.school);
            append(text, applicant.degree);
            append(text, applicant.yearOfStudy);
            append(text, applicant.supervisor);
        }
        append(text, coverLetter);
        return detectSkills(text.toString());
    }

    private Set<String> detectSkills(String raw) {
        String text = normalize(raw);
        Set<String> out = new TreeSet<>();
        if (text.isBlank()) return out;
        for (Map.Entry<String, List<String>> entry : SKILL_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (containsPhrase(text, alias)) {
                    out.add(entry.getKey());
                    break;
                }
            }
        }
        return out;
    }

    private boolean containsPhrase(String text, String alias) {
        String phrase = normalize(alias);
        if (phrase.isBlank()) return false;
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private double profileCompleteness(User user) {
        if (user == null) return 0;
        int total = 8;
        int filled = 0;
        if (hasText(user.fullName)) filled++;
        if (hasText(user.email)) filled++;
        if (hasText(user.phone)) filled++;
        if (hasText(user.studentId)) filled++;
        if (hasText(user.school)) filled++;
        if (hasText(user.degree)) filled++;
        if (hasText(user.yearOfStudy)) filled++;
        if (hasText(user.gender)) filled++;
        return filled * 100.0 / total;
    }

    private WorkloadSignal workloadSignal(User applicant, Job targetJob, Application currentApplication) {
        WorkloadSignal signal = new WorkloadSignal();
        signal.maxWeeklyHours = maxWeeklyHours();
        if (applicant == null) return signal;

        TreeMap<Integer, Double> weekly = new TreeMap<>();
        for (Application app : ds.getApplicationsByApplicant(applicant.id)) {
            if (!"APPROVED".equals(app.status)) continue;
            if (currentApplication != null && Objects.equals(currentApplication.id, app.id)) continue;
            Job job = ds.getJobById(app.jobId);
            mergeWeeklyMap(weekly, computeJobWeeklyHours(job));
        }

        signal.currentPeak = peak(weekly);
        mergeWeeklyMap(weekly, computeJobWeeklyHours(targetJob));
        signal.projectedPeak = peak(weekly);
        if (signal.projectedPeak > signal.maxWeeklyHours) {
            signal.risk = "HIGH";
            signal.score = 20;
        } else if (signal.projectedPeak > signal.maxWeeklyHours * 0.8) {
            signal.risk = "MEDIUM";
            signal.score = 65;
        } else {
            signal.risk = "LOW";
            signal.score = 100;
        }
        return signal;
    }

    public Map<Integer, Double> computeJobWeeklyHours(Job job) {
        if (job == null) return Collections.emptyMap();
        String tv = normalizeJobType(job.type);
        if ("FINAL_EXAM_TA".equals(tv)) {
            double dur = job.examDuration > 0 ? job.examDuration : job.weeklyHours;
            return dur > 0 ? Map.of(0, dur) : Collections.emptyMap();
        }

        TreeMap<Integer, Double> weekly = new TreeMap<>();
        mergeWeeklyMap(weekly, parseWeeklyHoursFromScheduleEntriesJson(job.courseScheduleGrid));
        mergeWeeklyMap(weekly, parseWeeklyHoursFromScheduleEntriesJson(job.labSessions));
        mergeWeeklyMap(weekly, parseWeeklyHoursFromScheduleEntriesJson(job.testScheduleDetail));
        if (weekly.isEmpty() && job.weeklyHours > 0) {
            int start = job.courseWeekStart > 0 ? job.courseWeekStart : 1;
            int end = job.courseWeekEnd >= start ? job.courseWeekEnd : start;
            for (int week = start; week <= end; week++) weekly.put(week, job.weeklyHours);
        }
        return weekly;
    }

    private Map<Integer, Double> parseWeeklyHoursFromScheduleEntriesJson(String json) {
        if (!hasText(json)) return Collections.emptyMap();
        try {
            JsonElement el = gson.fromJson(json, JsonElement.class);
            if (el == null || !el.isJsonArray()) return Collections.emptyMap();
            JsonArray arr = el.getAsJsonArray();
            TreeMap<Integer, Double> out = new TreeMap<>();
            for (JsonElement one : arr) {
                if (one == null || !one.isJsonObject()) continue;
                JsonObject obj = one.getAsJsonObject();
                int week = obj.has("week") ? obj.get("week").getAsInt() : 0;
                if (!obj.has("selection") || !obj.get("selection").isJsonObject()) continue;
                JsonObject sel = obj.getAsJsonObject("selection");
                int periods = 0;
                for (String day : List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) {
                    if (sel.has(day) && sel.get(day).isJsonArray()) {
                        periods += sel.getAsJsonArray(day).size();
                    }
                }
                double hours = periods * HOURS_PER_PERIOD;
                if (hours > 0) out.put(week, out.getOrDefault(week, 0.0) + hours);
            }
            return out;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private void mergeWeeklyMap(Map<Integer, Double> acc, Map<Integer, Double> add) {
        if (acc == null || add == null) return;
        for (Map.Entry<Integer, Double> e : add.entrySet()) {
            double h = e.getValue() == null ? 0 : e.getValue();
            if (h <= 0) continue;
            acc.put(e.getKey(), acc.getOrDefault(e.getKey(), 0.0) + h);
        }
    }

    private double peak(Map<Integer, Double> weekly) {
        return weekly.values().stream().mapToDouble(v -> v == null ? 0 : v).max().orElse(0);
    }

    private double maxWeeklyHours() {
        try {
            String v = ds.getSettings().get("maxWeeklyHours");
            return hasText(v) ? Double.parseDouble(v) : 20;
        } catch (Exception ignored) {
            return 20;
        }
    }

    private String recommendation(int score, Set<String> missing, WorkloadSignal workload) {
        if ("HIGH".equals(workload.risk)) return "Review workload before approval";
        if (score >= 80 && missing.isEmpty()) return "Strong match";
        if (score >= 60) return "Potential match; review missing skills";
        return "Needs manual review";
    }

    private String explanation(Set<String> required, Set<String> matched, Set<String> missing, WorkloadSignal workload) {
        List<String> parts = new ArrayList<>();
        if (required.isEmpty()) {
            parts.add("No explicit skill requirement was detected, so the result relies on profile completeness and workload.");
        } else {
            parts.add("Matched " + matched.size() + " of " + required.size() + " detected required skills.");
        }
        if (!missing.isEmpty()) {
            parts.add("Missing: " + String.join(", ", missing) + ".");
        }
        parts.add("Projected peak workload is " + formatHours(workload.projectedPeak) + "h / " + formatHours(workload.maxWeeklyHours) + "h.");
        return String.join(" ", parts);
    }

    private String labelForScore(int score, String workloadRisk) {
        if ("HIGH".equals(workloadRisk)) return "Workload risk";
        if (score >= 80) return "Strong match";
        if (score >= 60) return "Potential match";
        return "Needs review";
    }

    private String normalizeJobType(String type) {
        String raw = type == null ? "" : type.trim();
        if ("COURSE".equalsIgnoreCase(raw)) return "COURSE_TA";
        if ("ACTIVITY".equalsIgnoreCase(raw)) return "CLASS_TEST_TA";
        return raw.toUpperCase(Locale.ROOT);
    }

    private static Map<String, List<String>> createSkillAliases() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("communication", List.of("communication", "communicate", "presentation", "expression", "answer questions", "student questions"));
        m.put("teaching", List.of("teaching", "tutorial", "tutoring", "teach", "classroom", "assist students"));
        m.put("programming", List.of("programming", "coding", "code", "software", "java", "python", "algorithm", "computer science"));
        m.put("data analysis", List.of("data analysis", "statistics", "statistical", "excel", "chart", "analysis"));
        m.put("laboratory", List.of("laboratory", "lab", "experiment", "equipment"));
        m.put("invigilation", List.of("invigilation", "invigilate", "exam supervision", "exam", "test supervision"));
        m.put("organization", List.of("organization", "organised", "organized", "coordination", "schedule", "management"));
        m.put("responsibility", List.of("responsibility", "responsible", "reliable", "punctual", "confidentiality"));
        m.put("english", List.of("english", "bilingual", "translation", "international school"));
        m.put("problem solving", List.of("problem solving", "troubleshooting", "debug", "debugging", "solve problems"));
        return m;
    }

    private void append(StringBuilder sb, String value) {
        if (hasText(value)) sb.append(' ').append(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String formatHours(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static class WorkloadSignal {
        double currentPeak = 0;
        double projectedPeak = 0;
        double maxWeeklyHours = 20;
        double score = 100;
        String risk = "LOW";
    }
}
