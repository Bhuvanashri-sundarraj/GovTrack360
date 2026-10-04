# Phase 3 change log

## Modified Phase 2 files (all additive)
| File | Change |
|---|---|
| `backend/.../repository/TaskRepository.java`, `AttendanceRepository.java`, `LeaveRequestRepository.java`, `TaskWorkflowHistoryRepository.java` | New query methods appended; no existing method touched |
| `backend/.../service/DashboardService.java` + 3 dashboard DTOs | New fields: avg performance score, rewards this month (admin/officer); score, band, reward points (employee) |
| `backend/.../config/DataSeeder.java` | `@Order(1)` so `Phase3DataSeeder` (`@Order(2)`) always runs after it |
| `backend/.../exception/GlobalExceptionHandler.java` | Bad date params / malformed JSON now return 400 instead of 500 |
| `backend/src/main/resources/application.yml` | `app.seed.phase3-demo-data` flag (env `SEED_PHASE3_DEMO`, default true) |
| `frontend/src/App.jsx`, `layouts/Sidebar.jsx` | New routes and nav entries |
| `frontend/src/pages/AdminDashboard.jsx`, `OfficerDashboard.jsx`, `EmployeeDashboard.jsx` | Performance / rewards cards |
| `frontend/src/pages/AuditLogs.jsx` | PERFORMANCE, REWARD, REPORT module filters |
| `frontend/src/index.css` | Print styles for reports |
| `README.md` | Phase 3 documentation |

## New tables
`performance_reviews`, `rewards` (auto-created by `ddl-auto: update`). No existing table or enum changed.

## Upgrading from Phase 2
1. Replace the project folder with this one (or copy the new/modified files).
2. `cd frontend && npm install` (no new dependencies were added) and restart both apps.
3. Start the backend once: Phase 3 tables are created and demo data is seeded automatically.

## Fixes to pre-existing Phase 2 issues (found while compiling/running on JDK 23 + PostgreSQL)
- `pom.xml`: pinned Lombok 1.18.42 and declared it as an annotation processor (Boot 3.3.4's Lombok does not run on JDK 23+).
- `DashboardService`: `.map(employeeService::getById)` passed an `Employee` where a `Long` id is required.
- `EmployeeService`, `TaskService`, `AuditLogService`: a null search string inside `LOWER()` fails on PostgreSQL (`function lower(bytea) does not exist`); an empty string is now passed instead.
