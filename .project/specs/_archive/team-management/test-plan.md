---
template: test-plan
version: 0.1.0
---
# Test Plan: Team Management

## Unit Tests

### Backend (Ktor / JVM)

| # | Subject | Cases |
|---|---|---|
| U-01 | `generateToken()` | produces 43-char URL-safe string; two calls produce different tokens; no padding chars |
| U-02 | `requireClubManager()` | passes for valid SA session; 403 for non-club-manager; 403 for different club |
| U-03 | `requireCoachOnTeam()` | passes for coach role; 403 for player-only; 403 for different team |
| U-04 | Last-coach guard | `DELETE .../roles/coach` → 409 when last coach; succeeds when ≥2 coaches remain |
| U-05 | Invite token expiry | `GET /invites/{token}` → 410 when `expires_at` < now; 200 when valid |
| U-06 | Invite token revoke | `GET /invites/{token}` → 410 after `status = revoked` |
| U-07 | Duplicate invite | `POST /teams/{id}/invites` → 409 when email already active member with same role |
| U-08 | Club coach link rotation | `POST /clubs/{id}/coach-link` sets `revoked_at` on previous; new token differs |
| U-09 | Logo MIME validation | `POST /clubs/{id}/logo` → 415 for `application/pdf`; 200 for `image/png` |
| U-10 | Team delete preserves attendance | `DELETE /teams/{id}` — attendance rows remain in DB; team status set to deleted |
| U-11 | `hasRole()` utility | returns true for matching role; false for non-member; false for wrong role |

### KMP Domain (shared)

| # | Subject | Cases |
|---|---|---|
| U-12 | `MembershipRepository` | returns all roles for a user on a team; empty list for non-member |
| U-13 | `hasRole()` | delegates to repository; correct boolean for coach vs player |

---

## Integration Tests

| # | Flow | Assertions |
|---|---|---|
| I-01 | Create club → invite coach → coach accepts via link → coach appears in roster | Club created; invite pending; after accept: `team_memberships` row with `role=coach`; `invites.status=accepted` |
| I-02 | Coach submits team request → ClubManager approves | Team status: `pending` → `active`; coach notified by email |
| I-03 | Coach submits team request → ClubManager rejects | Team status: `pending` → removed; rejection email sent with reason |
| I-04 | Add player to team → promote to coach → remove coach role | Player row + coach row in `team_memberships`; after remove coach: player row remains |
| I-05 | Attempt to remove last coach | 409 response; `team_memberships` unchanged |
| I-06 | Player leaves team | `team_memberships` rows deleted; attendance data preserved |
| I-07 | Rotate club coach link | Old token → 410; new token → 200 with club context |
| I-08 | Upload logo → serve via static route | `clubs.logo_url` updated; `GET /logos/{file}` returns image with correct content type |
| I-09 | Unregistered email follows invite link | Redirect to registration; after registration: `team_memberships` row created |
| I-10 | Multi-role user in roster | User appears in both Coach section and Player section with both chips |

---

## Edge Case Tests

| # | Scenario | Expected |
|---|---|---|
| E-01 | Invite link expired (7 days passed) | Landing screen shows "Link expired" message |
| E-02 | Same email invited twice to same team | Second `POST` returns 409 "Already invited" |
| E-03 | ClubManager adds Coach role to self | Allowed; both ClubManager + Coach chips shown |
| E-04 | Delete team with active events | Confirm dialog shown; after confirm: team deleted, events + attendance preserved |
| E-05 | Archive team | Team hidden from active list; data read-only; can be unarchived |
| E-06 | Coach signs up via club link, skips team creation | Prompted to create team on next login |

---

## Manual / E2E Tests

| # | User Journey | Steps |
|---|---|---|
| M-01 | Full club onboarding | SA creates club → CM creates teams → invites coaches → coaches accept → invite players → players join |
| M-02 | Invite link on mobile | Copy link on web → open on iOS simulator → confirm join → appear in roster |
| M-03 | Role chip display | User with Player + Coach roles: verify both sections in roster, both chips on profile |
| M-04 | Logo upload + display | Upload JPEG → verify circular crop preview → verify logo shown in club header |
| M-05 | Deep link handling | `playbook://join/{token}` — cold start app; confirm join flow; warm start app |
