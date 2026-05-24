# Admin Portal — System Administration

This folder contains the **Administrator** web portal for the BUPT International School TA Recruitment System (`Test_Version_02`). Admins monitor recruitment activity, manage users and workload, process password-reset requests, configure system settings, and review audit logs.

All pages are served by the Java backend together with the REST API. **Do not use a standalone static server** — dashboards, user management, and settings require the main application server.

---

## Prerequisites

1. Build and start the server from the project root (`Test_Version_02/`). See the main [README](../README.md).
2. Open the Admin login page:

   `http://localhost:8080/admin/index.html`

3. Log in with the default admin account: `admin` / `admin123` (or another ADMIN user created in the system).

Shared frontend utilities: [`../js/api.js`](../js/api.js).

---

## Pages

Recommended entry: `admin-dashboard.html` (after login via `index.html`).

| Page | Description |
|------|-------------|
| `index.html` | Admin login |
| `admin-dashboard.html` | System overview dashboard |
| `job-supervision.html` | Supervise MO job postings across the system |
| `workload-overview.html` | TA workload monitoring and charts |
| `user-management.html` | Manage TA, MO, and admin accounts |
| `user-detail.html` | Single-user detail and edits |
| `password-reset-requests.html` | Approve or reject password-reset requests |
| `basic-statistics.html` | Recruitment statistics and export |
| `system-settings.html` | Recruitment window, workload limits, SMTP, email templates |
| `audit-logs.html` | Audit log viewer |

Admins can also open MO or TA pages in **proxy view** (via user-management links) to inspect another user's portal with admin credentials.

---

## Tech Stack & Dependencies

- **Tailwind CSS** (CDN) — layout and styling
- **Iconify** (CDN) — icons
- **ECharts** (CDN) — charts on dashboard, workload, and statistics pages
- **Google Fonts — Inter**
- **Java backend** — `/api/admin/*` and shared `/api/*` endpoints

---

## API Integration

Admin pages use authenticated REST calls, including:

- `/api/auth/login` with `portalRole: 'ADMIN'`
- `/api/admin/users`, `/api/admin/settings`, `/api/admin/password-resets`
- `/api/admin/audit-logs`, statistics and export endpoints
- Shared resources: jobs, applications, notifications

Portal-scoped auth tokens are stored in `localStorage` via `api.js` (key suffix `_admin`).

---

## How to Run

From `Test_Version_02/`:

```powershell
.\build.bat
.\run.bat
```

Then visit `http://localhost:8080/admin/index.html`.

Custom port example: `.\run.bat 9090` → use `http://localhost:9090/admin/index.html`.

---

## Maintenance Notes

- **Sidebar/layout**: each page embeds its own sidebar; consider extracting shared markup if you add many new pages.
- **CDN**: Tailwind, Iconify, and ECharts require network access unless you self-host assets.
- **Security**: UI role checks are for convenience only; the Java handlers enforce authorization on every API request.
- **SMTP**: optional; configure in System Settings or `data/settings.json` (see main [README](../README.md)).

---

## Folder Contents

```
admin/
├── index.html
├── admin-dashboard.html
├── audit-logs.html
├── basic-statistics.html
├── job-supervision.html
├── password-reset-requests.html
├── system-settings.html
├── user-detail.html
├── user-management.html
└── workload-overview.html
```

For project-wide setup, troubleshooting, and course alignment, see the main [README](../README.md).
