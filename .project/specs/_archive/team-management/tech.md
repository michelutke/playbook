---
template: tech
version: 0.1.0
gate: READY
---
# Tech Spec: Team Management

## Platform Scope

- Mobile (iOS + Android): KMP shared domain/data + CMP UI
- Web: deferred per ADR-001
- Role changes take effect immediately — roles checked from DB per request (not JWT claims)

---

## Data Model

### `clubs`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `name` | TEXT NOT NULL | |
| `logo_url` | TEXT NULLABLE | path in self-hosted file storage |
| `sport_type` | TEXT | |
| `location` | TEXT NULLABLE | |
| `status` | ENUM(`active`, `inactive`) | managed by SuperAdmin |
| `created_at` | TIMESTAMPTZ | |
| `updated_at` | TIMESTAMPTZ | |

### `teams`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `club_id` | UUID FK → clubs | |
| `name` | TEXT NOT NULL | |
| `description` | TEXT NULLABLE | |
| `status` | ENUM(`active`, `archived`, `pending`) | `pending` = awaiting ClubManager approval |
| `requested_by` | UUID FK → users NULLABLE | set for coach-submitted pending teams |
| `rejection_reason` | TEXT NULLABLE | |
| `created_at` | TIMESTAMPTZ | |
| `updated_at` | TIMESTAMPTZ | |

### `team_memberships`
One row per user per role per team (user with Coach + Player has two rows).

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `team_id` | UUID FK → teams | |
| `user_id` | UUID FK → users | |
| `role` | ENUM(`coach`, `player`) | |
| `added_by` | UUID FK → users | |
| `joined_at` | TIMESTAMPTZ | |

Unique constraint: `(team_id, user_id, role)`

### `player_profiles` (per-team context)
| Column | Type | Notes |
|---|---|---|
| `team_id` | UUID FK → teams | |
| `user_id` | UUID FK → users | |
| `jersey_number` | INT NULLABLE | |
| `position` | TEXT NULLABLE | |

PK: `(team_id, user_id)`

### `invites` (one-time email invites)
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `invite_type` | ENUM(`team_player`, `team_coach`) | |
| `team_id` | UUID FK → teams | |
| `role` | ENUM(`coach`, `player`) | role granted on acceptance |
| `invited_email` | TEXT NOT NULL | |
| `invite_token` | TEXT UNIQUE NOT NULL | single-use URL token |
| `status` | ENUM(`pending`, `accepted`, `expired`, `revoked`) | |
| `invited_by` | UUID FK → users | |
| `expires_at` | TIMESTAMPTZ | default `now() + 7 days` |
| `created_at` | TIMESTAMPTZ | |
| `accepted_at` | TIMESTAMPTZ NULLABLE | |

Index: `invite_token` (unique), `(invited_email, team_id)`

### `club_coach_links` (reusable club-scoped coach signup links)
Multiple coaches can sign up via the same link until it expires or is rotated.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `club_id` | UUID FK → clubs | |
| `token` | TEXT UNIQUE NOT NULL | |
| `expires_at` | TIMESTAMPTZ | 7 or 14 days; configurable per club |
| `created_by` | UUID FK → users | |
| `created_at` | TIMESTAMPTZ | |
| `revoked_at` | TIMESTAMPTZ NULLABLE | set when rotated |

One active link per club at a time (revoked when rotated). Expired/revoked links kept for audit.

---

## Role Model

Roles stored in `team_memberships`, checked from DB per request — not embedded in JWT.
This satisfies "role changes take effect immediately without requiring re-login."

| Role | Scope | Granted by |
|---|---|---|
| `player` | Team | Coach / ClubManager via invite |
| `coach` | Team | ClubManager via invite or promote |
| `club_manager` | Club | SuperAdmin (see super-admin spec) |

A user can hold multiple roles simultaneously. Removing a role never removes others.

**Last-coach guard:** server rejects removal of the last `coach` role on a team with 409.

---

## API — Ktor Backend

### Clubs
| Method | Path | Description |
|---|---|---|
| POST | `/clubs` | Create club |
| GET | `/clubs/{id}` | Club detail |
| PATCH | `/clubs/{id}` | Edit profile (name, sport type, location) |
| POST | `/clubs/{id}/logo` | Upload logo (`multipart/form-data`) |

### Teams
| Method | Path | Description |
|---|---|---|
| GET | `/clubs/{clubId}/teams` | List teams (active + archived + pending) |
| POST | `/clubs/{clubId}/teams` | Create team (ClubManager) |
| POST | `/clubs/{clubId}/teams/request` | Submit team for approval (Coach via club link) |
| GET | `/teams/{id}` | Team detail |
| PATCH | `/teams/{id}` | Edit name/description |
| POST | `/teams/{id}/archive` | Archive |
| POST | `/teams/{id}/unarchive` | Unarchive |
| DELETE | `/teams/{id}` | Delete; attendance data preserved |
| POST | `/teams/{id}/approve` | ClubManager approves pending team |
| POST | `/teams/{id}/reject` | ClubManager rejects; optional reason body |

### Members & Roles
| Method | Path | Description |
|---|---|---|
| GET | `/teams/{id}/members` | Roster with all roles per user |
| POST | `/teams/{id}/members/{userId}/roles` | Add role `{ role }` |
| DELETE | `/teams/{id}/members/{userId}/roles/{role}` | Remove role; 409 if last coach |
| DELETE | `/teams/{id}/members/{userId}` | Remove member (all roles) |
| DELETE | `/users/me/teams/{teamId}` | Player leaves team |

### Player Profiles
| Method | Path | Description |
|---|---|---|
| GET | `/teams/{id}/members/{userId}/profile` | Player profile in team context |
| PATCH | `/teams/{id}/members/{userId}/profile` | Update jersey number / position |

### One-time Invites
| Method | Path | Description |
|---|---|---|
| POST | `/teams/{id}/invites` | Invite by email; `{ email, role }` |
| GET | `/teams/{id}/invites` | List pending invites |
| DELETE | `/teams/{id}/invites/{inviteId}` | Revoke invite |
| GET | `/invites/{token}` | Resolve token → invite context (public, no auth) |
| POST | `/invites/{token}/accept` | Accept invite; creates membership |

### Club Coach Links (reusable)
| Method | Path | Description |
|---|---|---|
| GET | `/clubs/{id}/coach-link` | Current active link + expiry |
| POST | `/clubs/{id}/coach-link` | Generate or rotate link; body: `{ expires_in_days: 7 \| 14 }` |
| DELETE | `/clubs/{id}/coach-link` | Revoke without replacing |
| GET | `/club-links/{token}` | Resolve token → club context (public, no auth) |
| POST | `/club-links/{token}/join` | Register/login + enter coach onboarding flow |

Rotating (`POST`) marks current active link `revoked_at = now()` and creates a new token.

---

## Invite & Link Flows

### One-time email invite
- Single-use; `status` → `accepted` on first use; subsequent attempts → 410
- Expired token: 410 Gone; client shows "Link expired — ask your coach for a new one"
- Duplicate: 409 if already a member with that role

### Club coach link
- Reusable until `expires_at` or `revoked_at`
- Multiple coaches can follow and sign up independently
- On follow: unauthenticated users go to registration → auto-join on completion
- On join: coach enters team request flow (F12 in UX spec); team pending ClubManager approval

---

## Logo Storage

- Self-hosted file storage; served via internal file server
- `logo_url` stores relative path; resolved to full URL by client/API layer
- Upload: `POST /clubs/{id}/logo` with `multipart/form-data`; old file replaced

---

## KMP Architecture

- `ClubRepository`, `TeamRepository`, `MembershipRepository`, `InviteRepository` in shared KMP domain
- `hasRole(teamId, role): Boolean` shared domain utility — used across all features that gate on roles
- Sub-group management (`SubgroupRepository`) defined in event-scheduling module; referenced via FR-TM-14
