package com.bupt.tarecruit.service;

import com.bupt.tarecruit.model.Application;
import com.bupt.tarecruit.model.Job;
import com.bupt.tarecruit.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Represents the ai matching service component of the TA recruitment system.
 */
public class AiMatchingService {
    private static final String API_BASE_URL = "https://jeniya.cn";
    private static final String API_KEY = "sk-RyysKIhqi4L2XiqvCPfMxAg3Ae0ygYYLfTGb5drCghmfsUy8";
    private static final String DEFAULT_MODEL = "gpt-5-mini";
    private static final Set<String> ALLOWED_MODELS = Set.of("gemini-2.5-pro", "qwen-plus", "gpt-5-mini");
    private static final Duration TIMEOUT = Duration.ofSeconds(120);
    private static final double HOURS_PER_PERIOD = 0.75;
    private static final Map<String, List<String>> SKILL_ALIASES = createSkillAliases();

    private final DataService ds;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Creates a new ai matching service instance.
     */
    public AiMatchingService(DataService ds) {
        this.ds = ds;
    }

    /**
     * Handles the match operation.
     */
    public Map<String, Object> match(Job job, User applicant, String coverLetter, Application currentApplication) {
        return match(job, applicant, coverLetter, currentApplication, DEFAULT_MODEL);
    }

    /**
     * Handles the match operation.
     */
    public Map<String, Object> match(Job job, User applicant, String coverLetter, Application currentApplication, String model) {
        String resolvedModel = normalizeModel(model);
        System.out.println("[AiMatchingService] match() invoked, model=" + resolvedModel
                + ", jobId=" + (job != null ? job.id : "null")
                + ", applicantId=" + (applicant != null ? applicant.id : "null")
                + ", applicationId=" + (currentApplication != null ? currentApplication.id : "null"));
        if (isMockApiEnabled()) {
            return buildMockApiResult(resolvedModel);
        }
        try {
            Map<String, Object> apiResult = callApi(job, applicant, coverLetter, currentApplication, resolvedModel);
            if (apiResult != null && !apiResult.isEmpty()) return apiResult;
        } catch (Exception ex) {
            System.out.println("[AiMatchingService] API call failed: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            ex.printStackTrace();
        }
        throw new IllegalStateException("AI API call failed. Please try again later.");
    }

    /**
     * Handles the is allowed model operation.
     */
    public boolean isAllowedModel(String model) {
        return ALLOWED_MODELS.contains(normalizeModel(model));
    }

    /**
     * Handles the get default model operation.
     */
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    private boolean isMockApiEnabled() {
        String prop = System.getProperty("ta.ai.mockApi", "");
        String env = System.getenv("TA_AI_MOCK_API");
        return "true".equalsIgnoreCase(prop) || "true".equalsIgnoreCase(env);
    }

    private Map<String, Object> callApi(Job job, User applicant, String coverLetter, Application currentApplication,
                                        String resolvedModel) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", resolvedModel);
        payload.addProperty("temperature", 0.2);
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        payload.add("response_format", responseFormat);

        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", "You are a TA recruitment matching assistant. Return ONLY valid JSON with the keys: score (0-100 integer), label, recommendation, explanation, requiredSkills (array of strings), matchedSkills (array of strings), missingSkills (array of strings), applicantSkills (array of strings), profileCompleteness (0-100 integer), workloadRisk (LOW/MEDIUM/HIGH), currentPeakWeeklyHours (number), projectedPeakWeeklyHours (number), maxWeeklyHours (number), cot (array of strings). Keep the result concise, grounded in the provided data, and provide a short reasoning trace in cot without exposing private data.");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", buildPrompt(job, applicant, coverLetter, currentApplication));
        messages.add(user);
        payload.add("messages", messages);

        String requestBody = gson.toJson(payload);
        System.out.println("[AiMatchingService] POST " + API_BASE_URL + "/v1/chat/completions");
        System.out.println("[AiMatchingService] Request model=" + resolvedModel);
        System.out.println("[AiMatchingService] Request body size=" + requestBody.length());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/v1/chat/completions"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .header("X-Requested-Model", resolvedModel)
                .header("X-Requested-From", "ta-recruit-match")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[AiMatchingService] Response status=" + response.statusCode());
        System.out.println("[AiMatchingService] Response body length=" + (response.body() == null ? 0 : response.body().length()));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("AI API returned HTTP " + response.statusCode());
        }

        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        if (root == null || !root.has("choices") || !root.get("choices").isJsonArray()) {
            throw new IllegalStateException("AI API response did not include choices");
        }
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices.isEmpty()) throw new IllegalStateException("AI API returned no choices");

        JsonObject first = choices.get(0).getAsJsonObject();
        if (!first.has("message") || !first.get("message").isJsonObject()) {
            throw new IllegalStateException("AI API response did not include a message");
        }
        JsonObject message = first.getAsJsonObject("message");
        String content = message.has("content") && !message.get("content").isJsonNull() ? message.get("content").getAsString() : "";
        if (content.isBlank()) throw new IllegalStateException("AI API returned empty content");

        JsonObject parsed = extractJsonObject(content);
        if (parsed == null) throw new IllegalStateException("AI API did not return valid JSON");

        Map<String, Object> out = normalizeApiResult(parsed, buildBaseResult(job, applicant, coverLetter, currentApplication));
        out.put("requestedModel", resolvedModel);
        out.put("apiBaseUrl", API_BASE_URL);
        out.put("apiCalled", true);
        return out;
    }

    private Map<String, Object> buildBaseResult(Job job, User applicant, String coverLetter, Application currentApplication) {
        WorkloadSignal workload = workloadSignal(applicant, job, currentApplication);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", 0);
        result.put("label", "AI match result");
        result.put("requiredSkills", new ArrayList<>());
        result.put("matchedSkills", new ArrayList<>());
        result.put("missingSkills", new ArrayList<>());
        result.put("applicantSkills", new ArrayList<>());
        result.put("profileCompleteness", Math.round(profileCompleteness(applicant)));
        result.put("workloadRisk", workload.risk);
        result.put("currentPeakWeeklyHours", workload.currentPeak);
        result.put("projectedPeakWeeklyHours", workload.projectedPeak);
        result.put("maxWeeklyHours", workload.maxWeeklyHours);
        result.put("recommendation", "Review the AI-generated report.");
        result.put("explanation", "");
        return result;
    }

    private Map<String, Object> buildMockApiResult(String resolvedModel) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", 82);
        result.put("label", "Strong match");
        result.put("requiredSkills", List.of("programming", "communication", "laboratory"));
        result.put("matchedSkills", List.of("programming", "communication"));
        result.put("missingSkills", List.of("laboratory"));
        result.put("applicantSkills", List.of("programming", "communication"));
        result.put("profileCompleteness", 90);
        result.put("workloadRisk", "LOW");
        result.put("currentPeakWeeklyHours", 0);
        result.put("projectedPeakWeeklyHours", 4);
        result.put("maxWeeklyHours", 20);
        result.put("recommendation", "API mock: suitable candidate; verify lab experience.");
        result.put("explanation", "Mocked API response for tests. Production calls the configured AI gateway.");
        result.put("cot", List.of("Compared job requirements with applicant materials.", "Identified matching communication and programming evidence."));
        result.put("requestedModel", resolvedModel);
        result.put("apiBaseUrl", API_BASE_URL);
        result.put("apiCalled", true);
        return result;
    }

    private String buildPrompt(Job job, User applicant, String coverLetter, Application currentApplication) {
        StringBuilder sb = new StringBuilder();
        sb.append("Evaluate the TA match and return JSON only.\n\n");

        // Only send essential job fields (skip schedule grids, timestamps, etc.)
        sb.append("Job: {");
        sb.append("\"title\":\"").append(job.title != null ? job.title : "").append("\",");
        sb.append("\"type\":\"").append(job.type != null ? job.type : "").append("\",");
        sb.append("\"courseName\":\"").append(job.courseName != null ? job.courseName : "").append("\",");
        sb.append("\"description\":\"").append(job.description != null ? job.description : "").append("\",");
        sb.append("\"requirements\":").append(gson.toJson(job.requirements));
        sb.append(",\"quota\":").append(job.quota);
        sb.append("}\n\n");

        // Only send essential applicant fields
        sb.append("Applicant: {");
        sb.append("\"fullName\":\"").append(applicant.fullName != null ? applicant.fullName : "").append("\",");
        sb.append("\"school\":\"").append(applicant.school != null ? applicant.school : "").append("\",");
        sb.append("\"degree\":\"").append(applicant.degree != null ? applicant.degree : "").append("\",");
        sb.append("\"yearOfStudy\":\"").append(applicant.yearOfStudy != null ? applicant.yearOfStudy : "").append("\"");
        sb.append("}\n\n");

        sb.append("CoverLetter: ").append(coverLetter == null ? "" : coverLetter).append("\n\n");
        if (currentApplication != null && currentApplication.cvFileName != null && !currentApplication.cvFileName.isBlank()) {
            sb.append("ResumeFileName: ").append(currentApplication.cvFileName).append("\n");
            String resumeSnippet = readResumeTextSnippet(currentApplication.cvFileName);
            if (!resumeSnippet.isBlank()) {
                sb.append("ResumeExtractedText: ").append(resumeSnippet).append("\n\n");
            } else {
                sb.append("ResumeContentNote: The submitted PDF resume is attached in the system as this file; text extraction was unavailable, so use the cover letter and structured applicant fields as resume evidence.\n\n");
            }
        }
        sb.append("Rules: score should reflect skill fit, profile completeness, and workload risk. If workloadRisk is HIGH, recommendation should warn about workload before approval.");
        return sb.toString();
    }

    private String readResumeTextSnippet(String fileName) {
        try {
            byte[] bytes = ds.getUpload(fileName);
            String raw = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            String printable = raw.replaceAll("[^\\p{Print}\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (printable.length() > 6000) return printable.substring(0, 6000);
            return printable;
        } catch (Exception ignored) {
            return "";
        }
    }

    private JsonObject extractJsonObject(String content) {
        try {
            String trimmed = content.trim();
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start < 0 || end <= start) return null;
            return gson.fromJson(trimmed.substring(start, end + 1), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> normalizeApiResult(JsonObject parsed, Map<String, Object> fallback) {
        Map<String, Object> out = new LinkedHashMap<>(fallback);
        putInt(parsed, out, "score", 0, 100);
        putString(parsed, out, "label");
        putString(parsed, out, "recommendation");
        putString(parsed, out, "explanation");
        putString(parsed, out, "workloadRisk");
        putNumber(parsed, out, "currentPeakWeeklyHours");
        putNumber(parsed, out, "projectedPeakWeeklyHours");
        putNumber(parsed, out, "maxWeeklyHours");
        putInt(parsed, out, "profileCompleteness", 0, 100);
        putStringArray(parsed, out, "requiredSkills");
        putStringArray(parsed, out, "matchedSkills");
        putStringArray(parsed, out, "missingSkills");
        putStringArray(parsed, out, "applicantSkills");
        putStringArray(parsed, out, "cot");
        return out;
    }

    private void putString(JsonObject parsed, Map<String, Object> out, String key) {
        if (parsed.has(key) && !parsed.get(key).isJsonNull()) out.put(key, parsed.get(key).getAsString());
    }

    private void putNumber(JsonObject parsed, Map<String, Object> out, String key) {
        if (parsed.has(key) && !parsed.get(key).isJsonNull()) {
            try {
                out.put(key, parsed.get(key).getAsDouble());
            } catch (Exception ignored) {
            }
        }
    }

    private void putInt(JsonObject parsed, Map<String, Object> out, String key, int min, int max) {
        if (parsed.has(key) && !parsed.get(key).isJsonNull()) {
            try {
                int v = parsed.get(key).getAsInt();
                out.put(key, Math.max(min, Math.min(max, v)));
            } catch (Exception ignored) {
            }
        }
    }

    private void putStringArray(JsonObject parsed, Map<String, Object> out, String key) {
        if (!parsed.has(key) || !parsed.get(key).isJsonArray()) return;
        List<String> values = new ArrayList<>();
        for (JsonElement el : parsed.getAsJsonArray(key)) {
            if (el != null && !el.isJsonNull()) values.add(el.getAsString());
        }
        out.put(key, values);
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

    /**
     * Handles the compute job weekly hours operation.
     */
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

    private String normalizeModel(String model) {
        String m = model == null ? "" : model.trim();
        if (m.isEmpty()) return DEFAULT_MODEL;
        return switch (m.toLowerCase(Locale.ROOT)) {
            case "gemini", "gemini-2.5-pro", "gemini2.5pro" -> "gemini-2.5-pro";
            case "qwen", "qwen-plus" -> "qwen-plus";
            case "gpt-5", "gpt-5-mini", "gpt5mini" -> "gpt-5-mini";
            default -> m;
        };
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
