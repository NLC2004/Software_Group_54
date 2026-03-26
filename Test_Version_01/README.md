# BUPT International School — TA Recruitment System

Standalone Java web application for recruiting Teaching Assistants (TAs): applicants browse and apply, Module Organisers (MOs) post positions and review applications, and Admins oversee users, postings, and TA workload.

Designed for **EBU6304** coursework constraints: **Java only**, **no database** (JSON/text files), **Agile** delivery.

---

## Requirements

- **JDK 17+** (tested with OpenJDK 24)
- **Windows**: use `build.bat` / `run.bat` as below  
- **macOS / Linux**: same steps with `javac`/`java` and classpath separator `:` instead of `;`

---

## Quick start

1. Open a terminal in this folder (`NLC_test`).

2. **Build** (downloads Gson if missing, compiles to `out/`):

   ```bat
   build.bat
   ```

3. **Run** (starts HTTP server, default port **8080**):

   ```bat
   run.bat
   ```

   Or with a custom port:

   ```bat
   run.bat 9090
   ```

4. Open a browser: **http://localhost:8080** (or your port).  
   The app may try to open the browser automatically on Windows.

5. **Stop**: press `Ctrl+C` in the terminal where the server is running.

---

## Default account

| Username | Password | Role  |
|----------|----------|-------|
| `admin`  | `admin123` | Admin |

On first run, `data/users.json` is created with this admin user. **Change the password** before any real deployment.

---

## Roles and typical flow

| Role | What they do |
|------|----------------|
| **TA** | Register/login, edit profile, browse open jobs (course vs activity), upload CV and apply, track status, withdraw pending applications. |
| **MO** | Register/login, post jobs (course TA or activity e.g. invigilation), review applicants, approve/reject, open/close postings. |
| **Admin** | Dashboard stats, manage users (activate/deactivate, reset password), view/remove any job, workload view (weekly hours vs limit), recruitment settings (dates, max hours). |

**Suggested demo path:** register one MO and one TA → MO posts a job → TA applies → MO reviews → Admin checks workload.

---

## Data and uploads

All persistence is under the **current working directory** when you start the server (normally the project root):

| Path | Purpose |
|------|---------|
| `data/users.json` | User accounts |
| `data/jobs.json` | Job postings |
| `data/applications.json` | Applications |
| `data/settings.json` | Admin settings (optional; created when saved) |
| `uploads/` | Uploaded CV files |

To **reset** the system, stop the server and delete `data/` and `uploads/` (a fresh admin will be recreated on next start).

---

## Project layout

```
NLC_test/
├── build.bat / run.bat     # Windows build & run
├── lib/gson-2.10.1.jar     # JSON (downloaded by build.bat)
├── out/                    # Compiled .class files
├── data/                   # Runtime JSON data (gitignored recommended)
├── uploads/                # Uploaded files
├── src/main/java/...       # Java source (HttpServer + REST-style API)
└── src/main/resources/static/  # Web UI (HTML, CSS, JS)
```

---

## API (for debugging)

Base URL: `http://localhost:<port>/api`

- `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/me`, `PUT /api/auth/profile`, …
- `GET/POST /api/jobs`, `POST /api/jobs/{id}/apply`, …
- `GET /api/applications`, `PUT /api/applications/{id}/status`
- `GET/PUT /api/admin/...` (Admin only)
- `POST /api/upload` — include header `Authorization: Bearer <token>` for protected routes

Static pages: `/`, `/login.html`, `/dashboard.html`, `/css/style.css`, `/js/app.js`.

---

## Troubleshooting

- **Port in use**: another process is using 8080 — close it or run `run.bat 9090`.
- **Blank page / 404**: run from the **project root** so `src/main/resources/static` exists relative to `user.dir`.
- **Build fails**: ensure `javac` is on `PATH` and `lib/gson-2.10.1.jar` exists (re-run `build.bat`).

---

## Course alignment (EBU6304)

- Implementation: **standalone Java** with embedded HTTP server (not Spring Boot).
- Storage: **JSON files** — no SQL database.
- Optional “AI” features from the handout (e.g. skill matching) are **not required**; if added, document them clearly and keep outputs explainable.

---

## Licence / course use

Course group project — use and modify according to your module rules.
