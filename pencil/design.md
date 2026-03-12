# Playbook Design System — Pencil Reference

## File
`playbook.pen` — active design file.

---

## Design Tokens

### Dark Mode (default — defined in file variables)
| Token | Value | Usage |
|---|---|---|
| `--background` | `#090912` | Screen background |
| `--surface` | `#13131F` | Slightly lighter surface (headers, tabs) |
| `--card` | `#1C1C2E` | Cards, list rows |
| `--foreground` | `#F0F0FF` | Primary text |
| `--muted-foreground` | `#9090B0` | Secondary / label text |
| `--border` | `#2A2A40` | Dividers, ghost borders, strokes |
| `--primary` | `#4F8EF7` | Electric blue — buttons, active nav, links |
| `--primary-foreground` | `#FFFFFF` | Text on primary |
| `--accent` | `#F97316` | Orange accent |
| `--destructive` | `#EF4444` | Destructive actions |
| `--color-success` | `#22C55E` | Confirmed / present |
| `--color-warning` | `#FACC15` | Unsure / pending |
| `--color-error` | `#EF4444` | Declined / absent |
| `--color-purple` | `#A855F7` | Other event type |
| `--color-gold` | `#FACC15` | ClubManager chip |
| `--color-grey` | `#6B7280` | Inactive icons / muted |

### Light Mode (derived — applied via property overrides)
| Dark token | Light value |
|---|---|
| `--background` `#090912` | `#FFFFFF` |
| `--surface` `#13131F` | `#F4F4F9` |
| `--card` `#1C1C2E` | `#FFFFFF` |
| `--foreground` `#F0F0FF` | `#0A0A1A` |
| `--muted-foreground` `#9090B0` | `#6B7280` |
| `--border` `#2A2A40` | `#E2E2F0` |
| Primary / accent / status colors | unchanged |

---

## Mobile Frame
- Size: **390 × 844** (iPhone 15)
- Status bar: 62px
- Bottom nav: 62px pill, `cornerRadius: 36`

---

## Component Library

### Dark — node `fJJQz` (🧩 Component Library)

| Category | ID | Name |
|---|---|---|
| Buttons | `QB89y` | Button/Primary |
| | `lNVAJ` | Button/Ghost |
| | `ORRL8` | Button/Destructive |
| | `t2TjJ` | FAB |
| Chips | `ZFhaz` | Chip/ClubManager |
| | `0tF7Z` | Chip/Coach |
| | `dmNwJ` | Chip/Player |
| | `yzjqZ` | Chip/Training |
| | `0XrEE` | Chip/Match |
| | `Dizpn` | Chip/Other |
| | `HDc0Q` | Chip/Cancelled |
| Avatars | `3Viyw` | Avatar/48 |
| | `e8oeo` | Avatar/40 |
| | `4Yd1W` | Avatar/32 |
| Badges | `BLXtP` | Badge/Confirmed |
| | `edKuA` | Badge/Declined |
| | `V5O8v` | Badge/Unsure |
| | `BEYAy` | Badge/NoResponse |
| Forms | `h74Ra` | Input/Text |
| | `3g7Dw` | Toggle/Row |
| | `HuzeF` | SegmentedControl |
| | `QyMne` | AttendanceButtons/Small |
| Misc | `NnOHj` | Snackbar/Success |
| | `lg5Be` | Snackbar/Error |
| | `DMLmP` | OfflineBanner |
| | `sVemC` | BottomSheet/Header |
| | `nhuyT` | SectionHeader |
| | `FTbAZ` | BottomNav |
| | `CR0YP` | StatsBar |

### Light — node `yAPEw` (Component Library — Light)
Same 29 components with light mode color overrides applied at instance level.

---

## V1 Screens

All screens: **390 × 844**, dark mode originals + light mode duplicates.

### Dark Mode

| Node | Screen |
|---|---|
| `S5I1w` | V1-C — Event List |
| `Lrlit` | V1-P — Event List |
| `vyGwO` | V1-C — Event Calendar |
| `tON9m` | V1-P — Event Calendar |
| `1PUbR` | V1-C — Teams |
| `LAA1H` | V1-P — Teams |
| `uUJMZ` | V1-P — Teams (single) |
| `CSLp4` | V1 — Inbox |
| `5pAXJ` | V1 — Profile |
| `gaD2C` | V1 - New Event |
| `2uF5C` | V1 - Event Detail |
| `21Qtq` | V1 - Event Edit |
| `mNOVF` | V1 - NE-Date Picker |
| `EBbYR` | V1 - NE-Time Picker |
| `2Ub2q` | V1 - NE-Recurring Config |
| `g2gXC` | V1 - NE-Sub-groups |
| `raffO` | V1 - EE-Modal for recurring events |
| `nGmTp` | V1-C — Invite Member |
| `IV26M` | V1-C — Invite Member Sent |
| `FcxGd` | V1 — Add absence |
| `BPjIK` | V1-T - Player Detail |
| `GbsY3` | V1-Team - Statistics |

### Light Mode

| Node | Screen |
|---|---|
| `iRz5V` | V1-C — Event List (Light) |
| `UhlZ9` | V1-P — Event List (Light) |
| `8RVYu` | V1-C — Event Calendar (Light) |
| `JRE44` | V1-P — Event Calendar (Light) |
| `mgXNx` | V1-C — Teams (Light) |
| `tnB8i` | V1-P — Teams (Light) |
| `E4bif` | V1-P — Teams (single) (Light) |
| `sWbWj` | V1 — Inbox (Light) |
| `m5RdO` | V1 — Profile (Light) |
| `p6DNb` | V1 - New Event (Light) |
| `NDxOi` | V1 - Event Detail (Light) |
| `fhGRl` | V1 - Event Edit (Light) |
| `20pZ0` | V1 - NE-Date Picker (Light) |
| `6E3xt` | V1 - NE-Time Picker (Light) |
| `0bGW1` | V1 - NE-Recurring Config (Light) |
| `yBJPo` | V1 - NE-Sub-groups (Light) |
| `Cxc0M` | V1 - EE-Modal for recurring events (Light) |
| `ujU0j` | V1-C — Invite Member (Light) |
| `dbOXm` | V1-C — Invite Member Sent (Light) |
| `7bpO0` | V1 — Add absence (Light) |
| `CHGZR` | V1-T - Player Detail (Light) |
| `4Ewz6` | V1-Team - Statistics (Light) |

---

## Pencil MCP — Key Lessons

### Screen frame setup (CRITICAL)
Always set `layout: "vertical"` on the top-level screen frame.

```javascript
screen=I(document, {type: "frame", width: 390, height: 844, fill: "#090912", layout: "vertical", name: "Screen Name", x: 0, y: 0})
```

### Standard screen structure
```javascript
sb=I(screen, {type: "frame", width: "fill_container", height: 62, layout: "horizontal", alignItems: "center", padding: [0, 20, 0, 20]})
body=I(screen, {type: "frame", layout: "vertical", gap: 16, padding: [8, 20, 20, 20], width: "fill_container", height: "fill_container"})
spacer=I(body, {type: "frame", height: "fill_container", width: "fill_container"})
nav=I(body, {type: "ref", ref: "FTbAZ"})
```

### Text color (CRITICAL)
Use `fill` for text color — NOT `color`.
```javascript
I(parent, {type: "text", content: "Hello", fill: "#F0F0FF", fontSize: 16})
```

### Icon fonts
Use `iconFontFamily: "lucide"`. Icon names follow Lucide convention (`circle-alert`, `chevron-right`, etc.).

### Light mode — applying color overrides to ref instances
Use `replace_all_matching_properties` with all parents at once. For ref nodes, set `fill` directly on the ref and use `descendants` map for nested overrides. Alternatively use `U("refId/childId", {fill: "..."})` for targeted descendant updates.

### 2-column grid
Use two row frames — `flexWrap` is not supported.
```javascript
grid=I(parent, {type: "frame", layout: "vertical", gap: 10, width: "fill_container"})
row1=I(grid, {type: "frame", layout: "horizontal", gap: 10, width: "fill_container"})
row2=I(grid, {type: "frame", layout: "horizontal", gap: 10, width: "fill_container"})
```

### Variables format
Color variables require `{type: "color", value: "#hex"}` — not a plain string.
