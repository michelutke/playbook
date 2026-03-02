---
template: ux
version: 0.1.0
gate: READY
---
# UX Spec: Notifications

## User Flows

### F1 — Player/Coach: View Notification Inbox
```
Bottom tab bar → Bell icon (badge count)
  └─ Notification Inbox (S1)
       ├─ Unread items highlighted
       ├─ Tap item → deep-link to relevant screen (event detail, settings, etc.)
       └─ "Mark all read" action (top-right)
```

### F2 — Player/Coach: Tap Push Notification
```
Push notification received (locked screen / background)
  └─ Tap → app opens / foregrounds
       └─ Deep-link resolves:
            ├─ New/edited/cancelled event → Event Detail
            ├─ Event reminder → Event Detail
            ├─ Attendance response (coach) → Event Attendance List
            └─ Abwesenheit change (coach) → Player Profile / Absence entry
```

### F3 — Player: Configure Notification Settings
```
Profile → "Notification Settings" (S2)
  ├─ Toggle: New Events
  ├─ Toggle: Event Changes (edits)
  ├─ Toggle: Event Cancellations
  ├─ Toggle: Reminders
  │    └─ [if on] Lead time picker: 2h | 1 day | 2 days (segmented)
  └─ Save (auto-save on toggle)
```

### F4 — Coach: Configure Attendance Notification Preferences
```
Profile → "Notification Settings" (S2)
  ├─ [Coach section]
  │    ├─ Toggle: Per-response notifications (each confirm / decline / unsure)
  │    ├─ Toggle: Pre-event pending summary
  │    │    └─ [if on] Summary timing picker: 2h | 1 day | 2 days before event
  │    └─ Toggle: Abwesenheit changes
  └─ Auto-save on toggle
```
- Both per-response and summary can be active simultaneously (FR-NO-07)

---

## Screens

### S1 — Notification Inbox
- Grouped by date (Today / Yesterday / Earlier)
- Row: type icon + title + body snippet + relative timestamp + unread dot
- Unread rows: slightly highlighted background
- Tap row → deep-link + mark as read
- Swipe-left: dismiss (removes from list locally)
- Empty state: "You're all caught up" illustration

### S2 — Notification Settings
Sectioned scroll:

**All Users**
| Setting | Control |
|---|---|
| New Events | Toggle |
| Event Changes | Toggle |
| Event Cancellations | Toggle |
| Reminders | Toggle + lead time picker (2h / 1 day / 2 days) |

**Coach only** (shown if user has Coach or ClubManager role)
| Setting | Control |
|---|---|
| Per-response (confirm/decline/unsure) | Toggle |
| Pre-event pending summary | Toggle + timing picker |
| Abwesenheit changes | Toggle |

- Banner shown if push permission denied: "Enable notifications in device settings" + "Open Settings" button
- All changes auto-save; no explicit Save button

---

## Interaction Patterns

### Notification Types & Icons
| Type | Icon | Recipient |
|---|---|---|
| New event | 📅 | Player |
| Event edited | ✏️ | Player |
| Event cancelled | ❌ | Player |
| Event reminder | ⏰ | Player |
| Attendance response | ✅ | Coach |
| Pending summary | 📋 | Coach |
| Abwesenheit change | 🏖 | Coach |

### Badge Count
- App icon badge = count of unread inbox items
- Tab bar bell icon badge mirrors app badge
- Clears to zero when inbox is opened

### Deep-link Fallback
- If target event/player no longer exists: land on inbox with toast "Content no longer available"

### Push Permission
- Permission prompt triggered on first login (not on app launch)
- If denied: push silently disabled; in-app inbox still populated
- Settings screen shows push status banner

### Delivery Deduplication
- Server assigns idempotency key per notification trigger; duplicate deliveries suppressed

---

## Edge Cases

| Scenario | Behaviour |
|---|---|
| Push denied by user | In-app inbox only; settings banner prompts re-enable |
| User removed from team | No further notifications for that team's events |
| Event cancelled after reminder queued | Reminder cancelled server-side before send |
| Coach has both Player and Coach roles | Receives both player and coach notification types |
| App backgrounded, multiple notifications arrive | Badge count accumulates; inbox shows all |
| Notification tapped for deleted event | Toast "Content no longer available"; stay on inbox |
| Duplicate trigger (e.g. rapid event edits) | Deduplication key suppresses extra delivery; latest data shown |
| Web push unsupported browser | Silently skipped; in-app inbox unaffected |
