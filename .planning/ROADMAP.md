# ROADMAP.md — Playbook

**6 phases** | **67 requirements mapped** | Each phase ships a working product

---

## Phase 1 — Foundation + Auth
**Goal:** Runnable app on Android + iOS with working auth flow. The skeleton everything else builds on.

**Requirements:** AUTH-01–06

**Deliverables:**
- KMP monorepo scaffolded (shared, composeApp, androidApp, iosApp, server, admin)
- Material 3 theme wired with custom design tokens (dark + light)
- Navigation3 shell with placeholder screens
- Ktor server bootstrapped (DB, Flyway, JWT auth)
- Register, login, logout flows — fully tested
- Role system in place (Coach, Player, ClubManager, SuperAdmin — DB-checked per request)
- SQLDelight schema initialized

**Success criteria:**
1. App runs on Android and iOS without crash
2. User can register, log in, and log out — verified by automated test
3. JWT issued; role loaded from DB on each protected request
4. Theme matches design tokens (dark mode default)
5. CI runs tests on every commit

---

## Phase 2 — Team Management
**Goal:** Clubs and teams work. Coaches and players can be invited and manage their rosters.

**Requirements:** TM-01–19

**Deliverables:**
- Club setup flow (ClubManager creates club, logo upload)
- Team creation + editing
- Invite system (email + shareable link, 7-day expiry)
- Role assignment: coach → team, player → team
- Sub-groups within teams
- Player profiles (avatar, jersey, position, contact)
- Team roster screen
- Automated tests for all invite + membership flows

**Success criteria:**
1. ClubManager can create a club and a team
2. Coach invited by email can accept and manage the team
3. Player can be invited, join, and leave a team
4. Sub-groups created and players assigned
5. All membership flows covered by tests

---

## Phase 3 — Event Scheduling
**Goal:** Coaches can create, edit, and cancel events. Players see and filter their schedule.

**Requirements:** ES-01–16

**Plans:** 3/4 plans executed

Plans:
- [ ] 03-00-PLAN.md -- Wave 0 test stubs (Nyquist compliance)
- [ ] 03-01-PLAN.md — Backend data foundation (migration, Exposed tables, EventRepository)
- [ ] 03-02-PLAN.md — Server API routes + materialisation job + integration tests
- [ ] 03-03-PLAN.md — Shared KMP contracts (domain models, repository interface, screen routes, calendar dep)
- [ ] 03-04-PLAN.md — Shared KMP repository impl + SQLDelight offline cache
- [ ] 03-05-PLAN.md — Event list screen + event detail screen + navigation wiring
- [ ] 03-06-PLAN.md — Create/edit event form + recurring/scope bottom sheets
- [ ] 03-07-PLAN.md — Calendar screen with kizitonwose month + week views

**Deliverables:**
- Event creation form (all types, multi-day, recurring)
- Recurring event server-side expansion
- Edit/cancel flows (this / this+future / all for recurring)
- Event duplication
- Event list (chronological, filterable)
- Calendar view (month + week)
- Sub-group targeting for events
- Timezone handling (UTC storage, local display)
- Automated tests: create, edit, cancel, recurrence

**Success criteria:**
1. Coach can create a one-off and a recurring event
2. Player sees events filtered by team and type
3. Calendar view renders correctly
4. Editing a recurring event with all 3 scope options works correctly
5. All event flows covered by tests

---

## Phase 4 — Attendance Tracking
**Goal:** Core attendance loop works end-to-end including offline, Abwesenheit, and coach overrides.

**Requirements:** AT-01–16

**Deliverables:**
- Per-event response UI (confirm / decline / unsure + Begründung)
- Response deadline enforcement (client UX + server 409)
- Abwesenheit: recurring weekly + period absence, auto-decline
- Manual coach override (present/absent/excused + note)
- Attendance list per event (all members + statuses)
- Attendance statistics (% presence, filters)
- Offline mutation queue + sync
- Audit log for coach overrides
- Automated tests: response flows, deadline enforcement, offline sync, Abwesenheit

**Success criteria:**
1. Player can confirm/decline/unsure; coach sees response immediately
2. Abwesenheit auto-declines events; manual override takes precedence
3. Late sync rejected with 409 (server authoritative)
4. Offline responses sync correctly when back online
5. Statistics correct for a player with mixed attendance history

---

## Phase 5 — Notifications
**Goal:** Players and coaches receive timely, relevant notifications. Users control what they get.

**Requirements:** NO-01–12

**Deliverables:**
- OneSignal integration (iOS + Android, behind PushService abstraction)
- Event notifications: new event, edits, cancellations
- Reminder scheduler (configurable lead time per user)
- Coach response notifications (per-response + summary modes)
- Abwesenheit change notifications to coach
- In-app notification inbox
- Per-user notification settings screen
- No-duplicate + removed-member guards
- Automated tests: notification triggers, settings, dedup

**Success criteria:**
1. Player receives push on new event and cancellation
2. Coach receives response notification (both modes configurable)
3. Reminder fires at configured lead time
4. In-app inbox shows all notifications without push enabled
5. No duplicate notifications sent

---

## Phase 6 — Super Admin
**Goal:** Platform operators can manage clubs and monitor the platform via the SvelteKit admin panel.

**Requirements:** SA-01–12

**Deliverables:**
- SvelteKit admin panel (fresh start, failed example as reference)
- Club management (create, deactivate, delete)
- ClubManager invites + removal
- Impersonation (1h limit, audit-logged, clearly shown in UI)
- Platform overview dashboard
- User search
- Immutable audit log viewer (DB role restricted — no UPDATE/DELETE)
- Automated tests: club CRUD, impersonation flow, audit log immutability

**Success criteria:**
1. SuperAdmin can create a club and invite a ClubManager
2. Impersonation session clearly indicated and auto-expires at 1h
3. All SA actions appear in audit log; log cannot be modified
4. Platform dashboard shows correct aggregates
5. All SA flows covered by tests

---

## Phase Order Rationale

Auth → Teams → Events → Attendance → Notifications → Admin

Each phase depends on the previous. At the end of each phase, the product is working and testable for everything shipped so far.

---

## Milestones

| Phase | Feature | Status |
|---|---|---|
| 1 | Foundation + Auth | ✅ Done |
| 2 | 3/4 | In Progress|  | 3 | 8/8 | Complete   | 2026-03-19 | 4 | Attendance Tracking | 🔲 Not started |
| 5 | Notifications | 🔲 Not started |
| 6 | Super Admin | 🔲 Not started |
