---
template: plan
version: 0.1.0
gate: READY
---
# Implementation Plan: Team Management

> **Implement first** — provides foundational entities (clubs, teams, users, roles) that all other features depend on.

---

## Phase 1 — Database Schema

- [ ] Create `clubs` table (id, name, logo_url, sport_type, location, status, timestamps)
- [ ] Create `teams` table (id, club_id, name, description, status, requested_by, rejection_reason, timestamps)
- [ ] Create `team_memberships` table (id, team_id, user_id, role, added_by, joined_at) + unique `(team_id, user_id, role)`
- [ ] Create `player_profiles` table (team_id, user_id, jersey_number, position) — PK `(team_id, user_id)`
- [ ] Create `invites` table (id, invite_type, team_id, role, invited_email, invite_token, status, invited_by, expires_at, accepted_at)
- [ ] Create `club_coach_links` table (id, club_id, token, expires_at, created_by, created_at, revoked_at)
- [ ] Add indexes: `invite_token` (unique), `(invited_email, team_id)`, `(team_id, user_id)` on memberships

## Phase 2 — Ktor Backend

### Infrastructure
- [ ] Configure Koin: shared module (repositories) + server module (DB, mailer)
- [ ] Add Simple Java Mail `Mailer` singleton in server Koin module; config from env vars
- [ ] Add `staticFiles("/logos", File("uploads/logos"))` route for logo serving
- [ ] Add `requireClubManager()` and `requireCoachOnTeam()` middleware helpers

### Club endpoints
- [ ] `POST /clubs` — create club
- [ ] `GET /clubs/{id}` — club detail
- [ ] `PATCH /clubs/{id}` — edit name/sport/location
- [ ] `POST /clubs/{id}/logo` — multipart upload; validate MIME type; write to `uploads/logos/`; update `logo_url`

### Team endpoints
- [ ] `GET /clubs/{clubId}/teams` — list (active + archived + pending)
- [ ] `POST /clubs/{clubId}/teams` — create (ClubManager)
- [ ] `POST /clubs/{clubId}/teams/request` — submit for approval (Coach)
- [ ] `GET /teams/{id}` — detail
- [ ] `PATCH /teams/{id}` — edit name/description
- [ ] `POST /teams/{id}/archive` / `unarchive`
- [ ] `DELETE /teams/{id}` — delete; preserve attendance data
- [ ] `POST /teams/{id}/approve` — ClubManager approves pending
- [ ] `POST /teams/{id}/reject` — with optional reason

### Member & role endpoints
- [ ] `GET /teams/{id}/members` — roster with roles grouped per user
- [ ] `POST /teams/{id}/members/{userId}/roles` — add role
- [ ] `DELETE /teams/{id}/members/{userId}/roles/{role}` — remove role; enforce last-coach guard (409)
- [ ] `DELETE /teams/{id}/members/{userId}` — remove all roles
- [ ] `DELETE /users/me/teams/{teamId}` — player leaves

### Player profile endpoints
- [ ] `GET /teams/{id}/members/{userId}/profile`
- [ ] `PATCH /teams/{id}/members/{userId}/profile` — jersey number / position

### Invite endpoints (one-time)
- [ ] `POST /teams/{id}/invites` — generate token via `SecureRandom + Base64`; send invite email
- [ ] `GET /teams/{id}/invites` — list pending
- [ ] `DELETE /teams/{id}/invites/{inviteId}` — revoke
- [ ] `GET /invites/{token}` (public) — resolve invite context; 410 if expired/revoked
- [ ] `POST /invites/{token}/accept` — validate; create membership; handle unregistered users

### Club coach link endpoints
- [ ] `GET /clubs/{id}/coach-link` — current active link + expiry
- [ ] `POST /clubs/{id}/coach-link` — rotate: revoke current, create new; `expires_in_days` param
- [ ] `DELETE /clubs/{id}/coach-link` — revoke without replacement
- [ ] `GET /club-links/{token}` (public) — resolve club context; 410 if expired/revoked
- [ ] `POST /club-links/{token}/join` — create membership + trigger team request flow

### Email
- [ ] Invite email template (plain text + HTML): "You've been invited to [Team] as [role]"
- [ ] Club coach link email: "Join [Club] as a coach"
- [ ] Team approval/rejection notification email to coach

## Phase 3 — KMP Domain Layer

- [ ] Define `ClubRepository` interface + `ClubRepositoryImpl` in `commonMain`
- [ ] Define `TeamRepository` interface + `TeamRepositoryImpl`
- [ ] Define `MembershipRepository` interface + `MembershipRepositoryImpl`
- [ ] Define `InviteRepository` interface + `InviteRepositoryImpl`
- [ ] Implement `hasRole(teamId: UUID, role: Role): Boolean` shared utility (used by all features)
- [ ] Wire platform drivers (SQLDelight `NativeSqliteDriver` / `AndroidSqliteDriver`) via Koin platform modules

## Phase 4 — Mobile UI (CMP)

- [ ] **S1 Club Setup screen** — full-screen form; logo picker (circular crop)
- [ ] **S2 Club Dashboard** — teams list; pending section; "Invite Coaches" button; FAB
- [ ] **S3 Team Detail** — tabbed: Roster / Sub-groups / Statistics / Settings
  - Roster tab: coach section + player section; role chips; swipe-remove; tap → profile
- [ ] **S4 Club Profile Edit** — pre-filled; logo replaceable
- [ ] **S5 Team Edit Sheet** — name + description
- [ ] **S6 Invite Sheet** — email input + copy link + pending invites list
- [ ] **S6b Club Coach Invite Sheet** — copy link + email
- [ ] **S7 Player Profile** — avatar, jersey, position, role chips, contact info, attendance summary
- [ ] **S8 Coach First-Team Setup** — "Submit for Approval" flow
- [ ] **F9 Invite link handling** — deep link → confirm join sheet → register if needed
- [ ] **F13 Pending team approval** — ClubManager approve/reject flow
- [ ] Role chips (Gold/Blue/Grey) + invite state badges (Pending/Accepted/Expired/Revoked)
- [ ] Multiple-role display (user appears in all applicable roster sections)
