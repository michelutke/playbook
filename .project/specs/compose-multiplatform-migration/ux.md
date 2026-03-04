---
template: ux
version: 0.1.0
gate: READY
---
# UX Spec: Compose Multiplatform Migration

> This is a parity migration — no new UX. The existing Android app is the source of truth.
> This spec serves as a complete UX inventory to ensure nothing is lost during migration,
> and to call out iOS platform adaptations required.

## Cross-Platform UX Principle

CMP renders the same Compose UI on both Android and iOS. This means **iOS and Android users get an identical experience** — same screens, same layouts, same interaction patterns — without platform-specific UI code. The goal of this migration is to achieve that parity with zero visual regression on Android and a fully functional iOS app that looks and feels the same.

---

## Navigation Architecture

### App Shell
```
Scaffold
  ├─ BottomBar (visible only on ClubDashboard + NotificationInbox)
  │    ├─ Home tab → ClubDashboard
  │    └─ Notifications tab → NotificationInbox (badge: unread count, capped at 99+)
  └─ NavHost (full-screen stack)
```

### Auth Gate
```
App start
  ├─ token present → ClubDashboard(clubId)
  └─ no token     → Login
```

### Deep Links
```
playbook://invite?token={token}
  ├─ authenticated → InviteAccept(token)
  └─ not authenticated → Login → InviteAccept(token)
```

---

## User Flows

### F1 — First-time Coach Onboarding
```
Login (register)
  └─ ClubSetup (create club: name, sport)
       └─ CoachFirstTeamSetup (create first team)
            └─ ClubDashboard
```

### F2 — Returning User Login
```
Login (email + password)
  └─ ClubDashboard(clubId)
```

### F3 — Member Invite Accept
```
Deep link: playbook://invite?token=…
  ├─ [not logged in] Login → InviteAccept(token)
  └─ [logged in]     InviteAccept(token)
       └─ ClubDashboard
```

### F4 — Club Coach Invite Accept
```
Email link → ClubCoachInvite sheet
  └─ Accept → ClubDashboard (as coach)
```

### F5 — Team Management (Coach)
```
ClubDashboard
  └─ Team row tap → TeamDetail
       ├─ "⋯" → TeamEditSheet (edit name/sport)
       ├─ "Invite" → TeamInviteSheet (generate link)
       ├─ Member tap → PlayerProfile
       │    └─ PlayerStats (via stats tab)
       ├─ "Sub-groups" → SubgroupMgmt
       └─ "Stats" → TeamStats
```

### F6 — Event Flows
See event-scheduling UX spec (unchanged). Screens: EventList, EventCalendar, EventDetail, EventForm, SubgroupMgmt.

### F7 — Attendance Flows
See attendance-tracking UX spec (unchanged). Screens: AttendanceList, BegrundungSheet, MyAbsences, PlayerStats, TeamStats.

### F8 — Notifications
```
Bottom bar → NotificationInbox
  ├─ Notification tap → relevant deep destination (EventDetail, etc.)
  └─ Settings icon → NotificationSettings
       └─ Permission prompt → PushPermission (if not granted)
```

### F9 — Push Permission (first time)
```
NotificationSettings → PushPermission screen
  └─ "Allow" → OS permission dialog → back to NotificationSettings
```

---

## Screens

### Auth
| Screen | Key elements |
|--------|-------------|
| Login | Email + password fields, "Sign in" CTA, register link, inline error |
| ClubSetup | Club name field, sport field, "Create" CTA |
| CoachFirstTeamSetup | Team name + sport fields, "Create" CTA |
| InviteAccept | Invite summary (team name, role), "Accept" + "Decline" CTAs, loading/error states |

### Main Shell
| Screen | Key elements |
|--------|-------------|
| ClubDashboard | Club header, team list (rows: name + member count + role chip), FAB "+" (coach only) |
| NotificationInbox | Chronological notification list, read/unread state, tap to navigate |

### Team Management
| Screen | Key elements |
|--------|-------------|
| TeamDetail | Member list (avatar + name + role chip + status badge), action buttons |
| TeamEditSheet | Name + sport fields (bottom sheet) |
| TeamInviteSheet | Generated link + copy/share buttons (bottom sheet) |
| ClubEditScreen | Club name + sport fields |
| PlayerProfile | Player info, stats summary, absence summary |
| SubgroupMgmt | Group list (name + count), add/edit/delete |
| ClubCoachInviteSheet | Invite details, accept/decline (bottom sheet) |

### Events (see event-scheduling UX)
EventList, EventCalendar, EventDetail, EventForm, SubgroupMgmt

### Attendance (see attendance-tracking UX)
AttendanceList, BegrundungSheet, MyAbsences, PlayerStats, TeamStats

### Notifications
| Screen | Key elements |
|--------|-------------|
| NotificationInbox | List, unread badge, tap → deep link destination |
| NotificationSettings | Toggle switches per notification type |
| PushPermission | Explainer screen, "Allow" CTA → OS dialog |

### Shared Components
| Component | Description |
|-----------|-------------|
| PlaybookBottomBar | 2-tab nav bar (Home, Notifications with badge) |
| StatusBadge | Attendance status chip (Confirmed / Declined / Pending) |
| InviteStatusBadge | Invite status chip (Pending / Accepted / Rejected) |
| RoleChip | Role label (Coach / Player) |
| EventTypeIndicator | Coloured icon for Training / Match / Other |
| OfflineIndicator | Banner shown when network unavailable |

---

## Interaction Patterns

### Back Navigation
- Both platforms: back arrow in top-left of all pushed screens
- Android: system back button + predictive back gesture
- iOS: swipe-from-left edge gesture (handled by CMP Navigation — no custom code needed)

### Bottom Sheets
Material3 `ModalBottomSheet` — renders identically on both platforms.
Sheets: TeamEditSheet, TeamInviteSheet, BegrundungSheet, AbsenceSheet, ClubCoachInviteSheet.

### Date/Time Pickers
Material3 `DatePicker` / `TimePicker` dialogs render the same on both platforms. No native iOS picker. Acceptable for MVP.

### Offline Indicator
`OfflineIndicator` banner appears at top of affected screens when `NetworkMonitor` detects no connectivity.
SQLDelight cache serves stale data; write operations rejected with inline error.

### Push Permission Flow (platform-specific expect/actual)
- Android: `POST_NOTIFICATIONS` permission → OS dialog
- iOS: `UNUserNotificationCenter.requestAuthorization` → OS dialog
- Both triggered from `PushPermissionScreen`; platform call wrapped in `expect/actual`.

### Haptic Feedback
- Android: `HapticFeedbackType` via `LocalHapticFeedback`
- iOS: `UIImpactFeedbackGenerator` via `expect/actual`
- Used on: attendance confirm/decline, invite accept

### Safe Area Insets
All screens use `WindowInsets.safeContent` / `safeDrawing` — works on both platforms without platform-specific code.

### Status Bar Appearance
- Android: edge-to-edge via `enableEdgeToEdge()`
- iOS: handled by `ComposeUIViewController`; light/dark follows system theme automatically

---

## Edge Cases

| Scenario | Behaviour |
|----------|-----------|
| Network offline at app start | Cached data shown; OfflineIndicator visible; sync on reconnect |
| Token expired mid-session | 401 from API → logout → Login; "Session expired" snackbar |
| Invite link already used | InviteAccept shows "Invite already used or expired" error state |
| Push permission denied (iOS) | "Notifications disabled — enable in Settings" with deep link to iOS Settings |
| Push permission denied (Android) | Same, deep link to Android app notification settings |
| Club with no teams | ClubDashboard empty state + "Create your first team" CTA |
| Deep link while unauthenticated | Login shown; after auth, navigate to deep link destination |
| App backgrounded mid-flow | State preserved via ViewModel; on resume, screen restores |
