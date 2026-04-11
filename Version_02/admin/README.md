# Admin (Static Admin Pages)

This folder contains a **static (plain HTML)** admin dashboard UI:

- Pages are provided as `*.html` files
- Navigation is done via relative links between pages (no SPA router)
- Styling/icons/charts are loaded via CDN (no npm, no build step)

## Tech Stack & External Dependencies

- **TailwindCSS** (CDN) for layout and styling
- **Iconify** (CDN) for icons
- **ECharts** (CDN) for charts on selected pages
- **Google Fonts - Inter** for typography

Note: the JavaScript in these pages is primarily **UI demo interactions** (modals, drawers, tabs, alerts, etc.). There is currently no real backend API integration or authentication/authorization logic.

## Pages (Entry & Navigation)

Recommended entry page: `admin-dashboard.html`.

- `admin-dashboard.html`
  - System overview dashboard
  - Includes an ECharts trend chart and a “Refresh Data” demo action
- `job-supervision.html`
  - Job / recruitment post supervision list
  - Filters + bulk action bar (demo logic)
  - Contains a small amount of Chinese UI text (e.g., delete confirmation)
- `workload-overview.html`
  - TA workload monitoring and statistics
  - Includes ECharts pie/line charts and export/filter demo actions
- `user-management.html`
  - User management (students / teachers / administrators)
  - Includes “Add user” and “Permissions” modals (demo logic)
- `basic-statistics.html`
  - Basic statistics and export center
  - Includes ECharts charts and an export history table
- `system-settings.html`
  - System configuration (parameters, SMTP, email templates, admin/privileges, backup & recovery, etc.)
  - Includes “Restore” modal demo logic
- `audit-logs.html`
  - Audit log list
  - Includes a log details drawer (right side panel)

## How to Run / Preview

### Option 1: Open directly in a browser

Since these are static pages, you can open `admin-dashboard.html` directly.

- Chrome / Edge recommended
- If you later add `fetch()` calls or other features that require an `http(s)` context, the `file://` approach may hit browser restrictions

### Option 2: Start a local static server (recommended)

Run a static server in this folder and visit:

- `http://localhost:<port>/admin-dashboard.html`

Windows (PowerShell) examples:

- Python 3:
  - `python -m http.server 5173`
- Node (if installed):
  - `npx serve -l 5173`

## Folder Structure

This folder currently contains only page files:

- `admin-dashboard.html`
- `audit-logs.html`
- `basic-statistics.html`
- `job-supervision.html`
- `system-settings.html`
- `user-management.html`
- `workload-overview.html`

Not included:

- A dedicated `assets/` folder (CSS/JS/images)
- Shared layout/components (sidebar and base styles are duplicated per page)
- Real backend API endpoints, request wrappers, login, or access control

## Common Maintenance Notes

- **Unify sidebar/header**: each page embeds its own sidebar/styles. Consider extracting a shared template/component to avoid repeated edits.
- **CDN dependencies**:
  - CDN may be unreachable in intranet/offline environments
  - For production, consider self-hosting static assets (or serving from your own static domain)
- **Authentication & authorization**: currently there is no login state or permission checks. When integrating with a backend, you typically need:
  - A login page (token/session acquisition)
  - A guard on page load (redirect to login when unauthenticated)
  - Role-based UI gating (hide menus/actions) plus server-side enforcement as the final authority
- **Data integration**: current charts and tables use static/demo data. When wiring up real data, consider:
  - Centralizing HTTP calls into a single `api.js` (or TypeScript) module
  - Standardizing error handling, timeouts, retries, and loading states

## Known Issue

- `workload-overview.html` contains a small piece of corrupted/extra text near the end of the sidebar markup (similar to `</aside>te-100"&gt;`).
  - It does not change the overall intent of the page, but should be cleaned up to improve readability and reduce layout risk.

## Notes

This README **only documents the admin static pages in this folder**. If your repository also contains a backend, a non-admin user portal, or other modules, document those modules separately in their own directories.
