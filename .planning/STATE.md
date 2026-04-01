---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: unknown
last_updated: "2026-04-01T11:44:49.294Z"
progress:
  total_phases: 8
  completed_phases: 6
  total_plans: 31
  completed_plans: 31
---

# STATE.md — Playbook

## Current State

- **Active phase:** Phase 4 — Attendance Tracking (COMPLETE)
- **Mode:** YOLO
- **Last updated:** 2026-03-24
- **Last session:** 2026-04-01T11:39:28.569Z

## Phase Status

| Phase | Status | Started | Completed |
|---|---|---|---|
| 1 — Foundation + Auth | ✅ Done | 2026-03-11 | 2026-03-19 |
| 2 — Team Management | ✅ Done | 2026-03-11 | 2026-03-19 |
| 3 — Event Scheduling | ✅ Done | 2026-03-19 | 2026-03-24 |
| 4 — Attendance Tracking | ✅ Done | 2026-03-24 | 2026-03-24 |
| 5 — Notifications | 🔲 Not started | — | — |
| 6 — Super Admin | 🔲 Not started | — | — |

## What's Actually Done

### Phase 1 — Foundation + Auth ✅

- ✅ KMP monorepo scaffolded (shared, composeApp, androidApp, iosApp, server, admin)
- ✅ Ktor server: DB (PostgreSQL + Flyway), JWT auth, register/login/logout
- ✅ Role system in DB (Coach, Player, ClubManager, SuperAdmin)
- ✅ Android + iOS apps build and run, login flow verified in simulator
- ✅ Shared KMP code: ViewModels, screens, navigation (Nav3 CMP), Koin DI
- ✅ Auth screens: Login, Register working on Android + iOS
- ✅ CI/CD: Test Suite + Deploy Android pipelines, iOS XCTest smoke tests

### Phase 2 — Team Management ✅

- ✅ Backend: clubs, teams, invite system, role assignments, sub-groups (DB + API)
- ✅ Flyway migrations V1–V6
- ✅ Android UI: EmptyState, ClubSetup, TeamRoster, Invite screens + ViewModels
- ✅ UI + E2E tests for all team management flows
- ✅ GitHub Actions: CI build check + Updraft deploy workflow
- ⚠️ Updraft deploy needs `UPDRAFT_APP_ID` + `UPDRAFT_API_KEY` secrets added to GitHub

### Phase 3 — Event Scheduling ✅

- ✅ 03-00: Wave 0 test stubs — 9 stub files across shared/server/composeApp
- ✅ 03-01: Event DB foundation — V7 migration + Exposed tables + EventRepository (interface + impl)
- ✅ 03-03: Shared KMP contracts — Event domain models, EventRepository interface, navigation screens, kizitonwose calendar in catalog
- ✅ 03-02: Event API routes — 7 event endpoints + GET /teams/{teamId}/subgroups + background materialisation job + 9 integration tests
- ✅ 03-04: KMP EventRepositoryImpl (Ktor) + EventCacheManager (SQLDelight) + DatabaseDriverFactory expect/actual + Koin wiring
- ✅ 03-05: EventListScreen + EventDetailScreen — filterable event list, full detail (7 sections), coach role detection via UserPreferences, detailRefreshTrigger in navigation
- ✅ 03-06: CreateEditEventScreen (S4) + RecurringPatternSheet (S5) + RecurringScopeSheet (S6) — full create/edit form, recurring config, scope sheet, navigation wired, Koin registered
- ✅ 03-07: Calendar screen with kizitonwose month + week views

### Phase 4 — Attendance Tracking ✅

- ✅ 04-01: Attendance DB — V8 migration (attendance_responses, check_in_records, abwesenheit_rules, pending_mutations)
- ✅ 04-02: Attendance API routes — PUT /events/{id}/attendance/me, GET /events/{id}/attendance, coach check-in override, deadline enforcement
- ✅ 04-03: Abwesenheit routes — CRUD + recurring/period rules, backfill job, auto-decline materialisation
- ✅ 04-04: AttendanceStatsCalculator (KMP shared) + AttendanceCacheManager (SQLDelight) + AttendanceRepositoryImpl (Ktor)
- ✅ 04-05: EventDetailViewModel + EventListViewModel — attendance loading, RSVP submission, check-in entries, coach override
- ✅ 04-06: AttendanceRsvpButtons, BegrundungSheet, CoachOverrideSheet, MemberResponseList, ResponseDeadlineLabel, PlayerProfileScreen stats, AbsenceCard, AddAbsenceSheet
- ✅ 04-07: CoachOverrideSheet wired end-to-end in EventDetail; integration tests for attendance + abwesenheit; stats unit tests; coach auth enforcement on check-in route

## Decisions

- Used `kotlin.test.@Ignore` for shared/server stubs (consistent with existing test convention)
- Used `org.junit.@Ignore` for androidTest stubs (JUnit 4 standard for Android instrumented tests)
- Created `composeApp/src/androidTest/` with `androidInstrumentedTest` source set (VALIDATION.md targets `connectedAndroidTest`)
- [Phase 03-event-scheduling]: Used enumerationByName for EventType/EventStatus/PatternType to store as TEXT matching V7 CHECK constraints
- [Phase 03-event-scheduling]: Used array<Short> for weekdays column — confirmed supported in Exposed 0.54.0 via resolveColumnType
- [Phase 03-event-scheduling]: kotlinx.datetime.Instant used for all timestamps (not java.time) — required for KMP iOS compilation
- [Phase 03-event-scheduling]: kizitonwose-calendar added to version catalog only; Plan 07 adds to build.gradle.kts
- [Phase 03-event-scheduling]: EditEventWithScope route-local wrapper keeps scope deserialization out of domain model
- [Phase 03-event-scheduling]: Custom KSerializer objects for java.time types — explicit, no SerializersModule needed
- [Phase 03-event-scheduling]: SQLDelight 2.0.2 generates CachedEvent in package ch.teamorg (not ch.teamorg.db); DatabaseDriverFactory expect/actual pattern for Android+iOS
- [Phase 03-event-scheduling]: Offline fallback covers ConnectTimeoutException, HttpRequestTimeoutException, IOException only; ResponseException (4xx/5xx) propagates to caller
- [Phase 03-event-scheduling]: Used UserPreferences.getUserId() for coach role detection — avoids extra getMe() API call since userId is cached at login
- [Phase 03-event-scheduling]: kotlinx-datetime added as direct composeApp dependency — shared module uses implementation (not api) so not transitive
- [Phase 03-event-scheduling]: Local val captures for nullable Event fields — required for Kotlin smart cast from cross-module public API
- [Phase 03-event-scheduling]: Local val capture for nullable RecurringPatternState required for Kotlin smart cast across module boundary
- [Phase 03-event-scheduling]: detailRefreshTrigger incremented in EditEvent onSaved and EventDetail onCancel to cover all write paths
- [Phase 03-event-scheduling]: kizitonwose downgraded 2.10.0→2.7.0: v2.10.0 references kotlinx.datetime.YearMonth (0.7.x only) causing unlinked iOS symbols; v2.7.0 uses library own YearMonth type, iOS links cleanly
- [Phase 03-event-scheduling]: Multi-day event spanning in CalendarViewModel (not UI): iterates startDate..endDate inclusive, adds event to each date in eventsByDate map
- [Phase 02-team-management]: getMyRoles() replaces roster-scan: one API call, correctly detects club_manager role
- [Phase 02-team-management]: CalendarViewModel injected with teamRepository + userPreferences for checkCoachRole()
- [Phase 02]: TeamsListViewModel derives clubId from getMyRoles() clubRoles — avoids storing clubId separately
- [Phase 02]: TeamEditSheet reused for both create (TeamsListScreen) and edit (TeamRosterScreen)
- [Phase 02-team-management]: leaveTeam uses DELETE /teams/{teamId}/leave (separate from DELETE /members/{userId} which is coach-only remove)
- [Phase 02-team-management]: SubGroup domain model moved to Event.kt by Plan 02-09; shared TeamRepository imports from ch.teamorg.domain.SubGroup (same package)
- [Phase 02-team-management]: SubGroupResponse data class added to SubGroupRoutes — mapOf with mixed String/Long types caused kotlinx.serialization crash
- [Phase 02-team-management]: getClub() called alongside getClubTeams() in loadTeams() — no separate trigger needed
- [Phase 02-team-management]: ClubEditSheet added as private composable in TeamsListScreen.kt — too small for separate file
- [Phase 02-team-management]: uploadAvatar placed in TeamRepository (not a new repo) — user-scoped but TeamRepo already has getMyRoles() pattern
- [Phase 02-team-management]: expect/actual rememberImagePickerLauncher: Android GetContent, iOS UIImagePickerController — avoids peekaboo/mpfilepicker dependency
- [Phase 04-attendance-tracking]: AttendanceStats not @Serializable — client-side computed only (ADR-007)
- [Phase 04-attendance-tracking]: AbwesenheitRule uses String for date fields to avoid kotlinx-datetime LocalDate serialization complexity
- [Phase 04-attendance-tracking]: pending_mutation table uses AUTOINCREMENT id for stable ordering and deletion
- [Phase 04-attendance-tracking]: Used text() column for attendance_responses.status — declined-auto and no-response have hyphens, invalid Kotlin enum identifiers
- [Phase 04-attendance-tracking]: team_roles.user_id FK changed CASCADE -> SET NULL (TM-19): column made nullable to preserve historical attendance data
- [Phase 04-attendance-tracking]: AbwesenheitBackfillJob.enqueue() is non-suspend — takes Application scope, launches coroutine internally
- [Phase 04-attendance-tracking]: AutoPresentJob checks existing records before insert to preserve coach overrides (no ON CONFLICT needed)
- [Phase 04]: AttendanceCacheManager.deleteRule() added (not in plan spec) for AbwesenheitRepositoryImpl cache consistency
- [Phase 04-05]: EventListItem compact RSVP buttons read-only — user taps card to navigate to detail to respond
- [Phase 04-05]: loadAttendanceCounts fetches per-event serially — acceptable for MVP list size
- [Phase 04-attendance-tracking]: Icons.Outlined.MenuBook kept (not AutoMirrored) — deprecation warning only, consistent with codebase
- [Phase 04-attendance-tracking]: BodyPartGrid uses two Rows (not LazyVerticalGrid) — simpler for fixed 2x5 layout
- [Phase 04-07]: Coach role enforcement added to PUT /events/{id}/check-in/{userId}
- [Phase 04-07]: onOverrideTap opens CoachOverrideSheet via state pattern instead of calling submitOverride directly
- [Phase 04.1-02]: EventRepository injected as 5th param to PlayerProfileViewModel; eventTypes map built at loadStats time from getMyEvents()
- [Phase 04.1-02]: Profile tab resolves teamId via getMyRoles().teamRoles.firstOrNull() — consistent with CalendarViewModel pattern
- [Phase 04.1-01]: Server-side DTOs for CheckInEntry defined in AttendanceRepository.kt co-located with domain contracts
- [Phase 04.1-01]: getCheckInEntries uses three separate queries merged in memory (simpler than multi-table LEFT JOIN with alias collisions)
- [Phase 04.1-01]: kotlinx-datetime added to server build.gradle.kts for @Serializable Instant fields in response DTOs
- [Phase 05-notifications]: createdAt stored as ISO-8601 String in Notification domain model — avoids kotlinx-datetime serialization complexity
- [Phase 05-notifications]: NotificationRepositoryImpl uses relative URL paths — base URL configured in HttpClientFactory, consistent with all other repos
- [Phase 05-notifications]: Used timestamp() not timestampWithTimeZone() in NotificationTables — not in exposed-java-time 0.54.0, TIMESTAMPTZ handled by PostgreSQL JDBC transparently
- [Phase 05-notifications]: insertIgnore used for dedup in NotificationRepository.createNotification/createBatch — maps to INSERT IGNORE ON CONFLICT DO NOTHING via Exposed
- [Phase 05-notifications]: ktor-client-cio added to server production deps for PushServiceImpl HttpClient — CIO engine is JVM-native
- [Phase 05-notifications]: PushRegistration expect/actual: Android calls OneSignal.login/logout; iOS+JVM no-op (iOS OneSignal SDK is native Swift, not accessible from KMM)
- [Phase 05-notifications]: ONESIGNAL_APP_ID read from onesignal.appId local.properties via findProperty() in BuildConfig
- [Phase 05-notifications]: iOS OneSignal SPM package requires manual Xcode setup; placeholder string in iOSApp.swift marks substitution point
- [Phase 05-notifications]: call.application.launch(Dispatchers.IO) used in route handlers — top-level launch() is deprecated in Ktor route context
- [Phase 05-notifications]: Reminder row management methods added to NotificationRepository interface — insertReminderRows, deleteReminderRowsForEvent, getDueReminders, getCoachIdsForTeam, getEventAttendanceSummary, getUpcomingEventsForCoachSummary
- [Phase 05-notifications]: TeamRoleEntry has no teamName field — used teamId as display label in team picker
- [Phase 05-notifications]: EventDetailViewModel: NotificationRepository injected as 5th param; reminder loaded in loadEvent flow
- [Phase 05-notifications]: fireCoachSummaries made internal for direct test invocation
- [Phase 05-notifications]: AbwesenheitRoutes had destructure bug (clubId sent as teamId) — fixed to roleTriple.first
- [Phase 05.1]: flushQueue called inside existing LaunchedEffect(authState) — fires on auth change and foreground resume without extra observer
- [Phase 05.1]: GET /events/{id}/check-in role check mirrors exact PUT pattern (getUserTeamRoles + getUserClubRoles)
- [Phase 05.1-02]: ClubRepository injected as 3rd param to NotificationSettingsViewModel; team names fetched per distinct clubId via getClubTeams
- [Phase 05.1-02]: CalendarScreen.kt and CalendarViewModel.kt deleted; Screen.Calendar case retained in AppNavigation using EventListScreen+EventViewMode.CALENDAR

## Notes

- CI budget exhausted until ~2026-04-01 — work on feature branches, only merge to main when ready
- Last session: 2026-03-24 — Phase 3 complete, starting Phase 4 (Attendance Tracking)
