---
template: test-plan
version: 0.1.0
---
# Test Plan: Super Admin

## Unit Tests

### Backend

| # | Subject | Cases |
|---|---|---|
| U-01 | `requireSuperAdmin()` | passes for `super_admin = true`; 403 for `false`; 403 for missing flag |
| U-02 | `mintImpersonationToken()` | JWT contains `impersonated_by`, `impersonation_session_id`; `exp` = now + 3600; `sub` = manager ID |
| U-03 | Audit logging plugin | writes row for every SA route; captures `actor_id`, `action`, `target_id`; does not write for non-SA routes |
| U-04 | Impersonation claim in audit plugin | when impersonation JWT used: `impersonated_as` + `impersonation_session_id` populated in log row |
| U-05 | `DELETE /sa/clubs/{id}` — confirm_name guard | returns 400 when `confirm_name` absent; 400 when name mismatch; 200 when exact match |
| U-06 | `GET /sa/billing/summary` rate config | returns correct CHF for configurable rate (1.00/member default; 2.00 when config overridden) |
| U-07 | Export job state machine | status transitions: `pending → processing → done`; `done` sets `result_path`; `failed` on exception |
| U-08 | Audit log immutability | DB role cannot execute `UPDATE audit_log` or `DELETE FROM audit_log` (test at DB level) |

---

## Integration Tests

| # | Flow | Assertions |
|---|---|---|
| I-01 | SA login → access SA route | Valid JWT + `super_admin = true` → 200; normal user JWT → 403 |
| I-02 | Create club + invite managers | Club row created; invite emails queued; managers appear as `pending` in `GET /sa/clubs/{id}/managers` |
| I-03 | Deactivate club → club members lose access | `clubs.status = inactive`; regular API calls for that club return 403 |
| I-04 | Start impersonation → act as manager → end session | `impersonation_sessions` row created; impersonation JWT valid for manager routes; `ended_at` set on end |
| I-05 | Impersonation session expires (mock time) | Impersonation JWT rejected after 3600s; `ended_at` auto-set by expiry check |
| I-06 | SA action during impersonation → audit log | Row has both `actor_id = sa_id` and `impersonated_as = manager_id` |
| I-07 | Audit log export — small dataset | `POST /export` → job `pending`; poll → `done`; `GET` returns CSV file with correct headers + rows |
| I-08 | Audit log export — filter respected | Export with `actor = sa_x` only contains rows for that actor |
| I-09 | User search excludes personal data | Response contains `club_memberships[]` but no `phone`, `jersey_number`, `position`, etc. |
| I-10 | Billing summary reflects live member count | Add member to club → `GET /sa/billing/summary` shows updated count |

---

## Edge Case Tests

| # | Scenario | Expected |
|---|---|---|
| E-01 | Delete active club (not deactivated first) | 409 "Deactivate club before deleting" (or require deactivation first per UX) |
| E-02 | Remove last manager from club | Allowed; warning banner shown in SA panel |
| E-03 | Impersonate manager of inactive club | Allowed (SA support use case); impersonation JWT still valid |
| E-04 | Two SAs impersonate same manager simultaneously | Both sessions valid independently; audit log records both |
| E-05 | Export job fails mid-write | `export_jobs.status = failed`; partial file cleaned up; poll returns `{ status: "failed" }` |
| E-06 | `confirm_name` with leading/trailing whitespace | Server trims before compare; mismatch if differs after trim |

---

## Manual / E2E Tests

| # | Journey | Steps |
|---|---|---|
| M-01 | Full SA onboarding flow | Login to SA panel → create club → invite 2 managers → verify both appear as pending → one accepts → verify active |
| M-02 | Impersonation session | Start impersonation → amber banner visible on all pages → perform action → verify audit log entry → exit → banner gone |
| M-03 | Impersonation auto-expiry | Start session → wait / mock time → verify 5-min warning toast → session ends → redirected to SA context |
| M-04 | Delete club type-to-confirm | Click delete → modal appears → type wrong name → button stays disabled → type correct name → button enables → confirm → club gone |
| M-05 | Audit log export | Filter by date range → export CSV → file downloads with correct rows → verify no extra columns |
| M-06 | Billing summary accuracy | Add/remove members across clubs → verify counts update in billing summary |
