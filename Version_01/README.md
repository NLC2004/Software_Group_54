# BUPT International School — TA Recruitment System (Version_01)

Standalone Java web application for recruiting Teaching Assistants (TAs): applicants browse and apply, Module Organisers (MOs) post positions and review applications, and Admins oversee users, postings, reset requests, and TA workload.

Designed for **EBU6304** coursework constraints: **Java only**, **no database** (JSON/text files), **Agile** delivery.

---

## Requirements

- **JDK 17+** (tested with OpenJDK 24)
- **Windows**: use `build.bat` / `run.bat`
- **macOS / Linux**: use `javac`/`java` manually (`:` classpath separator)

---

## Quick start

1. Open terminal in `Version_01/`.

2. **Build** (downloads Gson if missing, compiles to `out/`):

```bat
build.bat
```

3. **Run** (starts HTTP server, default port **8080**):

```bat
run.bat
```

Or custom port:

```bat
run.bat 9090
```

4. Open browser: **http://localhost:8080** (or your custom port).

5. Stop server with `Ctrl+C`.

---

## Default account

| Username | Password   | Role  |
|----------|------------|-------|
| `admin`  | `admin123` | ADMIN |

On first run, `data/users.json` is auto-created with this admin.

---

## Main features by role

### TA
- Register/login
- Maintain personal profile
- Browse OPEN jobs (Course TA / Activity)
- Apply with optional CV upload
- View application status
- Withdraw **pending** applications only
- Account security page:
  - Change password (logged-in)
  - Submit forgot-password request for admin review

### MO
- Post job vacancies
- Review applications for own postings
- Approve / reject **pending** applications
- Quota protection: cannot approve beyond job `quota`
- When approved count reaches quota, job auto-switches to `CLOSED`

### ADMIN
- Dashboard statistics
- User management (activate/deactivate, reset password to `123456`)
- Review password-reset requests (approve/reject)
- Audit log view for admin actions
- View/remove all jobs
- TA workload view (weekly hours vs configured max)
- Recruitment settings (start/end date + max weekly hours)

---

## Real-time UI behavior

For key management pages, frontend auto-refreshes every ~8 seconds (when tab is visible):

- `MO -> Review Applicants`
- `ADMIN -> Reset Requests`
- `ADMIN -> Workload`

This helps demo multi-user updates without manual refresh.

---

## Data files and uploads

All persistence is local file storage under project runtime directory:

| Path | Purpose |
|------|---------|
| `data/users.json` | User accounts |
| `data/jobs.json` | Job postings |
| `data/applications.json` | Applications |
| `data/password_reset_requests.json` | Forgot-password requests |
| `data/audit_logs.json` | Admin action audit records |
| `data/settings.json` | Recruitment settings |
| `uploads/` | Uploaded CV files |

To reset environment: stop server and delete `data/` + `uploads/`.

---

## Project layout

```text
Version_01/
├── build.bat / run.bat
├── lib/gson-2.10.1.jar
├── out/
├── data/
├── uploads/
├── src/main/java/com/bupt/tarecruit/
│   ├── Main.java
│   ├── handler/
│   ├── model/
│   └── service/
└── src/main/resources/static/
    ├── login.html
    ├── dashboard.html
    ├── css/style.css
    └── js/app.js
```

---

## API summary (debug use)

Base URL: `http://localhost:<port>/api`

- Auth:
  - `POST /auth/login`
  - `POST /auth/register`
  - `GET /auth/me`
  - `PUT /auth/profile`
  - `PUT /auth/password`
  - `POST /auth/forgot-password`
- Jobs:
  - `GET/POST /jobs`
  - `GET/PUT/DELETE /jobs/{id}`
  - `POST /jobs/{id}/apply`
- Applications:
  - `GET /applications`
  - `PUT /applications/{id}/status`
- Admin:
  - `GET /admin/stats`
  - `GET /admin/users`, `PUT /admin/users/{id}`, `DELETE /admin/users/{id}`
  - `GET /admin/reset-requests`
  - `PUT /admin/reset-requests/{id}/review`
  - `GET /admin/audit-logs`
  - `GET /admin/workload`
  - `GET/PUT /admin/settings`
- Upload:
  - `POST /upload`
  - `GET /upload/{fileName}`

Static pages: `/`, `/login.html`, `/dashboard.html`.

---

## Troubleshooting

- **Port conflict**: use another port (`run.bat 9090`).
- **Page/resource not found**: ensure running from `Version_01` root.
- **Build error**: check `javac` in PATH, then rerun `build.bat`.
- **Chinese garbled output in cmd**: this does not affect compile result; use PowerShell/UTF-8 terminal if needed.

---

## Course alignment (EBU6304)

- Standalone Java + embedded `HttpServer` (no Spring Boot).
- File-based JSON storage (no SQL DB).
- Supports iterative delivery and traceable feature increments.

---

## Note

This repository includes generated/runtime files (`out/`, `data/`) in current workspace status. For cleaner submissions, consider `.gitignore` rules for build artifacts and local runtime data.