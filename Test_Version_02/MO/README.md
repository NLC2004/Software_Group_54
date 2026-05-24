# MO Portal — EduMO / TA Recruitment (Module Organiser)

This folder contains the **Module Organiser (MO)** web portal for the BUPT International School TA Recruitment System (`Test_Version_02`). The product name shown in the UI is **EduMO**. MO users post and manage TA vacancies, review applicants, view statistics, and manage notifications.

All pages are served by the Java backend together with the REST API. **Do not open these HTML files with a standalone static server** — login, job CRUD, and applicant review require the main application server.

---

## Prerequisites

1. Build and start the server from the project root (`Test_Version_02/`). See the main [README](../README.md).
2. Open the MO login page in a browser:

   `http://localhost:8080/MO/index.html`

   (Replace `8080` if you used a custom port.)

3. Log in with an MO account (e.g. `teacher01` … `teacher06`, password `123456`).

Shared frontend utilities live in [`../js/api.js`](../js/api.js) (authentication, REST calls, toasts).

---

## Pages

| File | Description |
|------|-------------|
| `index.html` | MO login and registration |
| `forgot-password.html` | Submit a password-reset request |
| `my-job-list.html` | Dashboard — list of posted vacancies |
| `job-create.html` | Create a new TA vacancy |
| `job-edit.html` | Edit an existing vacancy |
| `job-detail.html` | Vacancy detail, applicant list, approve/reject |
| `statistics-analysis.html` | Recruitment statistics and charts (ECharts) |
| `notifications-messages.html` | System notifications |
| `personal-center.html` | Profile and account settings |

Typical flow: **Dashboard** → create or open a job → **Edit** / **Detail** → review applications.

Navigation uses a shared sidebar and breadcrumbs across pages.

---

## Tech Stack

- **HTML5** + **Tailwind CSS** (CDN)
- **Iconify** (CDN) for icons
- **Google Fonts** — Inter
- **ECharts** (CDN, statistics page only)
- **Backend** — Java `HttpServer`, JSON file storage, REST API under `/api/`

---

## API Integration

MO pages call the backend for:

- Authentication (`/api/auth/login`, `/api/auth/logout`, profile)
- Job management (`/api/jobs`, create / update / delete / status)
- Applications per job (`/api/jobs/{id}/applications`, status updates)
- Notifications (`/api/notifications`)
- File upload for related features where applicable

Portal-scoped auth tokens are stored in `localStorage` via `api.js` (key suffix `_mo`).

---

## Troubleshooting

| Issue | What to do |
|-------|------------|
| Login or save fails after restarting the server | Sessions are in-memory — log in again at `/MO/index.html` |
| Job detail shows updates but Edit page looks stale | Hard-refresh (`Ctrl+F5`) or open **Edit Job** again from the detail page |
| Styles or icons missing | Internet access is required for CDN assets on first load |

For full setup, configuration, and default accounts, see the main [README](../README.md).
