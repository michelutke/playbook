---
template: tasks
version: 0.1.0
gate: READY GO
---
# Tasks: Team Management

## Phase 1 — Database Schema

| ID | Task | Deps |
|---|---|---|
| TM-001 | Create `clubs` table migration | — |
| TM-002 | Create `teams` table migration | TM-001 |
| TM-003 | Create `team_memberships` table + unique `(team_id, user_id, role)` | TM-002 |
| TM-004 | Create `player_profiles` table — PK `(team_id, user_id)` | TM-003 |
| TM-005 | Create `invites` table | TM-002 |
| TM-006 | Create `club_coach_links` table | TM-001 |
| TM-007 | Add all indexes (`invite_token` unique, `(invited_email, team_id)`, `(team_id, user_id)` on memberships) | TM-003, TM-005 |

## Phase 2 — Backend Infrastructure

| ID | Task | Deps |
|---|---|---|
| TM-008 | Configure Koin: shared module (repos) + server module (DB, mailer) | TM-001 |
| TM-009 | Set up Simple Java Mail `Mailer` Koin singleton; config from `SMTP_*` env vars | TM-008 |
| TM-010 | Configure `staticFiles("/logos", File("uploads/logos"))` route | TM-008 |
| TM-011 | Implement `requireClubManager(clubId)` middleware — checks `club_managers` table | TM-008 |
| TM-012 | Implement `requireCoachOnTeam(teamId)` middleware — checks `team_memberships` | TM-008 |

## Phase 2 — Club Endpoints

| ID | Task | Deps |
|---|---|---|
| TM-013 | `POST /clubs` — create club | TM-008 |
| TM-014 | `GET /clubs/{id}` — detail | TM-013 |
| TM-015 | `PATCH /clubs/{id}` — edit name/sport/location | TM-013 |
| TM-016 | `POST /clubs/{id}/logo` — multipart; validate MIME (`image/jpeg`, `image/png`, `image/webp`); write to `uploads/logos/{clubId}.{ext}`; update `logo_url` | TM-010, TM-013 |

## Phase 2 — Team Endpoints

| ID | Task | Deps |
|---|---|---|
| TM-017 | `GET /clubs/{clubId}/teams` — list (active + archived + pending) | TM-011 |
| TM-018 | `POST /clubs/{clubId}/teams` — create (ClubManager) | TM-011 |
| TM-019 | `POST /clubs/{clubId}/teams/request` — submit for approval (Coach via club link) | TM-012 |
| TM-020 | `GET /teams/{id}` — detail | TM-018 |
| TM-021 | `PATCH /teams/{id}` — edit name/description | TM-012, TM-020 |
| TM-022 | `POST /teams/{id}/archive` + `unarchive` | TM-011, TM-020 |
| TM-023 | `DELETE /teams/{id}` — hard delete; attendance data preserved | TM-011, TM-020 |
| TM-024 | `POST /teams/{id}/approve` + `reject` (with optional reason) — notify coach by email | TM-009, TM-011, TM-019 |

## Phase 2 — Member & Role Endpoints

| ID | Task | Deps |
|---|---|---|
| TM-025 | `GET /teams/{id}/members` — roster; group roles per user in response | TM-012 |
| TM-026 | `POST /teams/{id}/members/{userId}/roles` — add role | TM-011, TM-025 |
| TM-027 | `DELETE /teams/{id}/members/{userId}/roles/{role}` — remove role; enforce last-coach guard (409) | TM-011, TM-025 |
| TM-028 | `DELETE /teams/{id}/members/{userId}` — remove all roles | TM-011, TM-025 |
| TM-029 | `DELETE /users/me/teams/{teamId}` — player leaves | TM-020 |

## Phase 2 — Player Profile Endpoints

| ID | Task | Deps |
|---|---|---|
| TM-030 | `GET` + `PATCH /teams/{id}/members/{userId}/profile` — jersey number / position | TM-025 |

## Phase 2 — Invite Endpoints

| ID | Task | Deps |
|---|---|---|
| TM-031 | Implement `generateToken()` utility — `SecureRandom` + Base64-URL, 32 bytes | — |
| TM-032 | `POST /teams/{id}/invites` — generate token; send invite email; return invite | TM-009, TM-031, TM-012 |
| TM-033 | `GET /teams/{id}/invites` — list pending | TM-012 |
| TM-034 | `DELETE /teams/{id}/invites/{inviteId}` — revoke (set `status = revoked`) | TM-012 |
| TM-035 | `GET /invites/{token}` (public, no auth) — resolve context; 410 if expired/revoked | TM-031 |
| TM-036 | `POST /invites/{token}/accept` — validate; upsert `team_memberships`; handle unregistered user | TM-035 |
| TM-037 | `GET /clubs/{id}/coach-link` — current active link + expiry | TM-011 |
| TM-038 | `POST /clubs/{id}/coach-link` — rotate: revoke current, insert new; `expires_in_days: 7 | 14` | TM-011, TM-031 |
| TM-039 | `DELETE /clubs/{id}/coach-link` — revoke without replacement | TM-011 |
| TM-040 | `GET /club-links/{token}` (public) — resolve club context; 410 if expired/revoked | TM-031 |
| TM-041 | `POST /club-links/{token}/join` — create membership; trigger team request flow | TM-040, TM-019 |

## Phase 2 — Email Templates

| ID | Task | Deps |
|---|---|---|
| TM-042 | Invite email (plain text + HTML): "You've been invited to [Team] as [role]" | TM-009 |
| TM-043 | Club coach link email: "Join [Club] as a coach" | TM-009 |
| TM-044 | Team approval notification email to coach | TM-009 |
| TM-045 | Team rejection notification email to coach (with optional reason) | TM-009 |

## Phase 3 — KMP Domain Layer

| ID | Task | Deps |
|---|---|---|
| TM-046 | Define `ClubRepository` interface + `ClubRepositoryImpl` in `commonMain` | TM-013 |
| TM-047 | Define `TeamRepository` interface + `TeamRepositoryImpl` | TM-018 |
| TM-048 | Define `MembershipRepository` interface + `MembershipRepositoryImpl` | TM-025 |
| TM-049 | Define `InviteRepository` interface + `InviteRepositoryImpl` | TM-032 |
| TM-050 | Implement `hasRole(teamId, role): Boolean` shared utility | TM-048 |
| TM-051 | Wire Koin platform modules: `AndroidSqliteDriver` (androidMain), `NativeSqliteDriver` (iosMain) | TM-008 |

## Phase 4 — Mobile UI

| ID | Task | Deps |
|---|---|---|
| TM-052 | **S1 Club Setup screen** — form: name, sport type, location, logo picker (circular crop) | TM-046 |
| TM-053 | **S2 Club Dashboard** — teams list with member count + coach avatars; pending section; FAB; "Invite Coaches" button | TM-047 |
| TM-054 | **S3 Team Detail — Roster tab** — coach section + player section; role chips; swipe-remove; tap → profile | TM-048 |
| TM-055 | **S3 Team Detail — Settings + Sub-groups tabs** (Sub-groups UI delegated to event-scheduling) | TM-047 |
| TM-056 | **S4 Club Profile Edit** — pre-filled form; logo replaceable | TM-046 |
| TM-057 | **S5 Team Edit Sheet** — name + description bottom sheet | TM-047 |
| TM-058 | **S6 Invite Sheet** — email input + copy link + pending invites list + expiry note | TM-049 |
| TM-059 | **S6b Club Coach Invite Sheet** — copy link + email + pending coach list | TM-049 |
| TM-060 | **S7 Player Profile** — avatar, jersey, position, role chips, contact info, attendance stats stub | TM-048, TM-050 |
| TM-061 | **S8 Coach First-Team Setup** — "Submit for Approval" screen post-signup | TM-047 |
| TM-062 | **F9 Invite link deep link handling** — `playbook://join/{token}` → confirm sheet → register if needed | TM-049 |
| TM-063 | **F13 Pending team approval** — ClubManager approve/reject with optional reason | TM-047 |
| TM-064 | Role chip components (Gold=ClubManager, Blue=Coach, Grey=Player) | — |
| TM-065 | Invite state badge components (Pending/Accepted/Expired/Revoked) | — |
| TM-066 | Multiple-role display: user appears in all applicable roster sections with all role chips | TM-054, TM-064 |
