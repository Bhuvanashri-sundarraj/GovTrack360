# GovTrack 360 — Government Productivity & Performance Management System

**Phase 1 + 2 + 3: Authentication, Employee & Department Management, Task Workflow, Attendance, Leave, Notifications, Audit Logs, Performance, Rewards & Recognition, Reports, Analytics**

A full-stack e-Office platform for government offices, built with a React (Vite) frontend and a Spring Boot backend on PostgreSQL. This repository implements **all three phases**.

---

## 1. Project Overview

GovTrack 360 digitizes government office operations: employee records, department structure, role-based dashboards, task assignment and review workflows, attendance and leave tracking, notifications, and a full audit trail — all backed by real PostgreSQL data. Nothing in this codebase is a frontend-only mock.

## 2. Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, Tailwind CSS, React Router v6, Axios, lucide-react |
| Backend | Java 17, Spring Boot 3.3, Spring Web, Spring Data JPA (Hibernate), Spring Security, JWT (jjwt) |
| Database | PostgreSQL |
| Build tools | Maven (backend), npm (frontend) |

## 3. Architecture

```
React Frontend (Vite, port 5173)
        │  Axios / REST (JSON) / JWT Bearer token
        ▼
Spring Boot Backend (port 8080)
        │  Spring Security filter chain (stateless JWT)
        ▼
Spring Data JPA / Hibernate
        │
        ▼
PostgreSQL
```

Backend packages: `entity → repository → service → controller`, with `security`, `config`, `dto`, and `exception` as supporting layers. Phase 2 added new entities/services/controllers as new files under this same structure — no Phase 1 class was rewritten to accommodate it — so Phase 3 (Performance, Rewards, Reports) can be added the same way.

## 4. Prerequisites

- Java 17 (JDK)
- Maven 3.9+ (or use your IDE's bundled Maven)
- Node.js 18+ and npm
- PostgreSQL 14+ running locally (or accessible remotely)

## 5. PostgreSQL Setup

1. Install and start PostgreSQL.
2. Create a database and (optionally) a dedicated user:

```sql
CREATE DATABASE govtrack360;
CREATE USER govtrack_user WITH ENCRYPTED PASSWORD 'change_this_password';
GRANT ALL PRIVILEGES ON DATABASE govtrack360 TO govtrack_user;
```

Hibernate is configured with `ddl-auto: update`, so tables are created automatically the first time the backend starts — you do not need to run any SQL scripts manually. Phase 2's new tables (`tasks`, `task_workflow_history`, `task_comments`, `attendance`, `leave_requests`, `notifications`, `audit_logs`) are created the same way, alongside the existing Phase 1 tables.

## 6. Database Configuration

The backend reads its database and JWT settings from environment variables (see `backend/src/main/resources/application.yml`):

| Variable | Purpose | Example |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/govtrack360` |
| `DB_USERNAME` | DB user | `govtrack_user` |
| `DB_PASSWORD` | DB password | `change_this_password` |
| `JWT_SECRET` | HMAC secret for signing JWTs (32+ chars) | a long random string |
| `JWT_EXPIRATION_MS` | Token lifetime in ms | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins allowed to call the API | `http://localhost:5173` |

Sensible local defaults are baked in as fallbacks, so the app runs out of the box for a demo, but you should override `JWT_SECRET` and DB credentials for anything beyond local testing.

You can export these as real environment variables, or create `backend/src/main/resources/application-local.yml` (already gitignored) and run with `--spring.profiles.active=local`.

## 7. Backend Setup

```bash
cd backend
# Option A: environment variables
export DB_URL=jdbc:postgresql://localhost:5432/govtrack360
export DB_USERNAME=govtrack_user
export DB_PASSWORD=change_this_password
export JWT_SECRET=replace-with-a-long-random-string-at-least-32-characters

mvn spring-boot:run
```

On first successful startup, `DataSeeder` automatically inserts the Phase 1 demo data (5 departments, 8 employees, 3 login accounts) **and** Phase 2 demo data (8 tasks spanning every status, recent attendance history, 3 leave requests, welcome notifications, and audit log entries) — but only if the `departments` and `users` tables are empty, so it is safe to restart the app without duplicating data.

Backend runs on **http://localhost:8080**.

## 8. Frontend Setup

```bash
cd frontend
npm install
cp .env.example .env    # adjust VITE_API_BASE_URL if your backend runs elsewhere
npm run dev
```

Frontend runs on **http://localhost:5173**.

## 9. How to Run the Application

1. Start PostgreSQL and ensure the `govtrack360` database exists.
2. Start the backend: `cd backend && mvn spring-boot:run`
3. Start the frontend: `cd frontend && npm run dev`
4. Open `http://localhost:5173` in your browser.
5. Log in with one of the demo accounts below.

## 10. Demo Credentials

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@govtrack360.com | Admin@123 |
| OFFICER | officer@govtrack360.com | Officer@123 |
| EMPLOYEE | employee@govtrack360.com | Employee@123 |

All passwords are hashed with BCrypt before being stored — the plaintext values above only ever exist in this README and in the login form.

## 11. API Overview

Base URL: `http://localhost:8080/api`

### Phase 1

**Auth**
- `POST /auth/login` — returns a JWT
- `GET /auth/me` — current authenticated user (requires Bearer token)
- `POST /auth/logout` — no-op confirmation (JWT is stateless; client discards the token)

**Employees** (ADMIN full CRUD, OFFICER read-only, EMPLOYEE no access)
- `GET /employees?search=&departmentId=&status=`, `GET /employees/{id}`, `POST /employees`, `PUT /employees/{id}`, `PATCH /employees/{id}/status`, `DELETE /employees/{id}`

**Departments** (ADMIN full CRUD, OFFICER + EMPLOYEE read-only)
- `GET /departments?search=`, `GET /departments/{id}`, `POST /departments`, `PUT /departments/{id}`, `PATCH /departments/{id}/status`, `DELETE /departments/{id}` (blocked if employees are still assigned)

**Dashboard**
- `GET /dashboard/admin`, `GET /dashboard/officer`, `GET /dashboard/employee`

**Profile**
- `GET /profile`, `PUT /profile` (first name, last name, phone only — role/email are not editable here)

### Phase 2

**Tasks** (ADMIN full control; OFFICER dept-scoped create/assign/edit/review; EMPLOYEE progress/submit/comment on their own assigned tasks only)
- `GET /tasks?search=&status=&priority=&departmentId=&assignedToId=` — role-scoped automatically
- `GET /tasks/{id}`, `POST /tasks`, `PUT /tasks/{id}`
- `PATCH /tasks/{id}/assign` — assign/reassign
- `PATCH /tasks/{id}/progress` — assignee only, 0–100
- `PATCH /tasks/{id}/submit` — assignee only, requires progress = 100
- `PATCH /tasks/{id}/review` — `{ "decision": "APPROVE" | "REJECT", "comment": "..." }` (comment required on reject)
- `PATCH /tasks/{id}/status` — cancel only (`{ "status": "CANCELLED" }`)
- `DELETE /tasks/{id}` — ADMIN only, PENDING/CANCELLED tasks only
- `GET/POST /tasks/{id}/comments`, `GET /tasks/{id}/history` — read-only workflow trail

**Attendance**
- `POST /attendance/check-in`, `PATCH /attendance/check-out` — self, one record per employee per day (DB-enforced unique constraint)
- `GET /attendance/my?month=&year=`, `GET /attendance/my/summary?month=&year=`
- `GET /attendance?employeeId=&departmentId=&date=` (ADMIN/OFFICER, dept-scoped for OFFICER)
- `GET /attendance/summary/today` (ADMIN/OFFICER)

**Leave**
- `POST /leave-requests`, `GET /leave-requests/my`, `PATCH /leave-requests/{id}/cancel` (own pending only)
- `GET /leave-requests?employeeId=&departmentId=&status=` (ADMIN/OFFICER, dept-scoped for OFFICER)
- `PATCH /leave-requests/{id}/review` — `{ "decision": "APPROVE" | "REJECT", "comment": "..." }` (comment required on reject)

**Notifications** (scoped to the caller)
- `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all`

**Audit Logs** (ADMIN only, read-only)
- `GET /audit-logs?search=&userId=&module=&action=&from=&to=`

Every endpoint except `/auth/**` requires an `Authorization: Bearer <token>` header. Unauthorized requests get `401`; requests from an authenticated user without the right role get `403` — enforced by Spring Security `@PreAuthorize` **and**, where a check depends on runtime data (department ownership, task assignee), by explicit service-layer checks that throw `AccessDeniedException` — never just hidden in the UI.

### Phase 3

| Area | Endpoints |
|---|---|
| Performance (self) | `GET /api/performance/my`, `GET /api/performance/my/trend?months=`, `GET /api/performance/reviews/my` |
| Performance (management) | `GET /api/performance/leaderboard`, `GET /api/performance/employees/{id}`, `GET /api/performance/employees/{id}/trend`, `GET /api/performance/scope`, `GET /api/performance/eligible-employees` |
| Reviews | `GET/POST /api/performance/reviews`, `GET/PUT /api/performance/reviews/{id}`, `DELETE /api/performance/reviews/{id}` (admin) |
| Rewards | `GET /api/rewards/types`, `GET /api/rewards/wall`, `GET /api/rewards/my`, `GET /api/rewards/leaderboard`, `GET/POST /api/rewards`, `DELETE /api/rewards/{id}` |
| Reports | `GET /api/reports/types`, `GET /api/reports/{type}`, `GET /api/reports/{type}/export` (CSV). Types: `TASK_SUMMARY`, `EMPLOYEE_PERFORMANCE`, `ATTENDANCE`, `LEAVE`, `DEPARTMENT_SUMMARY`, `REWARDS` |
| Analytics | `GET /api/analytics/overview` |

All list/report/analytics endpoints accept optional `from` / `to` (`YYYY-MM-DD`; default = last 90 days, max 366 days) and `departmentId` (ignored for officers, who are always limited to their own department).

## 12. Role Permissions

### Phase 1

| Action | ADMIN | OFFICER | EMPLOYEE |
|---|:---:|:---:|:---:|
| View own role-specific dashboard | ✅ | ✅ | ✅ |
| View employees list | ✅ | ✅ | ❌ |
| Create/Edit/Delete/Deactivate employees | ✅ | ❌ | ❌ |
| View departments | ✅ | ✅ | ✅ |
| Create/Edit/Delete/Deactivate departments | ✅ | ❌ | ❌ |
| View/edit own profile | ✅ | ✅ | ✅ |

### Phase 2

| Action | ADMIN | OFFICER | EMPLOYEE |
|---|:---:|:---:|:---:|
| View all tasks | ✅ | dept-scoped | assigned-only |
| Create / assign / reassign / edit / cancel tasks | ✅ | ✅ (own dept) | ❌ |
| Delete tasks | ✅ (PENDING/CANCELLED only) | ❌ | ❌ |
| Update progress / submit / comment on a task | ❌ | ❌ | ✅ (own assigned tasks) |
| Review (approve/reject) tasks | ✅ | ✅ (own dept) | ❌ |
| Check in / check out, view own attendance | ✅ | ✅ | ✅ |
| View all attendance records | ✅ | ✅ (own dept) | ❌ (self only) |
| Apply / cancel own leave | ✅ | ✅ | ✅ |
| Approve / reject leave requests | ✅ | ✅ (own dept) | ❌ |
| View own notifications | ✅ | ✅ | ✅ |
| View audit logs | ✅ | ❌ | ❌ |


### Phase 3

| Action | ADMIN | OFFICER | EMPLOYEE |
|---|---|---|---|
| View own performance score, trend and reviews | ✅ | ✅ | ✅ |
| View leaderboard / any employee's score | ✅ | ✅ (own dept) | ❌ |
| Write / edit performance reviews | ✅ (any) | ✅ (own dept; edit only own reviews) | ❌ |
| Delete a review | ✅ | ❌ | ❌ |
| Grant rewards | ✅ | ✅ (own dept) | ❌ |
| Revoke rewards | ✅ (any) | ✅ (only ones they granted) | ❌ |
| View own rewards + recognition wall | ✅ | ✅ | ✅ |
| Reports (view + CSV export) | ✅ | ✅ (own dept) | ❌ |
| Analytics dashboard | ✅ | ✅ (own dept) | ❌ |

Nobody can review or reward themselves. Only one *Employee of the Month* may be granted per department per calendar month.

## 13. Navigation

| ADMIN | OFFICER | EMPLOYEE |
|---|---|---|
| Dashboard | Dashboard | Dashboard |
| Employees | Employees | My Tasks |
| Departments | Departments | My Attendance |
| Tasks | Tasks | My Leave |
| Attendance | Attendance | Notifications |
| Leave | Leave | My Profile |
| Notifications | Notifications | Departments |
| Performance | Performance | My Performance |
| Rewards | Rewards | My Rewards |
| Reports | Reports | |
| Analytics | Analytics | |
| Audit Logs | Profile | |
| Profile | | |

`/attendance`, `/leave`, `/performance` and `/rewards` are single routes that render a management view for ADMIN/OFFICER or a self-service view for EMPLOYEE, based on role — no separate URLs needed. Task workflow history is shown inline on the Task Details page rather than as a separate module.

## 14. Project Folder Structure

```
govtrack360/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/govtrack360/
│       │   ├── config/        (SecurityConfig, CorsConfig, DataSeeder)
│       │   ├── security/      (JwtUtil, JwtAuthenticationFilter, CustomUserDetails...)
│       │   ├── entity/        (User, Employee, Department, Task, Attendance, LeaveRequest,
│       │   │                   Notification, AuditLog, TaskWorkflowHistory, TaskComment, enums)
│       │   ├── repository/    (Spring Data JPA repositories)
│       │   ├── dto/           (request/response objects, grouped by feature)
│       │   ├── service/       (business logic — TaskService, AttendanceService, LeaveService,
│       │   │                   NotificationService, AuditLogService, etc.)
│       │   ├── controller/    (REST endpoints)
│       │   └── exception/     (custom exceptions + GlobalExceptionHandler)
│       └── resources/application.yml
└── frontend/
    └── src/
        ├── api/           (axios client + interceptors)
        ├── context/       (AuthContext)
        ├── routes/        (ProtectedRoute)
        ├── layouts/       (Sidebar, Header, DashboardLayout)
        ├── pages/         (Dashboards, Employees, Departments, Profile, Tasks, MyTasks,
        │                   TaskDetails, Attendance, Leave, Notifications, AuditLogs...)
        ├── components/    (Modal, ConfirmDialog, Toast, Badge, TaskStatusBadge,
        │                   ProgressBar, NotificationBell...)
        └── services/      (thin wrappers per API resource)
```

## 15. Phase 1 — Implemented Features

- JWT authentication with BCrypt password hashing
- Role-based access control (ADMIN / OFFICER / EMPLOYEE) enforced on the backend with `@PreAuthorize`
- Protected frontend routes with automatic redirect on missing/expired tokens
- Employee Management (search, filter, create, edit, activate/deactivate, delete)
- Department Management (search, create, edit, activate/deactivate, delete-with-guard, employee counts)
- Profile view/edit (non-sensitive fields only)
- Global exception handling with consistent JSON error responses
- Responsive, government-enterprise-styled UI

## 16. Phase 2 — Implemented Features

- **Task Management**: full lifecycle (create → assign/reassign → progress → submit → review → approve/reject/reopen), enforced status-transition rules, department- and assignee-scoped visibility
- **Workflow History**: append-only, auto-generated audit trail on every task action, visible on the Task Details page
- **Task Comments**: threaded discussion per task, visible only to authorized viewers
- **Task Review & Approval**: reviewers can approve or reject (rejection requires a comment and reopens the task for the employee)
- **Attendance**: check-in/check-out with duplicate-prevention (DB-level unique constraint), automatic LATE/HALF_DAY classification, monthly self-summary, admin/officer org-wide view and today's summary
- **Leave Management**: apply/cancel (employee), approve/reject with required rejection comment (admin/officer), leave-aware check-in blocking
- **Notifications**: real-time-ish (30s poll) unread count in the header bell, mark-read/mark-all-read, generated automatically on task assignment/submission/approval/rejection and leave approval/rejection
- **Audit Logs**: append-only, admin-only, searchable by user/module/action/date range; automatically written on login, and every task/attendance/leave action
- **Dashboard extensions**: all three role dashboards now also show Active/Awaiting-Review/Overdue task counts, today's attendance, and pending leave counts — all from live database queries
- Seeded demo data: 8 tasks across every status, recent attendance history, 3 leave requests (1 approved, 2 pending), welcome notifications, and audit log entries — safe to restart without duplicating

## 17. Phase 3 — Implemented Features

- **Performance scoring** — calculated live from Phase 2 data, nothing to maintain. The overall score (0–100) is a weighted average of: task completion (25), on-time delivery (20), quality / first-pass approvals (15), attendance (25) and punctuality (15). A component with no data in the period is left out and the weights are rescaled, so nobody is penalised for something that could not be measured. Bands: Outstanding ≥ 90, Very Good ≥ 80, Good ≥ 65, Needs Improvement ≥ 50, Unsatisfactory < 50.
- **Metric definitions** — *tasks in scope* are non-cancelled tasks assigned to the employee whose due date falls in the period (only days up to today are evaluated, so tasks not yet due never count against anyone); *delivered* = approved, submitted or under review; *on-time* = delivered by the due date, with tasks still open past their due date counting against it; *quality* = approvals ÷ (approvals + rejections); *attendance* = (present + late + ½ × half-days) ÷ expected working days, where working days are Monday–Friday from the later of the period start and joining date, excluding approved leave. There is no public-holiday calendar, so holidays count as working days.
- **Leaderboard, per-employee breakdown and 6-month trend** for admins/officers; a personal dashboard with the same breakdown for every employee.
- **Performance reviews** — reviewer ratings (quality, teamwork, initiative, 1–5) blended with the system score: *final = 70% system score + 30% manager score*. The system score is snapshotted when a review is saved or edited. One review per employee per period.
- **Rewards & Recognition** — 7 reward types with suggested points, citations, a points leaderboard, per-employee totals and an organisation-wide recognition wall. Recipients are notified.
- **Reports** — six report types in one tabular format, with filters, on-screen view, **CSV export** (Excel-safe, formula-injection protected) and **Print / Save as PDF**. CSV exports are written to the audit log.
- **Analytics** — KPIs, six-month trend, department comparison, rating distribution, task status/priority breakdowns, top performers, employees needing attention and recent rewards; all charts are lightweight inline SVG (no extra npm packages).
- **Dashboards** — Admin/Officer dashboards show average performance and rewards this month; the Employee dashboard shows score, band and reward points.
- **Demo data** — `Phase3DataSeeder` runs after `DataSeeder`, works on a fresh *or* an existing Phase 2 database, backfills ~6 months of weekday attendance (only on dates that have no record), and adds 5 reviews and 6 rewards. It does nothing once any review or reward exists. Set `SEED_PHASE3_DEMO=false` to skip it.
- **No schema migrations needed** — Phase 3 only adds two tables (`performance_reviews`, `rewards`), created automatically by `ddl-auto: update`. No existing table or enum was changed.

## 18. Troubleshooting

- **Backend fails to start with a connection error** — confirm PostgreSQL is running and `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` are correct.
- **`relation "tasks" does not exist`** (or any Phase 2 table) — first run creates tables automatically via `ddl-auto: update`; make sure the configured DB user has `CREATE TABLE` privileges.
- **401 immediately after login** — check that `VITE_API_BASE_URL` in `frontend/.env` matches your backend's actual host/port, and that the browser clock isn't wildly out of sync (JWT expiry checks use time).
- **403 Forbidden on Task/Attendance/Leave actions** — check the role/ownership rules in section 12; e.g. Officers are restricted to their own department, and Employees can only act on tasks assigned to them.
- **"You have already checked in today"** — attendance is one record per employee per day, enforced at the database level; check out and wait for the next day, or check a different demo account.
- **Leave rejection / task rejection fails with a validation error** — a comment is required when rejecting; the API returns `400` with a clear message if it's missing.
- **CORS errors in the browser console** — ensure `CORS_ALLOWED_ORIGINS` includes your frontend's exact origin (protocol + host + port), e.g. `http://localhost:5173`.
- **Login works but a page is blank/errors** — open browser dev tools → Network tab and check the failing request's response body; the API returns a consistent `{ timestamp, status, error, message, path }` JSON shape for all errors.
- **Seed data didn't appear** — the seeder only runs when `departments` and `users` are both empty; if you've already run the app once, drop and recreate the `govtrack360` database to reseed (this reseeds both Phase 1 and Phase 2 demo data together).
- **Performance scores look low / "Not Rated"** — scores come from real task and attendance data. An employee with no check-ins in the period shows 0% attendance. On a database that already existed before Phase 3, the demo seeder backfills attendance automatically; with `SEED_PHASE3_DEMO=false` older periods will simply have little data.
- **Phase 3 demo data didn't appear** — it is skipped if any review or reward already exists, or if `admin@govtrack360.com` is missing. Delete all rows from `rewards` and `performance_reviews` and restart to re-run it.
- **CSV export fails with 403/400** — officers can only export their own department; the period cannot exceed 366 days.
