# BUPT International School — TA Recruitment System (Test_Version_02)

Web application for Teaching Assistant (TA) recruitment at BUPT International School. TAs browse vacancies and submit applications; Module Organisers (MOs) post jobs and review applicants; Administrators manage users, workload, password resets, audit logs, and system settings.

Built for **EBU6304** coursework: **Java only**, **no database** (JSON file storage), **Agile** delivery.

This README provides instructions for **setting up**, **configuring**, and **running** the software.

---

## 1. Setting Up

### 1.1 Requirements

| Item | Details |
|------|---------|
| **JDK** | Java **11 or later** (JDK required, not JRE only) |
| **OS** | Windows recommended (`.\build.bat` / `.\run.bat` in PowerShell) |
| **Network** | Internet access on first build (Gson is downloaded automatically if missing) |
| **Browser** | Modern browser (Chrome, Edge, Firefox) for the HTML/JS frontends |

Verify Java is installed:

```bat
java -version
javac -version
```

### 1.2 Get the Project

Clone or download this repository, then open a terminal in the project root:

```text
Software_Group_54/Test_Version_02/
```

All commands below must be run from this folder. The server uses the current working directory as the project root for `data/` and `uploads/`.

### 1.3 Dependencies

The project uses one external library:

| Library | Version | Location |
|---------|---------|----------|
| Gson | 2.10.1 | `lib/gson-2.10.1.jar` |

On first build, `build.bat` downloads Gson from Maven Central if the JAR is not present.

Optional (for unit tests only):

| Library | Version | Location |
|---------|---------|----------|
| JUnit Platform Console | 1.10.2 | `lib/junit-platform-console-standalone-1.10.2.jar` |

No Maven, Gradle, Node.js, or database installation is required.

### 1.4 Build

**Windows — PowerShell (recommended):**

```powershell
.\build.bat
```

In PowerShell, scripts in the current folder must be invoked with the `.\` prefix. In **CMD**, you can also run `build.bat` without the prefix.

This compiles Java sources into `out/` and creates `data/` and `uploads/` at runtime when the server starts.

**macOS / Linux (manual equivalent):**

```bash
mkdir -p out lib
# Download gson-2.10.1.jar into lib/ if missing
javac -encoding UTF-8 -cp "lib/gson-2.10.1.jar" -d out \
  src/main/java/com/bupt/tarecruit/model/*.java \
  src/main/java/com/bupt/tarecruit/service/*.java \
  src/main/java/com/bupt/tarecruit/handler/*.java \
  src/main/java/com/bupt/tarecruit/Main.java
```

If the build fails, confirm `javac` is on your PATH and rerun the build command.

---

## 2. Configuring

### 2.1 Server Port

Default HTTP port is **8080**. Change it when starting the server:

```powershell
.\run.bat 9090
```

Or run Java directly:

```bat
java -cp "out;lib\gson-2.10.1.jar" com.bupt.tarecruit.Main 9090
```

On macOS/Linux, use `:` instead of `;` in the classpath.

### 2.2 System Settings (`data/settings.json`)

Runtime configuration is stored in `data/settings.json`. The file is read on startup and can be updated through the Admin portal or by editing the file while the server is stopped.

Common settings:

| Key | Purpose | Default / notes |
|-----|---------|-----------------|
| `maxWeeklyHours` | Maximum approved TA weekly hours before workload is flagged | `20` |
| `recruitmentStart` | Recruitment window start (Unix timestamp in ms) | Optional |
| `recruitmentEnd` | Recruitment window end (Unix timestamp in ms) | Optional |
| `defaultResetPassword` | Password assigned when admin approves a reset request | `123456` |
| `emailTemplatesJson` | JSON array of password-reset email templates | See Admin UI |

**Admin UI:** log in as Admin → **System Settings** (`/admin/system-settings.html`) → adjust recruitment period, max weekly hours, SMTP, and email templates → **Save All Changes**.

### 2.3 Email (SMTP) for Password-Reset Notifications

When an administrator approves or rejects a password-reset request, the system can send notification email via SMTP. Mail is **optional**; core features work without it.

Configure using **either** environment variables **or** `data/settings.json` keys (server restart or Admin save required):

| Environment variable | Settings key | Description |
|---------------------|--------------|-------------|
| `MAIL_SMTP_HOST` | `mail.smtp.host` | SMTP server hostname |
| `MAIL_SMTP_PORT` | `mail.smtp.port` | SMTP port (e.g. `465` for SSL) |
| `MAIL_SMTP_USERNAME` | `mail.smtp.username` | SMTP login username |
| `MAIL_SMTP_PASSWORD` | `mail.smtp.password` | SMTP auth code / password |
| `MAIL_SMTP_FROM` | `mail.smtp.from` | Sender email address |
| `MAIL_SMTP_FROM_NAME` | `mail.smtp.fromName` | Sender display name |

Example (`settings.json` fragment — replace with your own credentials):

```json
{
  "mail.smtp.host": "smtp.example.com",
  "mail.smtp.port": "465",
  "mail.smtp.username": "your-account@example.com",
  "mail.smtp.password": "your-smtp-auth-code",
  "mail.smtp.from": "your-account@example.com",
  "mail.smtp.fromName": "TA Recruit Support"
}
```

Do **not** commit real SMTP passwords to version control.

### 2.4 Data and Upload Directories

| Path | Purpose |
|------|---------|
| `data/` | JSON persistence (users, jobs, applications, drafts, notifications, audit logs, settings) |
| `uploads/` | Uploaded CVs and other files |

Both directories are created automatically on first run. To reset the environment to a clean state:

1. Stop the server (`Ctrl+C`).
2. Delete the `data/` and `uploads/` folders (or rename them as backup).
3. Restart the server — default admin and demo seed data will be recreated.

---

## 3. Running the Software

### 3.1 Start the Server

**Windows — PowerShell:**

```powershell
.\run.bat
```

Custom port:

```powershell
.\run.bat 9090
```

**macOS / Linux:**

```bash
java -cp "out:lib/gson-2.10.1.jar" com.bupt.tarecruit.Main
```

Expected console output:

```text
  TA Recruitment System v2
  Running at http://localhost:8080
  Default admin: admin / admin123
```

Stop the server with **`Ctrl+C`**.

### 3.2 Open the Application

After the server starts, open these URLs in a browser (replace `8080` if you used a custom port):

| Role | Login page |
|------|------------|
| **TA** (applicant) | http://localhost:8080/TA/index.html |
| **MO** (module organiser) | http://localhost:8080/MO/index.html |
| **Admin** | http://localhost:8080/admin/index.html |

The backend serves static HTML from `TA/`, `MO/`, and `admin/`, and REST APIs under `/api/`.

### 3.3 Default Accounts

On first run, `data/users.json` is created with a default administrator and seeded demo accounts.

| Role | Login ID | Password | Notes |
|------|----------|----------|-------|
| **Admin** | `admin` | `admin123` | Full system access |
| **TA** | Student ID (see below) | `123456` | Or use `{studentId}@bupt.edu.cn` |
| **MO** | `teacher01` … `teacher06` | `123456` | Or use `{teacherId}@bupt.edu.cn` |

Seeded TA student IDs (password `123456` for all):

| Name | Student ID |
|------|------------|
| Zijie Zhang | `231225731` |
| Zijun Song | `231225270` |
| Siying Li | `231225672` |
| Lingxiang Mei | `231225557` |
| Lechen Ning | `231225339` |
| Zhenkun Li | `231225649` |

New TA and MO accounts can also be registered from their respective login pages.

### 3.4 Typical Workflow

1. **MO** logs in → creates or manages job postings → reviews and approves/rejects applications.
2. **TA** logs in → browses open jobs → submits application (with optional CV upload and draft save).
3. **Admin** logs in → monitors workload and statistics → manages users → processes password-reset requests → exports data.

Key management pages auto-refresh periodically when the browser tab is visible, so multi-user demos update without manual reload.

### 3.5 Run Unit Tests (Optional)

Unit tests use JUnit 5 and do not require the HTTP server to be running. Full commands and expected output (`103 tests successful`) are documented in [`UNIT_TEST_EVIDENCE.md`](UNIT_TEST_EVIDENCE.md).

Quick summary from the project root (PowerShell on Windows):

```powershell
javac -encoding UTF-8 -cp "lib\gson-2.10.1.jar" -d out src\main\java\com\bupt\tarecruit\model\*.java src\main\java\com\bupt\tarecruit\service\*.java src\main\java\com\bupt\tarecruit\handler\*.java src\main\java\com\bupt\tarecruit\Main.java

javac -encoding UTF-8 -cp "lib\gson-2.10.1.jar;lib\junit-platform-console-standalone-1.10.2.jar;out" -d out src\main\java\com\bupt\tarecruit\unit_testing\*.java

java -jar lib\junit-platform-console-standalone-1.10.2.jar --class-path "out;lib\gson-2.10.1.jar" --scan-class-path
```

---

## 4. Project Layout

```text
Test_Version_02/
├── build.bat / run.bat          # Build and run scripts (Windows)
├── lib/                         # gson-2.10.1.jar (+ JUnit jar for tests)
├── out/                         # Compiled Java classes (created by build)
├── data/                        # JSON data files (runtime)
├── uploads/                     # Uploaded files (runtime)
├── src/main/java/com/bupt/tarecruit/
│   ├── Main.java                # HTTP server entry point
│   ├── model/                   # Data models
│   ├── service/                 # DataService, MailService, AiMatchingService
│   ├── handler/                 # REST API handlers
│   └── unit_testing/            # JUnit 5 tests
├── TA/                          # TA applicant frontend
├── MO/                          # Module Organiser frontend
├── admin/                       # Administrator frontend
└── js/api.js                    # Shared frontend API helper
```

---

## 5. Troubleshooting

| Problem | Solution |
|---------|----------|
| **Port already in use** | Start on another port: `.\run.bat 9090` |
| **`build.bat` / `run.bat` not recognized** | In PowerShell, use `.\build.bat` and `.\run.bat` (include the `.\` prefix) |
| **404 / page not found** | Ensure you run commands from `Test_Version_02/` root and the server is started |
| **Build failed** | Check `java -version` and `javac -version`; rerun `.\build.bat` |
| **Gson download failed** | Manually download `gson-2.10.1.jar` into `lib/` from [Maven Central](https://repo1.maven.org/maven2/com/google/gson/gson/2.10.1/gson-2.10.1.jar) |
| **Login fails for seeded accounts** | Use student ID / teacher ID or email; default password is `123456` (admin: `admin123`) |
| **Password-reset email not sent** | Configure SMTP (Section 2.3); approval still works without email |
| **Stale demo data** | Stop server, delete `data/` and `uploads/`, restart |
| **Save / login fails after server restart** | Login sessions are in-memory; log in again at the correct portal (TA / MO / Admin) |
| **Job detail shows updates but Edit page looks old** | Hard-refresh the edit page (`Ctrl+F5`), or open **Edit Job** again from the detail page |
| **Wrong port in browser** | If you started with `.\run.bat 9090`, use `http://localhost:9090/...` in the browser |

---

## 6. Course Alignment (EBU6304)

- Standalone Java with embedded `com.sun.net.httpserver.HttpServer` (no Spring Boot).
- File-based JSON storage (no SQL database).
- Three role-based web portals (TA, MO, Admin) with REST API backend.
- Supports iterative Agile delivery with traceable user stories.

For module-specific UI notes, see [`TA/README.md`](TA/README.md), [`MO/README.md`](MO/README.md), and [`admin/README.md`](admin/README.md).
