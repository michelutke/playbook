---
template: ux
version: 0.1.0
gate: READY
---
# UX Spec: Super Admin

## User Flows

### F1 — Onboard New Club (FR-SA-01)
```
Dashboard → "New Club" button
  └─ Create Club Sheet
       ├─ Club name (required)
       ├─ Metadata fields (optional)
       ├─ Manager emails section (optional, repeatable):
       │    ├─ Email input row + "Add another" link
       │    └─ [each email] gets an invite on save
       └─ "Create" → club created + invites queued → Club Detail screen
```
- Club is created even with zero managers; warning banner shown on Club Detail
- Each invited email receives a one-time setup link

### F2 — Invite Additional ClubManager (FR-SA-06)
```
Club Detail → "Managers" section → "Invite Manager" button
  └─ Invite Manager Sheet
       ├─ Email input
       └─ "Send Invite" → invite queued → pending row appears in managers list
```
- No cap on number of managers per club
- Pending invites shown with "Pending" badge + resend / revoke actions

### F3 — Remove ClubManager (FR-SA-07)
```
Club Detail → Managers list → manager row → "Remove" (kebab or swipe)
  └─ Confirmation dialog: "Remove [Name] as manager of [Club]?"
       ├─ Confirm → manager loses access immediately
       └─ Cancel → dismiss
```
- If last manager removed: warning banner appears on Club Detail
- Club data is unaffected

### F4 — Deactivate / Reactivate Club (FR-SA-02)
```
Club Detail → Danger Zone → "Deactivate Club"
  └─ Confirmation dialog: "Deactivate [Club Name]? All members lose access."
       ├─ Confirm → status → inactive; all sessions invalidated
       └─ Cancel → dismiss
```
- Deactivated club shows "Reactivate Club" instead; no confirmation required
- Data fully preserved

### F5 — Delete Club (FR-SA-05)
```
Club Detail → Danger Zone → "Delete Club"
  └─ Full-screen confirmation modal
       ├─ Warning: "Permanently deletes [Club] and all data. Cannot be undone."
       ├─ Type-to-confirm: must type club name exactly
       └─ "Delete permanently" (enabled only on name match) → deleted → Clubs List
```

### F6 — Impersonate ClubManager (FR-SA-08)
```
Club Detail → Managers list → manager row → "Impersonate"
  └─ Confirmation dialog: "You will act as [Name] for up to 1 hour. All actions are audit-logged."
       └─ Confirm → impersonation session begins
            ├─ Persistent banner: "⚠ Impersonating [Name] — [MM:SS] | Exit"
            └─ "Exit" / timer expiry → return to SuperAdmin context
```
- Banner cannot be dismissed; visible on all screens during session
- Auto-terminates at 1 hour; 5-min warning toast shown
- All actions logged with `impersonated_by: superadmin_id`

### F7 — Search User (FR-SA-11)
```
Global search bar → name or email
  └─ Results list: avatar + name + email + club membership chips
       └─ Tap → User Detail (club memberships + roles only; no player data)
```

### F8 — View Audit Log (FR-SA-12)
```
Sidebar → "Audit Log"
  └─ Audit Log screen
       ├─ Filter: action type · actor · date range
       ├─ Entries: timestamp + actor + action + target
       └─ Tap entry → detail sheet (full payload, read-only)
```

---

## Screens

### S1 — Dashboard
- Metric cards: Total Clubs · Total Users · Active Events Today · New Sign-ups (7d)
- Recent Activity feed: last 10 audit entries (tap → S6)
- Quick actions: "New Club" · "Search User"

### S2 — Clubs List
- Search / filter bar; status tabs: All · Active · Inactive
- Each row: club name + manager count + team count + member count + status badge
- Tap → S3; "New Club" FAB

### S3 — Club Detail
- Header: club name · status badge · edit (pencil)
- **Summary**: teams · members · created date
- **Managers section**:
  - List rows: avatar + name + email + status badge (Active / Pending)
  - Per-row actions: Impersonate · Remove
  - Pending rows: Resend Invite · Revoke
  - "Invite Manager" button (always visible; no cap)
  - Warning banner if managers list is empty
- **Danger Zone** (collapsed accordion): Deactivate/Reactivate · Delete Club

### S4 — Create Club Sheet (bottom sheet)
- Club name (required)
- Optional metadata (sport type, location, notes)
- Manager emails: repeating email input rows + "Add another manager" link
- "Create Club" CTA

### S5 — Invite Manager Sheet (bottom sheet)
- Email input
- "Send Invite" CTA

### S6 — Audit Log Screen
- Filter bar: actor search · action type · date range
- Log list: `[timestamp] [actor] [action] on [target]`
- Tap → detail bottom sheet (read-only JSON, monospace)
- Export CSV (respects active filters)

### S7 — User Detail
- Name · email · club membership list (club name + role per club)
- No player stats, health, or personal data

### S8 — Impersonation Banner (persistent overlay)
- Fixed below system status bar; amber background
- `⚠ Impersonating [Full Name] · [MM:SS] remaining`
- "Exit Impersonation" button (right-aligned)

---

## Interaction Patterns

### Club Status Badges
| Status | Visual |
|---|---|
| `active` | Green pill |
| `inactive` | Grey pill |

### Manager Status Badges
| Status | Visual |
|---|---|
| `active` | Green dot |
| `pending` | Amber dot + "Pending" label |

### Destructive Action Hierarchy
| Severity | Pattern |
|---|---|
| Remove manager | Confirm dialog |
| Deactivate club | Confirm dialog with consequence text |
| Delete club | Full-screen modal + type-to-confirm |

---

## Edge Cases

| Scenario | Behaviour |
|---|---|
| Invite sent to existing Playbook user | User added as manager directly; no setup flow needed |
| Invite sent to user already managing this club | Show inline error "Already a manager of this club" |
| Last manager removed | Warning banner on Club Detail; club remains functional |
| Delete attempted on active club | Delete button disabled; tooltip: "Deactivate club first" |
| Impersonation session expires mid-action | Action rolls back; returned to SA context with toast |
| Multiple managers — impersonation | SuperAdmin picks which manager to impersonate from the list |
| Audit log for impersonated action | Records `actor: superadmin_id` + `on_behalf_of: manager_id` |
