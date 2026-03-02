---
template: ux
version: 0.1.0
gate: READY
---
# UX Spec: Attendance Tracking

## User Flows

### F1 — Player: Respond to Event
```
Event Detail
  ├─ Tap "Confirmed" → sync immediately (no sheet)
  ├─ Tap "Declined"  → Begründung Sheet (optional reason) → submit → sync
  └─ Tap "Unsure"    → Begründung Sheet (mandatory reason) → submit → sync
```
- Status buttons are directly on the event detail screen (no bottom sheet for confirm)
- Response blocked + buttons hidden/disabled after deadline; "Deadline reached" label shown

### F2 — Player: View Team Attendance
```
Event Detail
  └─ Attendance summary card (counts only)
       └─ Tap "See All" → Attendance List Screen
            └─ List: avatar + name + status badge [+ Begründung on expand]
```

### F3 — Player: Manage Abwesenheit
```
Profile / Settings
  └─ "My Absences" screen
       ├─ List of active absences
       │    └─ Swipe to delete / tap to edit
       └─ "+" → Add Absence Sheet
            ├─ Preset type picker (icon grid):
            │    🏖 Holidays  🤕 Injury  💼 Work  🏫 School  ✈️ Travel  📝 Other
            ├─ Type: Recurring | Period
            ├─ [Recurring] Weekday picker (multi-select Mon–Sun) + optional end date
            └─ [Period] Date range picker (from / to) + optional custom label
```
- Preset type sets icon and default label; "Other" allows free-text label
- Recurring and period are sub-options within a chosen preset type

### F4a — Player/Coach: Individual Player Statistics
```
Player Profile
  └─ "Statistics" tab
       ├─ Filter bar: event type (All / Training / Match / Other) + date range
       └─ Stats card: % present, % training, % match, per category
```

### F4b — Coach/ClubManager: Team Statistics
```
Team Screen
  └─ "Statistics" tab
       ├─ Filter bar: event type + date range
       └─ Player list: avatar + name + presence % + training % + match %
            └─ Tap player → drill into F4a (individual view)
```

### F5 — Coach: Post-Event Attendance Check (optional)
```
[Event ends]
  └─ [If check-in enabled for team] Push notification to coach:
       "Training ended — confirm attendance for [Event Name]"
       └─ Open Event Detail
            └─ Attendance list (editable by coach)
                 └─ Tap player row → toggle: Present / Absent / Excused
                      └─ "Submit" button → locks attendance, stores audit log
```
- Check-in is **off by default**; enabled per-team by coach/manager in team settings
- When event ends with check-in disabled: all `confirmed` players auto-set to `present`
- When check-in enabled but coach does not submit: no auto-set (remains pending)

---

## Screens

### S1 — Event Detail
- Header: event name, date/time, location
- **Attendance summary card** (visible to all): confirmed N / declined N / unsure N
  - Tap "See All" → S3 Attendance List
- **My Attendance card** (player role): three inline tap targets — Confirmed / Declined / Unsure
  - If user has both coach and player roles: horizontal scroll between summary card and My Attendance card
- **Coach check-in button** (small, coach-only, visible after event ends if check-in enabled): "Update Attendance" → opens editable attendance list

### S2 — Begründung Sheet (bottom sheet)
- Appears only when "Declined" or "Unsure" tapped
- Text area: mandatory for Unsure, optional for Declined
- Inline validation: Unsure blocks submit without text
- "Submit" CTA

### S3 — Attendance List Screen
- Section headers: Confirmed (N) / Unsure (N) / Declined (N) / No Response (N)
- Each row: avatar + name + status badge
- Expandable row: Begründung text, auto-decline indicator (⟳ "Abwesenheit" label)
- Coach: rows are tappable to override status inline

### S6 — My Absences Screen
- Empty state: "No absences declared" + add button
- List: type icon + label + date info chip
- Swipe-left: delete (confirm dialog)
- Tap: edit → same add sheet

### S7 — Add/Edit Absence Sheet
- Icon grid for preset type (🏖 🤕 💼 🏫 ✈️ 📝)
- Recurring | Period toggle
- **Recurring**: weekday multi-select grid + optional end date
- **Period**: date range picker + optional custom label (pre-filled from preset)
- "Save"

### S8 — Player Statistics Screen (Individual)
- Filter bar: event type segmented + date range picker
- Stats: presence %, training %, match % displayed as progress rings or bars

### S9 — Team Statistics Screen
- Same filter bar as S8
- List: avatar + name + presence % + bar indicator
- Tap → S8

---

## Interaction Patterns

### Status Badges
| Status | Visual |
|---|---|
| `confirmed` | Green filled dot |
| `declined` | Red filled dot |
| `unsure` | Yellow filled dot |
| `declined-auto` | Dashed outline dot + ⟳ icon |

### Deadline Enforcement
- Client hides/disables response buttons at deadline
- Server rejects late submissions with 409; client shows "Deadline has passed"

### Offline Behaviour
- Responses queued locally; submitted on reconnect
- Optimistic UI: status updates immediately, reverts on sync failure with toast

---

## Edge Cases

| Scenario | Behaviour |
|---|---|
| Manual response while Abwesenheit active | Manual status wins; Abwesenheit rule unchanged |
| Change response after deadline | Blocked; show "Deadline has passed" |
| Unsure submitted without Begründung | Inline error on sheet; not submitted |
| Abwesenheit rule deleted | Past `declined-auto` events unchanged |
| Check-in off, event ends | All `confirmed` → `present` automatically |
| Check-in on, coach never submits | Attendance stays pending (no auto-set) |
| Two coaches edit attendance simultaneously | Last-write-wins; audit log records both |
