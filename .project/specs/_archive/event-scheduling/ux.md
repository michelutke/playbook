---
template: ux
version: 0.1.0
gate: READY
---
# UX Spec: Event Scheduling

## User Flows

### F1 — Coach: Create Event
```
Event List / Calendar → "+" button
  └─ Create Event Form (S4)
       ├─ Basic: title, type, team(s)
       ├─ Time: start + end date/time + optional meetup time
       ├─ Location (optional)
       ├─ Audience: sub-group filter (optional)
       ├─ Options: min attendees, description (optional)
       └─ Recurring toggle → Recurring Pattern Sheet (S5)
            └─ "Create" → players notified
```

### F2 — Coach: Edit Event
```
Event Detail → "⋯" menu → Edit
  └─ [Recurring event?]
       └─ Scope Sheet (S6): This event / This & future / All
  └─ Edit Form (S4, pre-filled)
       └─ "Save" → changed players notified
```

### F3 — Coach: Cancel Event
```
Event Detail → "⋯" menu → Cancel
  └─ [Recurring event?]
       └─ Scope Sheet: This event only / This and all future events
  └─ Confirm dialog
       └─ Event(s) marked cancelled; players notified
          Attendance responses preserved with `cancelled` marker
```

### F4 — Coach: Duplicate Event
```
Event Detail → "⋯" menu → Duplicate
  └─ Create Form (S4) pre-filled with event data
       └─ Coach adjusts → "Create"
```

### F5 — Coach: Manage Sub-groups
```
Team Settings → "Sub-groups"
  └─ List of sub-groups (name + member count)
       ├─ Tap → edit: name + player multi-select
       └─ "+" → new sub-group: name → player multi-select → save
```

### F6 — Player: Browse Events (List)
```
Home / Events tab
  └─ Chronological list of upcoming events (all teams)
       ├─ Filter: team chip(s) + event type chips
       └─ View toggle: List | Calendar → F7
```

### F7 — Player: Calendar View
```
Events tab → Calendar toggle
  ├─ Month view: dots on event days → tap day → inline day list
  └─ Week view: time blocks → tap event → Event Detail
```

---

## Screens

### S1 — Event List (Player)
- Filter chips row: team selector + All / Training / Match / Other
- List items: type icon + title + date/time + team badge + own status badge
- Cancelled events: greyed row + "Cancelled" badge
- FAB "+" visible to coach only

### S2 — Calendar View
- Month / Week segment control at top
- **Month**: dots under dates (colour-coded by type); tap day → slide-up day list
- **Week**: time-block grid; multi-day events span columns; tap → event detail
- Cancelled events: greyed blocks

### S3 — Event Detail
- Type icon + title + type chip
- Date/time (with multi-day indicator if applicable) + timezone-aware display
- Location field + "Open in Maps" link
- Team + sub-group tag (if restricted)
- Description (collapsible if long)
- Min attendees indicator if set
- Attendance summary card (see attendance-tracking UX S1)
- Coach "⋯" menu: Edit / Duplicate / Cancel

### S4 — Create / Edit Event Form
Sectioned scroll form:
1. **Basic**: title (text), type (Training / Match / Other segmented), team(s) multi-select
2. **Time**: start date+time picker, end date+time picker (defaults +90 min; multi-day supported), optional meetup time picker (before start)
3. **Location**: text field (optional)
4. **Audience**: sub-group filter (multi-select, shown after team selected; default = whole team)
5. **Options**: min attendees number field (optional), description text area (optional)
6. **Recurring**: toggle → opens S5

Primary CTA: "Create" / "Save"

### S5 — Recurring Pattern Sheet (bottom sheet)
- Repeat: Daily | Weekly | Custom interval (in days)
- [Weekly] Weekday multi-select grid (Mon–Sun)
- End date: toggle + date picker (optional)
- "Done" → returns to form with recurring summary chip

### S6 — Edit Recurring Scope Sheet (bottom sheet)
- Radio options:
  - This event only
  - This and future events
  - All events in series
- "Continue" → opens edit form

### S7 — Sub-group Management Screen
- List: group name + member count chip
- Tap row → edit sheet: name field + player multi-select
- Swipe-left: delete (confirm if events are targeting this group)
- "+" → new group sheet

---

## Interaction Patterns

### Event Type Icons & Colours
| Type | Icon | Colour |
|---|---|---|
| `training` | 🏃 | Blue |
| `match` | ⚽ | Green |
| `other` | 📅 | Purple |

### Recurring Indicator
- ⟳ icon on list items and calendar blocks
- Edit always prompts scope sheet (S6) before opening form

### Cancelled State
- List: greyed row + "Cancelled" chip
- Calendar: greyed block, strikethrough title
- Detail: banner "This event has been cancelled"
- Attendance data preserved (read-only)

### Multi-day Events
- List: date shown as "Fri 6 Jun — Sun 8 Jun"
- Calendar month: dot on each day of span
- Calendar week: event block spans across days

### Notifications (triggered by system)
- Event created → all targeted players notified
- Event edited → all targeted players notified
- Event cancelled → all targeted players notified

---

## Edge Cases

| Scenario | Behaviour |
|---|---|
| Edit recurring — this only | Detaches occurrence; original series unchanged |
| Edit recurring — all | Only future occurrences updated; past unchanged |
| Sub-group deleted with active events | Events targeting it become team-wide automatically |
| Multi-team event created | Appears independently in each team's calendar |
| End time before start time | Form validation error inline; save blocked |
| Coach cancels event with responses | Responses preserved; `cancelled` marker added |
| Recurring with no end date | Series continues indefinitely; expandable server-side |
