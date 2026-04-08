# TA Recruitment System v2

A web-based Teaching Assistant recruitment management system for BUPT International School, built with Java (`com.sun.net.httpserver`) and vanilla HTML/JS frontend.

## Quick Start

### Prerequisites
- **Java 11+** (JDK, not just JRE)
- Windows OS (`.bat` scripts provided)

### Build & Run
```
build.bat        # Compiles Java sources (downloads Gson if needed)
run.bat          # Starts server at http://localhost:8080
run.bat 3000     # Use custom port
```

### Default Accounts
| Role  | Username | Password  | Login URL |
|-------|----------|-----------|-----------|
| Admin | admin    | admin123  | `/MO/index.html` |
| MO    | *(register)* | *(choose)* | `/MO/index.html` |
| TA    | *(register)* | *(choose)* | `/TA/index.html` |

## Architecture

```
Test_Version_02/
├── src/main/java/com/bupt/tarecruit/
│   ├── Main.java                    # HTTP server bootstrap
│   ├── model/                       # Data models (POJO)
│   │   ├── User.java
│   │   ├── Job.java
│   │   ├── Application.java
│   │   ├── ApplicationDraft.java
│   │   ├── Notification.java
│   │   ├── AuditLog.java
│   │   └── PasswordResetRequest.java
│   ├── service/
│   │   └── DataService.java         # JSON file persistence + sessions
│   └── handler/                     # HTTP request handlers
│       ├── BaseHandler.java         # Shared utilities (auth, JSON, CORS)
│       ├── AuthHandler.java         # Login, register, profile, password
│       ├── JobHandler.java          # Job CRUD + apply
│       ├── ApplicationHandler.java  # Application list, detail, status
│       ├── DraftHandler.java        # TA application drafts (server-side)
│       ├── AdminHandler.java        # Users, workload, stats, settings, audit
│       ├── UploadHandler.java       # File upload/download (base64)
│       ├── NotificationHandler.java # User notifications
│       └── StaticHandler.java       # Serves HTML/JS/CSS files
├── TA/          # TA applicant frontend (10 pages)
├── MO/          # Module Organiser frontend (8 pages)
├── admin/       # Administrator frontend (7 pages)
├── js/api.js    # Shared frontend API utilities
├── data/        # JSON data files (auto-created at runtime)
├── uploads/     # Uploaded CV files (auto-created at runtime)
├── lib/         # gson-2.10.1.jar
├── build.bat
└── run.bat
```

## Data Storage

All data stored in `data/` as JSON files (no database):
- `users.json` — User accounts (TA, MO, ADMIN)
- `jobs.json` — Job postings
- `applications.json` — TA applications
- `application_drafts.json` — TA application drafts (server-side, per user + job)
- `notifications.json` — System notifications
- `audit_logs.json` — Audit trail
- `password_resets.json` — Password reset requests
- `settings.json` — System configuration

Uploaded files (CVs) stored in `uploads/`.

## API Reference

### Authentication (`/api/auth`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/login` | No | Login with username/password |
| POST | `/api/auth/register` | No | Register new user |
| POST | `/api/auth/logout` | Yes | Logout |
| GET | `/api/auth/me` | Yes | Get current user profile |
| PUT | `/api/auth/profile` | Yes | Update profile fields |
| PUT | `/api/auth/password` | Yes | Change password |
| POST | `/api/auth/password-reset` | No | Submit password reset request |

### Jobs (`/api/jobs`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/jobs?status=&search=&postedBy=` | No | List jobs (with filters) |
| POST | `/api/jobs` | MO/Admin | Create job posting |
| GET | `/api/jobs/{id}` | No | Get job details |
| PUT | `/api/jobs/{id}` | Owner/Admin | Update job |
| DELETE | `/api/jobs/{id}` | Owner/Admin | Delete job |
| POST | `/api/jobs/{id}/apply` | TA | Apply for job |
| GET | `/api/jobs/{id}/applications` | Owner/Admin | List applications for job |

### Applications (`/api/applications`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/applications` | Yes | List applications (role-scoped) |
| GET | `/api/applications/{id}` | Yes | Get application detail |
| PUT | `/api/applications/{id}/status` | Yes | Update status (TA: withdraw; MO: approve/reject) |

### Admin (`/api/admin`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/admin/users?search=&role=` | Admin | List/search users |
| PUT | `/api/admin/users/{id}` | Admin | Update user (active, role, password) |
| DELETE | `/api/admin/users/{id}` | Admin | Delete user |
| GET | `/api/admin/workload` | Admin | TA workload overview |
| GET | `/api/admin/stats` | Admin | System statistics |
| GET/PUT | `/api/admin/settings` | Admin | System settings |
| GET | `/api/admin/audit-logs?action=&search=` | Admin | Audit log viewer |
| GET | `/api/admin/password-resets?status=` | Admin | Password reset requests |
| PUT | `/api/admin/password-resets/{id}` | Admin | Approve/reject reset |
| GET | `/api/admin/export` | Admin | Export data as CSV |

### Drafts (`/api/drafts`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/drafts/application?jobId={jobId}` | TA | Get current user's application draft for a job |
| PUT | `/api/drafts/application?jobId={jobId}` | TA | Create/update current user's application draft for a job |
| DELETE | `/api/drafts/application?jobId={jobId}` | TA | Delete current user's application draft for a job |

## TA_09 Server-side Drafts

The TA application form (`/TA/ta-recruitment-application-form.html`) supports saving drafts to the backend for cross-device recovery.

- **Draft data** is stored in `data/application_drafts.json` (scoped by `userId + jobId`).
- **Draft resume** is stored as an uploaded file in `uploads/`, and the draft stores `resumeDraftFileName`.

### Verify

1. Login as a TA and open a job application form.
2. Fill in some fields and click `Save progress`.
3. Refresh the page: the draft should be restored.
4. Open the same job application form in another browser/device with the same TA account: the draft should be restored.

### Other
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/upload` | Yes | Upload file (base64 JSON) |
| GET | `/api/upload/{fileName}` | Yes | Download file |
| GET | `/api/notifications` | Yes | List user's notifications |
| PUT | `/api/notifications/{id}/read` | Yes | Mark notification as read |
| PUT | `/api/notifications/read-all` | Yes | Mark all as read |

## Implemented User Stories

### Sprint 1 (Must-have) ✅
- **TA_01** Personal Profile Management
- **TA_02** Vacancy Browsing & Filtering
- **TA_04** Self-Service Password Modification
- **TA_05** Forgot Password — Online Application
- **MO_01** Manage TA's Work
- **MO_02** View Questionnaire Submissions
- **MO_03** User Registration & Role Selection
- **MO_08** Self-Service Password Modification
- **MO_09** Forgot Password — Online Application
- **AD_01** Monitor Overall TA Workload
- **AD_02** Manage System Users
- **AD_03** Oversee Job Postings
- **AD_10** Admin Password Reset Processing

### Sprint 2 (Should-have) ✅
- **TA_03** Integrated CV Submission
- **TA_06** Forgot Password — Direct Admin Contact
- **TA_08** Document Preview Before Upload
- **TA_09** Save as Draft (server-side, supports cross-device recovery)
- **MO_04** Simplified Approval Process
- **MO_06** Applicant CV Review
- **MO_07** Applicant Selection & Approval
- **AD_04** View Basic Application Stats
- **AD_05** Export Core System Data
- **AD_11** Search Users/Jobs
- **AD_13** View Individual User Details

### Sprint 3 (Could-have) ✅
- **TA_10** Application Status Tracking
- **TA_11** Application Withdrawal
- **AD_06** View System Key Event Log
- **AD_12** View Job Application Status Summary

## Tech Stack
- **Backend**: Java 11+ with `com.sun.net.httpserver.HttpServer`
- **Data**: JSON files via Gson 2.10.1
- **Frontend**: HTML5, Tailwind CSS (CDN), Iconify icons, ECharts
- **No database, no framework** — per course requirements
