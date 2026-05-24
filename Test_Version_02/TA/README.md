# TA Portal — Teaching Assistant Applicant

This folder contains the **Teaching Assistant (TA)** applicant portal for the BUPT International School TA Recruitment System (`Test_Version_02`). TAs browse open vacancies, manage their profile, submit applications (with optional CV upload), save drafts, and track application status.

All pages are served by the Java backend together with the REST API. **Do not use a standalone static server** — browsing jobs, applying, and saving drafts require the main application server.

---

## Prerequisites

1. Build and start the server from the project root (`Test_Version_02/`). See the main [README](../README.md).
2. Open the TA login page:

   `http://localhost:8080/TA/index.html`

3. Log in with a seeded TA account (student ID or `{studentId}@bupt.edu.cn`, password `123456`).

Shared frontend utilities: [`../js/api.js`](../js/api.js).

---

## Pages

Start from `index.html`.

| Page | Purpose |
|------|---------|
| `index.html` | TA login and registration |
| `forgot-password.html` | Submit a password-reset request (admin approval required) |
| `dashboard.html` | Overview, quick links, deadline reminders |
| `personal-information.html` | Profile, password change |
| `notifications.html` | System notifications and deadline reminders |
| `ta-recruitment-list.html` | Browse open TA vacancies |
| `ta-recruitment-detail.html` | Vacancy detail |
| `ta-recruitment-application-form.html` | Application form, draft save, CV upload, AI match hint |
| `application-success.html` | Confirmation after successful submission |
| `my-ta-applications.html` | List of the TA's applications |
| `application-review-detail.html` | Single application detail |

---

## Key Behaviour (API-backed)

### Authentication and profile

- Login/register via `/api/auth/login` and `/api/auth/register` with `portalRole: 'TA'`.
- Profile read/update via `/api/auth/me` and `/api/auth/profile`.
- Password change via `/api/auth/password`.

### Applications

- Apply to a job: `POST /api/jobs/{jobId}/apply` with cover letter and priority (1–3).
- At most **three active applications**; each priority rank can only be used once across active applications.
- Application list: `GET /api/applications`.

### Drafts and CV upload

- Server-side application drafts: `GET/PUT/DELETE /api/drafts/application?jobId=...`
- CV upload: `POST /api/upload` (PDF); draft may reference uploaded filename.
- Legacy `localStorage` draft keys may still be migrated on load for older browser sessions.

### Deadline reminders

- Dashboard and notifications highlight open jobs whose deadline is within **48 hours** and for which the TA has not yet applied.

### Optional AI match score

- Application form can request a match score via `POST /api/jobs/{jobId}/match` before submit (subject to system limits).

---

## Tech Stack

- **HTML5** + **Tailwind CSS** (CDN)
- **Iconify** (CDN)
- **Java backend** — REST API, JSON persistence, file uploads in `uploads/`

---

## Troubleshooting

| Issue | What to do |
|-------|------------|
| Login fails after server restart | Log in again — sessions are stored in server memory only |
| Cannot submit application | Check priority rules, quota, deadline, and weekly workload limits (error message from API) |
| CDN styles missing | Ensure the machine has internet access for Tailwind / Iconify |

For full setup and seeded accounts, see the main [README](../README.md).
