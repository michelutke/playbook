---
template: handover
version: 0.1.0
status: DONE
---
# Handover: Team Management

## What Was Built

Full club and team management backend + Android UI covering:
- Club creation, editing, logo upload (multipart, self-hosted file storage)
- Teams: create, archive, delete, approve/reject pending teams
- Roles: `player`, `coach`, `club_manager` — stored in `team_memberships`, checked DB-per-request (not JWT)
- One-time email invites (team player + team coach) — SecureRandom token, expires 7d
- Reusable club coach links — rotatable, multiple coaches can join via same link
- Player profiles: jersey number, position (per-team context)
- Last-coach guard: 409 on removing final coach from a team
- Sub-group management (owned here, consumed by event-scheduling)
- Email notifications: invite, coach link, team approval/rejection

## Architecture Decisions

| Decision | Outcome |
|---|---|
| Role storage | `team_memberships` rows per role; DB-checked per request — immediate effect, no re-login needed |
| Invite tokens | `SecureRandom` + URL-safe Base64 (32 bytes = 43 chars, ~256-bit entropy) |
| Coach links | Reusable until `expires_at` or `revoked_at`; rotation = `revoked_at` + new row |
| Logo storage | Self-hosted filesystem; served via `staticFiles()`; `logo_url` stores relative path |
| Email delivery | Simple Java Mail 8.3.4 via `Dispatchers.IO`; `Mailer` singleton in Koin |
| DI | Koin 4.0.0: `sharedModule` (repositories) + `serverModule` (DB, mailer) |

## Key Files

```
backend/src/main/kotlin/ch/teamorg/
  routes/ClubRoutes.kt
  routes/TeamRoutes.kt
  routes/MemberRoutes.kt
  routes/InviteRoutes.kt
  routes/CoachLinkRoutes.kt
  email/InviteEmail.kt
  email/CoachLinkEmail.kt
  email/TeamApprovalEmail.kt
  di/ServerModule.kt
  di/SharedModule.kt

shared/src/commonMain/.../domain/
  Club.kt, Team.kt, TeamMembership.kt, PlayerProfile.kt
  Invite.kt, ClubCoachLink.kt

shared/src/commonMain/.../repository/
  ClubRepository.kt, TeamRepository.kt
  MembershipRepository.kt, InviteRepository.kt
```

## Migrations

`backend/src/main/resources/db/migrations/V1__*.sql` through `V10__*.sql`
- V9: rejected team status field
- V10: `uq_pending_team_per_coach` index

## Post-Completion Fixes

Security/correctness fixes committed after feature completion (commit `d92b4ff`).

## Known Limitations

- Logo storage is local filesystem — not suitable for multi-instance deploy without shared volume or object storage migration
- No invite resend endpoint (revoke + re-invite is the workaround)
- Web UI deferred (ADR-001 pending)

## Downstream Dependencies

All other features depend on entities from this module:
- `clubs`, `teams`, `team_memberships`, `player_profiles` used by every feature
- `hasRole(teamId, role)` shared utility used across all feature guards
- `subgroups` / `subgroup_members` consumed by event-scheduling
