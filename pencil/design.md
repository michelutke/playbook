# Playbook Design System — Pencil Reference

## File
`pencil-new.pen` — active design file used for all work below.

---

## Source Documents

All design decisions were informed by the following project files:

### Product
| File | What it provided |
|---|---|
| `.project/product/overview.md` | App purpose, feature set, role hierarchy (ClubManager > Coach > Player), overall scope |
| `.project/product/ux-patterns.md` | Global interaction patterns: Snackbar system (top-anchored, severity levels), Toast (bottom, info only), Persistent Banners |

### Feature Specs (all from `.project/specs/_archive/`)
| File | What it provided |
|---|---|
| `team-management/ux.md` | All user flows F1–F13; screens S1–S8 (Club Dashboard, Team Detail, Invite Sheet, etc.); role chip colors (Manager=Gold, Coach=Blue, Player=Grey); invite states (Pending/Accepted/Expired/Revoked) |
| `team-management/req.md` | Role capabilities: ClubManager manages structure/approvals, Coach manages day-to-day, Player is a team member |
| `event-scheduling/ux.md` | Event types (Training=blue, Match=green, Other=purple); Coach create/edit/cancel flows; Player list + calendar views; recurring events; cancelled state patterns |
| `attendance-tracking/ux.md` | RSVP states (Confirmed/Declined/Unsure/No reply); post-event check-in flow; Abwesenheit (pre-declared absences); status badge colors |
| `notifications/ux.md` | Notification inbox structure (grouped Today/Yesterday/Earlier); type icons per notification kind; unread dot; badge count; deep-link fallback pattern |

---

## Canvas Layout

| Layer | y offset | Description |
|---|---|---|
| Component Library | y=−1500 | Node `fJJQz` — 30 reusable components |
| Reference Screens | y=0 | 8 mixed-role screens, x=0 to x=3150 |
| ClubManager Flows | y=1100 | 4 screens, x=0 to x=1350 |
| Coach Flows | y=2150 | 4 screens, x=0 to x=1350 |
| Player Flows | y=3200 | 4 screens, x=0 to x=1350 |

Row labels sit 60px above each role row (nodes `4WugO`, `c3IjT`, `BBR7Q`).

Screen spacing: 450px between screens horizontally (390px frame + 60px gap).

---

## Design Tokens (Variables)

Set via `set_variables`. Key colors:

| Token | Value | Usage |
|---|---|---|
| `--primary` | `#3B82F6` | Primary blue — buttons, active nav, accents |
| `--bg` | `#0A0A0F` | Screen background |
| `--surface` | `#1C1C2E` | Cards, nav bar, inputs |
| `--surface-alt` | `#141418` | Subtle card variant |
| `--text` | `#F8FAFC` | Primary text |
| `--text-muted` | `#9CA3AF` | Secondary text |
| `--text-dim` | `#6B7280` | Tertiary / labels |
| `--border` | `#374151` | Dividers, ghost borders |
| `--success` | `#22C55E` | Confirmed / present |
| `--warning` | `#F59E0B` | Unsure / pending / amber health |
| `--error` | `#EF4444` | Declined / absent / destructive |
| `--gold` | `#FCD34D` | ClubManager role chip text |

---

## Mobile Frame

- Size: **390 × 844** (iPhone 15)
- Status bar: 62px fixed height at top
- Bottom nav: 62px pill, `cornerRadius: 36`, `fill: #1C1C2E`

---

## Component Library (node `fJJQz`)

### Buttons
| ID | Name |
|---|---|
| `QB89y` | Button/Primary |
| `lNVAJ` | Button/Ghost |
| `ORRL8` | Button/Destructive |
| `t2TjJ` | FAB |

### Chips
| ID | Name |
|---|---|
| `ZFhaz` | Chip/ClubManager (gold) |
| `0tF7Z` | Chip/Coach (blue) |
| `dmNwJ` | Chip/Player (grey) |
| `yzjqZ` | Chip/Training (blue) |
| `0XrEE` | Chip/Match (green) |
| `Dizpn` | Chip/Other (purple) |
| `HDc0Q` | Chip/Cancelled |

### Avatars
| ID | Name |
|---|---|
| `3Viyw` | Avatar/48 |
| `e8oeo` | Avatar/40 |
| `4Yd1W` | Avatar/32 |

### Status Badges
| ID | Name |
|---|---|
| `BLXtP` | Badge/Confirmed |
| `edKuA` | Badge/Declined |
| `V5O8v` | Badge/Unsure |
| `BEYAy` | Badge/NoResponse |

### Other Components
| ID | Name |
|---|---|
| `ZaeKz` | EventCard |
| `3bDrU` | MemberRow |
| `h74Ra` | Input/Text |
| `3g7Dw` | Toggle/Row |
| `HuzeF` | SegmentedControl |
| `NnOHj` | Snackbar/Success |
| `lg5Be` | Snackbar/Error |
| `DMLmP` | OfflineBanner |
| `sVemC` | BottomSheet/Header |
| `nhuyT` | SectionHeader |
| `FTbAZ` | BottomNav |
| `CR0YP` | StatsBar |

---

## Reference Screens (y=0)

| x | Node | Screen |
|---|---|---|
| 0 | `Ob8Dt` | S1 — Club Dashboard |
| 450 | `lGivb` | S2 — Team Detail (Roster) |
| 900 | `JGj64` | S3 — Invite Sheet (overlay) |
| 1350 | `fqlxQ` | S4 — Event List |
| 1800 | `2VTps` | S5 — Event Detail |
| 2250 | `RUeXD` | S6 — Attendance List |
| 2700 | `xgo5W` | S7 — Notification Inbox |
| 3150 | `oiPvL` | S8 — My Absences |

---

## Role-Based Flows

### ClubManager (y=1100) — Bottom nav: Overview / Teams / Members / Settings

| x | Node | Screen | Key patterns |
|---|---|---|---|
| 0 | `qCp71` | CM-1 Command Center | Stats strip (4 KPIs), gold action banner, 2-col team health grid (green/amber/red dots), quick action buttons, pill nav |
| 450 | `nj7W1` | CM-2 Pending Queue | Full-card per pending team with Approve (green) / Reject (red) buttons, collapsed "Recently Approved" |
| 900 | `Kw5L6` | CM-3 Club Roster | Filter chips (All/Coaches/Players), member list with role chips + team color dots, FAB invite |
| 1350 | `VH2ue` | CM-4 Team Detail | Coach avatar row + Add Coach ghost, attendance bar chart (last 5 events), 3 quick stat cards, danger zone (Archive/Delete) |

### Coach (y=2150) — Bottom nav: Today / Roster / Schedule / Stats

| x | Node | Screen | Key patterns |
|---|---|---|---|
| 0 | `8RJaO` | C-1 Today Dashboard | Hero card with event name + countdown chip + progress bar, avatar strip with colored RSVP rings, upcoming events list |
| 450 | `hLbLM` | C-2 Roster + Availability | Toggle to show next-event RSVP status per player row (Confirmed/Declined/Unsure/No reply badges), FAB invite |
| 900 | `cTzuE` | C-3 Quick Event Create | 3 large type tiles (Training/Match/Other), horizontal date strip, time pickers, title + location fields, recurring toggle, blue CTA |
| 1350 | `QbLLO` | C-4 Post-Event Check-In | Progress indicator (14/18 marked), player rows with 3-way toggle (✓ Present / — Absent / ~ Excused), pre-filled from RSVP, Submit button |

### Player (y=3200) — Bottom nav: My Week / Teams / Stats / Profile

| x | Node | Screen | Key patterns |
|---|---|---|---|
| 0 | `JrTEy` | P-1 My Week | Event cards with colored left accent bars (blue=training, green=match, purple=other) + inline status button, Coming Up section |
| 450 | `MMFZv` | P-2 Event RSVP | Large emoji hero icon, event details, 3 full-width stacked RSVP buttons (active = filled color), team attendance summary (12/2/4) |
| 900 | `8TGI0` | P-3 My Season Stats | Filter chips, ring chart (ellipse with colored border), big % in center, Training/Match breakdown cards, recent events with Present/Absent badges |
| 1350 | `gIq9e` | P-4 My Team | Next event teaser card with avatar strip, 2×2 player grid (green ring = confirmed for next event, grey = no response) |

---

## Pencil MCP — Key Lessons

### Screen frame setup (CRITICAL)
Always set `layout: "vertical"` on the top-level screen frame, otherwise `fill_container` on children won't resolve and content only renders in part of the frame.

```javascript
screen=I(document, {type: "frame", width: 390, height: 844, fill: "#0A0A0F", layout: "vertical", name: "Screen Name", x: 0, y: 0})
```

### Standard screen structure
```javascript
// Status bar
sb=I(screen, {type: "frame", width: "fill_container", height: 62, layout: "horizontal", alignItems: "center", padding: [0, 20, 0, 20], ...})
// Scrollable body
body=I(screen, {type: "frame", layout: "vertical", gap: 16, padding: [8, 20, 20, 20], width: "fill_container", height: "fill_container", ...})
// Spacer (pushes nav to bottom)
spacer=I(body, {type: "frame", height: "fill_container", width: "fill_container"})
// Bottom nav pill
nav=I(body, {type: "frame", layout: "horizontal", width: "fill_container", height: 62, padding: 4, cornerRadius: 36, fill: "#1C1C2E", gap: 4, alignItems: "center"})
```

### Icon fonts
Specify `family: "lucide"` but the stored property is `iconFontFamily`. Icons use Lucide names (e.g. `circle-alert` not `alert-circle`, `chevron-right`, `bar-chart-2`). Material Symbols Rounded is the fallback if family doesn't apply.

### Text color (CRITICAL)
Use `fill` for text node color — NOT `color`. The `color` property is silently ignored on text nodes.
```javascript
I(parent, {type: "text", content: "Hello", fill: "#F8FAFC", fontSize: 16})
```

### Variables format
Color variables require `{type: "color", value: "#hex"}` — not a plain string. Font and radius variables are not supported.

### 2-column grid
Use two explicit row frames, not `flexWrap` (not supported):
```javascript
grid=I(parent, {type: "frame", layout: "vertical", gap: 10, width: "fill_container"})
row1=I(grid, {type: "frame", layout: "horizontal", gap: 10, width: "fill_container"})
row2=I(grid, {type: "frame", layout: "horizontal", gap: 10, width: "fill_container"})
// Then insert cards into row1/row2 with width: "fill_container"
```

### Pill bottom nav — active tab pattern
Active tab: `fill: "#3B82F6"`, `layout: "horizontal"`, `padding: [10, 18]`, shows icon + label inline.
Inactive tabs: no fill, `layout: "vertical"`, `width: "fill_container"`, icon above label (10px font).

### fill_container in fixed-height frames
A `fill_container` spacer before the nav pushes it to the bottom of the body frame. Works when the body itself is `height: fill_container`.
