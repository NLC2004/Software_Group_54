import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

class Main {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path PROFILES_FILE = DATA_DIR.resolve("profiles.txt");
    private static final Path JOBS_FILE = DATA_DIR.resolve("jobs.txt");
    private static final Path APPLICATIONS_FILE = DATA_DIR.resolve("applications.txt");

    public static void main(String[] args) {
        try {
            bootstrapFiles();
            runCli();
        } catch (Exception ex) {
            System.out.println("Fatal error: " + ex.getMessage());
        }
    }

    private static void runCli() throws IOException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("=== BUPT TA Recruitment Prototype ===");
            System.out.println("1) TA: Create/Update profile");
            System.out.println("2) TA: View open jobs");
            System.out.println("3) TA: Apply for job");
            System.out.println("4) TA: Check application status");
            System.out.println("5) MO: Post a job");
            System.out.println("6) MO: Review and select applicants");
            System.out.println("7) Admin: Dashboard and management");
            System.out.println("0) Exit");
            System.out.print("Select: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    taCreateOrUpdateProfile(scanner);
                    break;
                case "2":
                    taViewOpenJobs();
                    break;
                case "3":
                    taApply(scanner);
                    break;
                case "4":
                    taCheckStatus(scanner);
                    break;
                case "5":
                    moPostJob(scanner);
                    break;
                case "6":
                    moReviewApplications(scanner);
                    break;
                case "7":
                    adminMenu(scanner);
                    break;
                case "0":
                    System.out.println("Bye.");
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void taCreateOrUpdateProfile(Scanner scanner) throws IOException {
        List<Profile> profiles = readProfiles();
        System.out.print("TA id (e.g. TA001): ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            System.out.println("TA id cannot be empty.");
            return;
        }
        System.out.print("Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Skills (comma separated): ");
        String skillsRaw = scanner.nextLine().trim();
        List<String> skills = parseCsvList(skillsRaw);

        Profile existing = profiles.stream().filter(p -> p.id.equalsIgnoreCase(id)).findFirst().orElse(null);
        if (existing == null) {
            profiles.add(new Profile(id, name, "TA", skills, "ACTIVE"));
            System.out.println("Profile created.");
        } else {
            existing.name = name;
            existing.skills = skills;
            existing.role = "TA";
            if (existing.status == null || existing.status.isEmpty()) {
                existing.status = "ACTIVE";
            }
            System.out.println("Profile updated.");
        }
        writeProfiles(profiles);
    }

    private static void taViewOpenJobs() throws IOException {
        List<Job> jobs = readJobs().stream().filter(j -> "OPEN".equals(j.status)).collect(Collectors.toList());
        if (jobs.isEmpty()) {
            System.out.println("No open jobs.");
            return;
        }
        System.out.println("Open jobs:");
        for (Job j : jobs) {
            System.out.println(String.format("- %s | %s | %s | skills=%s | %d hours | by %s",
                    j.id, j.title, j.module, String.join(",", j.requiredSkills), j.hoursPerWeek, j.postedBy));
        }
    }

    private static void taApply(Scanner scanner) throws IOException {
        List<Job> jobs = readJobs();
        List<Application> apps = readApplications();
        System.out.print("TA id: ");
        String taId = scanner.nextLine().trim();
        if (!existsProfile(taId, "TA")) {
            System.out.println("TA profile not found. Please create profile first.");
            return;
        }
        taViewOpenJobs();
        System.out.print("Job id to apply: ");
        String jobId = scanner.nextLine().trim();
        Job target = jobs.stream().filter(j -> j.id.equalsIgnoreCase(jobId)).findFirst().orElse(null);
        if (target == null || !"OPEN".equals(target.status)) {
            System.out.println("Invalid or closed job.");
            return;
        }

        boolean duplicate = apps.stream().anyMatch(a -> a.jobId.equalsIgnoreCase(jobId) && a.taId.equalsIgnoreCase(taId));
        if (duplicate) {
            System.out.println("You have already applied this job.");
            return;
        }

        String nextId = nextId("APP", apps.size() + 1);
        apps.add(new Application(nextId, jobId, taId, "PENDING", ""));
        writeApplications(apps);
        System.out.println("Application submitted: " + nextId);
    }

    private static void taCheckStatus(Scanner scanner) throws IOException {
        List<Application> apps = readApplications();
        System.out.print("TA id: ");
        String taId = scanner.nextLine().trim();
        List<Application> mine = apps.stream().filter(a -> a.taId.equalsIgnoreCase(taId)).collect(Collectors.toList());
        if (mine.isEmpty()) {
            System.out.println("No applications found.");
            return;
        }
        for (Application a : mine) {
            System.out.println(String.format("- %s | job=%s | status=%s | note=%s",
                    a.id, a.jobId, a.status, a.note));
        }
    }

    private static void moPostJob(Scanner scanner) throws IOException {
        List<Job> jobs = readJobs();
        System.out.print("MO id (e.g. MO001): ");
        String moId = scanner.nextLine().trim();
        if (!existsProfile(moId, "MO")) {
            System.out.print("MO profile not found, create now? (y/n): ");
            if (!"y".equalsIgnoreCase(scanner.nextLine().trim())) {
                return;
            }
            List<Profile> profiles = readProfiles();
            System.out.print("MO name: ");
            String moName = scanner.nextLine().trim();
            profiles.add(new Profile(moId, moName, "MO", new ArrayList<>(), "ACTIVE"));
            writeProfiles(profiles);
        }
        System.out.print("Job title: ");
        String title = scanner.nextLine().trim();
        System.out.print("Module/activity: ");
        String module = scanner.nextLine().trim();
        System.out.print("Required skills (comma separated): ");
        List<String> skills = parseCsvList(scanner.nextLine().trim());
        System.out.print("Hours per week: ");
        int hours = parseIntDefault(scanner.nextLine().trim(), 2);

        String id = nextId("JOB", jobs.size() + 1);
        jobs.add(new Job(id, title, module, skills, hours, "OPEN", moId));
        writeJobs(jobs);
        System.out.println("Job posted: " + id);
    }

    private static void moReviewApplications(Scanner scanner) throws IOException {
        List<Job> jobs = readJobs();
        List<Application> apps = readApplications();
        System.out.print("MO id: ");
        String moId = scanner.nextLine().trim();

        List<Job> myJobs = jobs.stream().filter(j -> j.postedBy.equalsIgnoreCase(moId)).collect(Collectors.toList());
        if (myJobs.isEmpty()) {
            System.out.println("No jobs posted by this MO.");
            return;
        }
        System.out.println("Your jobs:");
        for (Job j : myJobs) {
            System.out.println("- " + j.id + " | " + j.title + " | " + j.status);
        }
        System.out.print("Choose job id: ");
        String jobId = scanner.nextLine().trim();
        Job target = myJobs.stream().filter(j -> j.id.equalsIgnoreCase(jobId)).findFirst().orElse(null);
        if (target == null) {
            System.out.println("Invalid job id.");
            return;
        }
        List<Application> related = apps.stream().filter(a -> a.jobId.equalsIgnoreCase(jobId)).collect(Collectors.toList());
        if (related.isEmpty()) {
            System.out.println("No applicants yet.");
            return;
        }
        for (Application a : related) {
            System.out.println("- " + a.id + " | ta=" + a.taId + " | " + a.status);
        }
        System.out.print("Application id to accept: ");
        String appId = scanner.nextLine().trim();
        Application selected = related.stream().filter(a -> a.id.equalsIgnoreCase(appId)).findFirst().orElse(null);
        if (selected == null) {
            System.out.println("Invalid application id.");
            return;
        }

        for (Application a : apps) {
            if (a.jobId.equalsIgnoreCase(jobId)) {
                if (a.id.equalsIgnoreCase(appId)) {
                    a.status = "ACCEPTED";
                    a.note = "Selected by MO " + moId;
                } else if ("PENDING".equals(a.status)) {
                    a.status = "REJECTED";
                    a.note = "Another candidate selected";
                }
            }
        }
        target.status = "CLOSED";
        writeApplications(apps);
        writeJobs(jobs);
        System.out.println("Selection completed, job closed.");
    }

    private static void adminMenu(Scanner scanner) throws IOException {
        while (true) {
            System.out.println();
            System.out.println("=== Admin Menu ===");
            System.out.println("1) AD_01 View TA workload dashboard");
            System.out.println("2) AD_02 Manage system users");
            System.out.println("3) AD_03 Oversee job postings");
            System.out.println("4) AD_04 View basic application stats");
            System.out.println("0) Back");
            System.out.print("Select: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    adminWorkload(scanner);
                    break;
                case "2":
                    adminManageUsers(scanner);
                    break;
                case "3":
                    adminOverseeJobs(scanner);
                    break;
                case "4":
                    adminBasicStats();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void adminWorkload(Scanner scanner) throws IOException {
        List<Application> apps = readApplications();
        List<Job> jobs = readJobs();
        List<Profile> tas = readProfiles().stream()
                .filter(p -> "TA".equalsIgnoreCase(p.role))
                .collect(Collectors.toList());
        if (tas.isEmpty()) {
            System.out.println("No TA profiles.");
            return;
        }

        List<WorkloadRow> rows = new ArrayList<>();
        for (Profile ta : tas) {
            List<Application> accepted = apps.stream()
                    .filter(a -> a.taId.equalsIgnoreCase(ta.id) && "ACCEPTED".equals(a.status))
                    .collect(Collectors.toList());
            int hours = 0;
            for (Application a : accepted) {
                Job j = jobs.stream().filter(job -> job.id.equalsIgnoreCase(a.jobId)).findFirst().orElse(null);
                if (j != null) {
                    hours += j.hoursPerWeek;
                }
            }
            rows.add(new WorkloadRow(ta.id, ta.name, accepted.size(), hours));
        }

        System.out.print("Sort by (1=name, 2=hours desc): ");
        String sortChoice = scanner.nextLine().trim();
        if ("2".equals(sortChoice)) {
            rows.sort(Comparator.comparingInt((WorkloadRow r) -> r.totalHours).reversed());
        } else {
            rows.sort(Comparator.comparing(r -> r.taId.toUpperCase()));
        }

        System.out.print("Filter by TA id (press Enter for all): ");
        String idFilter = scanner.nextLine().trim();
        if (!idFilter.isEmpty()) {
            rows = rows.stream()
                    .filter(r -> r.taId.toUpperCase().contains(idFilter.toUpperCase()))
                    .collect(Collectors.toList());
        }

        System.out.print("Overload threshold hours/week (default 8): ");
        int threshold = parseIntDefault(scanner.nextLine().trim(), 8);

        System.out.println("TA workload dashboard:");
        for (WorkloadRow row : rows) {
            String tag = row.totalHours > threshold ? " [OVERLOAD]" : "";
            System.out.println("- " + row.taId + " | " + row.name + " | accepted jobs: " + row.acceptedCount
                    + " | total hours/week: " + row.totalHours + tag);
        }
    }

    private static void adminManageUsers(Scanner scanner) throws IOException {
        List<Profile> profiles = readProfiles();
        System.out.println("Users:");
        for (Profile p : profiles) {
            System.out.println("- " + p.id + " | " + p.name + " | role=" + p.role + " | status=" + p.status);
        }
        System.out.println("Actions: c=create/update, a=activate, d=deactivate, r=assign role, x=delete, q=quit");
        System.out.print("Action: ");
        String action = scanner.nextLine().trim().toLowerCase();

        if ("q".equals(action)) {
            return;
        }
        System.out.print("User id: ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            System.out.println("User id cannot be empty.");
            return;
        }

        Profile target = profiles.stream().filter(p -> p.id.equalsIgnoreCase(id)).findFirst().orElse(null);
        switch (action) {
            case "c":
                System.out.print("Name: ");
                String name = scanner.nextLine().trim();
                System.out.print("Role (TA/MO/ADMIN): ");
                String role = normalizeRole(scanner.nextLine().trim());
                if (target == null) {
                    profiles.add(new Profile(id, name, role, parseCsvList(""), "ACTIVE"));
                    System.out.println("User created.");
                } else {
                    target.name = name;
                    target.role = role;
                    if (target.status == null || target.status.isEmpty()) {
                        target.status = "ACTIVE";
                    }
                    System.out.println("User updated.");
                }
                break;
            case "a":
                if (target == null) {
                    System.out.println("User not found.");
                    return;
                }
                target.status = "ACTIVE";
                System.out.println("User activated.");
                break;
            case "d":
                if (target == null) {
                    System.out.println("User not found.");
                    return;
                }
                target.status = "INACTIVE";
                System.out.println("User deactivated.");
                break;
            case "r":
                if (target == null) {
                    System.out.println("User not found.");
                    return;
                }
                System.out.print("New role (TA/MO/ADMIN): ");
                target.role = normalizeRole(scanner.nextLine().trim());
                System.out.println("Role updated.");
                break;
            case "x":
                if (target == null) {
                    System.out.println("User not found.");
                    return;
                }
                profiles.remove(target);
                System.out.println("User deleted.");
                break;
            default:
                System.out.println("Unsupported action.");
                return;
        }
        writeProfiles(profiles);
    }

    private static void adminOverseeJobs(Scanner scanner) throws IOException {
        List<Job> jobs = readJobs();
        if (jobs.isEmpty()) {
            System.out.println("No jobs.");
            return;
        }
        System.out.println("All jobs:");
        for (Job j : jobs) {
            System.out.println("- " + j.id + " | " + j.title + " | " + j.module + " | status=" + j.status + " | by " + j.postedBy);
        }
        System.out.println("Actions: e=edit, x=remove, q=quit");
        System.out.print("Action: ");
        String action = scanner.nextLine().trim().toLowerCase();
        if ("q".equals(action)) {
            return;
        }
        System.out.print("Job id: ");
        String jobId = scanner.nextLine().trim();
        Job target = jobs.stream().filter(j -> j.id.equalsIgnoreCase(jobId)).findFirst().orElse(null);
        if (target == null) {
            System.out.println("Job not found.");
            return;
        }

        if ("e".equals(action)) {
            System.out.print("New title (Enter to keep): ");
            String title = scanner.nextLine().trim();
            System.out.print("New module/activity (Enter to keep): ");
            String module = scanner.nextLine().trim();
            System.out.print("New hours per week (Enter to keep): ");
            String hoursRaw = scanner.nextLine().trim();
            System.out.print("New status OPEN/CLOSED (Enter to keep): ");
            String status = scanner.nextLine().trim().toUpperCase();

            if (!title.isEmpty()) {
                target.title = title;
            }
            if (!module.isEmpty()) {
                target.module = module;
            }
            if (!hoursRaw.isEmpty()) {
                target.hoursPerWeek = parseIntDefault(hoursRaw, target.hoursPerWeek);
            }
            if ("OPEN".equals(status) || "CLOSED".equals(status)) {
                target.status = status;
            }
            writeJobs(jobs);
            System.out.println("Job updated.");
        } else if ("x".equals(action)) {
            jobs.remove(target);
            writeJobs(jobs);
            System.out.println("Job removed.");
        } else {
            System.out.println("Unsupported action.");
        }
    }

    private static void adminBasicStats() throws IOException {
        List<Job> jobs = readJobs();
        List<Application> apps = readApplications();
        long open = jobs.stream().filter(j -> "OPEN".equalsIgnoreCase(j.status)).count();
        long closed = jobs.stream().filter(j -> "CLOSED".equalsIgnoreCase(j.status)).count();
        long pending = apps.stream().filter(a -> "PENDING".equalsIgnoreCase(a.status)).count();
        long accepted = apps.stream().filter(a -> "ACCEPTED".equalsIgnoreCase(a.status)).count();
        long rejected = apps.stream().filter(a -> "REJECTED".equalsIgnoreCase(a.status)).count();

        System.out.println("Basic application stats:");
        System.out.println("- Total applications: " + apps.size());
        System.out.println("- Open positions: " + open);
        System.out.println("- Closed positions: " + closed);
        System.out.println("- Pending applications: " + pending);
        System.out.println("- Accepted applications: " + accepted);
        System.out.println("- Rejected applications: " + rejected);
    }

    private static boolean existsProfile(String id, String role) throws IOException {
        return readProfiles().stream().anyMatch(p -> p.id.equalsIgnoreCase(id)
                && role.equalsIgnoreCase(p.role)
                && "ACTIVE".equalsIgnoreCase(p.status));
    }

    private static String normalizeRole(String roleRaw) {
        String role = roleRaw == null ? "" : roleRaw.trim().toUpperCase();
        if ("PROFESSOR".equals(role)) {
            return "MO";
        }
        if ("TA".equals(role) || "MO".equals(role) || "ADMIN".equals(role)) {
            return role;
        }
        return "TA";
    }

    private static void bootstrapFiles() throws IOException {
        if (!Files.exists(DATA_DIR)) {
            Files.createDirectories(DATA_DIR);
        }
        if (!Files.exists(PROFILES_FILE)) {
            Files.write(PROFILES_FILE, Arrays.asList(
                    "TA001|Alice|TA|Java,Communication|ACTIVE",
                    "MO001|Dr.Wang|MO||ACTIVE",
                    "AD001|Admin|ADMIN||ACTIVE"), StandardCharsets.UTF_8);
        }
        if (!Files.exists(JOBS_FILE)) {
            Files.write(JOBS_FILE, Arrays.asList("JOB001|Java Lab TA|OOP Module|Java,Debugging|4|OPEN|MO001"), StandardCharsets.UTF_8);
        }
        if (!Files.exists(APPLICATIONS_FILE)) {
            Files.write(APPLICATIONS_FILE, new ArrayList<>(), StandardCharsets.UTF_8);
        }
    }

    private static List<Profile> readProfiles() throws IOException {
        List<Profile> result = new ArrayList<>();
        for (String line : Files.readAllLines(PROFILES_FILE, StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] p = line.split("\\|", -1);
            if (p.length < 4) {
                continue;
            }
            String status = p.length >= 5 && !p[4].trim().isEmpty() ? p[4].trim() : "ACTIVE";
            result.add(new Profile(p[0], p[1], p[2], parseCsvList(p[3]), status));
        }
        return result;
    }

    private static void writeProfiles(List<Profile> profiles) throws IOException {
        List<String> lines = profiles.stream()
                .map(p -> String.join("|", p.id, p.name, p.role, String.join(",", p.skills), p.status))
                .collect(Collectors.toList());
        Files.write(PROFILES_FILE, lines, StandardCharsets.UTF_8);
    }

    private static List<Job> readJobs() throws IOException {
        List<Job> result = new ArrayList<>();
        for (String line : Files.readAllLines(JOBS_FILE, StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] p = line.split("\\|", -1);
            if (p.length < 7) {
                continue;
            }
            result.add(new Job(p[0], p[1], p[2], parseCsvList(p[3]), parseIntDefault(p[4], 0), p[5], p[6]));
        }
        return result;
    }

    private static void writeJobs(List<Job> jobs) throws IOException {
        List<String> lines = jobs.stream()
                .map(j -> String.join("|",
                        j.id,
                        j.title,
                        j.module,
                        String.join(",", j.requiredSkills),
                        String.valueOf(j.hoursPerWeek),
                        j.status,
                        j.postedBy))
                .collect(Collectors.toList());
        Files.write(JOBS_FILE, lines, StandardCharsets.UTF_8);
    }

    private static List<Application> readApplications() throws IOException {
        List<Application> result = new ArrayList<>();
        for (String line : Files.readAllLines(APPLICATIONS_FILE, StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] p = line.split("\\|", -1);
            if (p.length < 5) {
                continue;
            }
            result.add(new Application(p[0], p[1], p[2], p[3], p[4]));
        }
        return result;
    }

    private static void writeApplications(List<Application> apps) throws IOException {
        List<String> lines = apps.stream()
                .map(a -> String.join("|", a.id, a.jobId, a.taId, a.status, a.note))
                .collect(Collectors.toList());
        Files.write(APPLICATIONS_FILE, lines, StandardCharsets.UTF_8);
    }

    private static List<String> parseCsvList(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static int parseIntDefault(String raw, int defaultValue) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static String nextId(String prefix, int value) {
        return String.format("%s%03d", prefix, value);
    }

    private static class Profile {
        String id;
        String name;
        String role;
        List<String> skills;
        String status;

        Profile(String id, String name, String role, List<String> skills, String status) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.skills = skills;
            this.status = status;
        }
    }

    private static class Job {
        String id;
        String title;
        String module;
        List<String> requiredSkills;
        int hoursPerWeek;
        String status;
        String postedBy;

        Job(String id, String title, String module, List<String> requiredSkills, int hoursPerWeek, String status, String postedBy) {
            this.id = id;
            this.title = title;
            this.module = module;
            this.requiredSkills = requiredSkills;
            this.hoursPerWeek = hoursPerWeek;
            this.status = status;
            this.postedBy = postedBy;
        }
    }

    private static class Application {
        String id;
        String jobId;
        String taId;
        String status;
        String note;

        Application(String id, String jobId, String taId, String status, String note) {
            this.id = id;
            this.jobId = jobId;
            this.taId = taId;
            this.status = status;
            this.note = note;
        }
    }

    private static class WorkloadRow {
        String taId;
        String name;
        int acceptedCount;
        int totalHours;

        WorkloadRow(String taId, String name, int acceptedCount, int totalHours) {
            this.taId = taId;
            this.name = name;
            this.acceptedCount = acceptedCount;
            this.totalHours = totalHours;
        }
    }
}
