---
template: ux
version: 0.1.0
gate: READY
---
# UX Spec: Team Management

## User Flows

### F1 — ClubManager: Create Club
```
Onboarding / Club Setup screen (S1)
  └─ Club name → sport type → location → logo (optional)
       └─ "Create Club" → Club Dashboard (S2)
```

### F2 — ClubManager: Create Team
```
Club Dashboard (S2) → "+" → New Team sheet
  └─ Name + optional description
       └─ "Create" → Team appears in club list
```

### F3 — ClubManager: Invite Coach
```
Two entry points:

[Club-level — minimal setup]
Club Dashboard (S2) → "Invite Coaches" button
  └─ Club Coach Invite sheet (S6b):
       ├─ "Copy Coach Signup Link" (club-scoped, no team pre-assigned)
       └─ Email field → "Send"
            └─ Recipient follows link → F9 (coach path) → creates own team → F12

[Team-level — pre-assign to team]
Team Detail (S3, ClubManager view) → Roster tab → "+" → Invite sheet (S6):
  └─ Email field → "Send Invite"  or  "Copy Invite Link"
       └─ Recipient follows link → F9 (coach path) → added directly to that team
```

### F4 — ClubManager: Add Coach Role to Member
```
Team Detail (S3) → Roster tab → tap player row → Player Profile (S7)
  └─ "⋯" menu → "Add Coach Role"
       └─ Confirm dialog → Coach role added; Player role retained
            └─ Roster now shows both role chips for that user
```

### F5 — ClubManager: Archive / Delete Team
```
Team Detail (S3) → "⋯" menu
  ├─ Archive → team hidden from active list; data preserved
  └─ Delete → confirm dialog (warns: removes all members; attendance data preserved)
```

### F6 — Coach: Invite Players
```
Team Detail (S3, Coach view) → Roster tab → "+"
  └─ Invite sheet (S6):
       ├─ Email field → "Send Invite"
       └─ "Copy Invite Link" → share externally
```

### F7 — Coach: Edit Team Details
```
Team Detail (S3) → "⋯" menu → Edit
  └─ Inline form: name + description
       └─ "Save"
```

### F8 — Coach: Remove Member
```
Team Detail (S3) → Roster tab → swipe-left on player row
  └─ "Remove" → confirm dialog
       └─ Member removed; historical attendance preserved
```

### F9 — New User: Follow Invite Link
```
Invite link opened
  ├─ [Has account, team-scoped link] → app opens → "Join [Team Name]?" confirm → added to team
  ├─ [Has account, club coach link] → app opens → "Join [Club Name] as Coach?" confirm → F12
  ├─ [No account, team-scoped link] → registration → "Join [Team Name]?" confirm → added to team
  └─ [No account, club coach link] → registration → "Join [Club Name] as Coach?" confirm → F12
```

### F10 — Player: Leave Team
```
Team Detail (S3, player view) → "⋯" menu → "Leave Team"
  └─ Confirm dialog → removed from roster; historical data preserved
```

### F12 — Coach: Create Team (pending ClubManager approval)
```
Post-signup (no team assigned yet) → "Create Your Team" prompt (S8)
  └─ Team name + description (optional)
       └─ "Submit for Approval"
            └─ Team appears in Club Dashboard (S2) as "Pending" for ClubManager
                 └─ ClubManager reviews → F13
```
- Also accessible later: Club context → "Request New Team"

### F13 — ClubManager: Approve / Reject Pending Team
```
Club Dashboard (S2) → "Pending" section → tap pending team
  └─ Preview: team name + requesting coach
       ├─ "Approve" → team activated; coach assigned; coach notified
       └─ "Reject" → optional reason text → team removed; coach notified
```

### F11 — Coach: Manage Sub-groups
```
Team Detail (S3) → "Sub-groups" tab → "+"
  └─ New sub-group sheet: name → player multi-select → "Save"
       └─ Sub-group listed; tap to edit; swipe-left to delete
```

---

## Screens

### S1 — Club Setup Screen
- Full-screen form: club name, sport type (dropdown/search), city/location, logo picker
- "Create Club" primary CTA
- Logo: tap → image picker; circular crop preview

### S2 — Club Dashboard (ClubManager)
- Header: club logo + name + edit button
- **Pending** section (shown when > 0): team name + requesting coach + Approve / Reject actions
- Teams list: team name + member count + coach avatars (up to 3)
- Archived teams toggle (collapsed by default)
- FAB "+" → create team
- "Invite Coaches" button → S6b

### S3 — Team Detail
Tabs (role-dependent):

| Tab | Visible to |
|---|---|
| Roster | All |
| Sub-groups | Coach / ClubManager |
| Statistics | Coach / ClubManager |
| Settings | Coach / ClubManager |

**Roster tab**:
- Search bar
- Section: Coaches (avatars + names + role chip(s))
- Section: Players (avatar + name + jersey number chip if set + role chip(s))
- Users with multiple roles appear in all applicable sections with all their chips shown
- Coach: swipe-left → Remove; tap → S7
- ClubManager: additional "⋯" per row with Add Coach Role / Remove Coach Role / Remove from Team

### S4 — Club Profile Edit Screen
- Same fields as S1, pre-filled
- Logo replaceable
- "Save"

### S5 — Team Edit Sheet (bottom sheet)
- Name text field
- Description text area (optional)
- "Save"

### S6 — Invite Sheet (bottom sheet)
- Title: "Invite to [Team Name]"
- Email input + "Send" button
- Divider "or"
- Invite link row: truncated URL + "Copy" button + expiry note ("Link expires in 7 days")
- Sent invites list (pending, with revoke option)

### S7 — Player Profile (team context)
- Avatar (large) + name + jersey number + position
- Role chip(s) shown (multiple if applicable)
- Contact info section (email / phone; visible to all team members)
- Attendance stats summary card (links to full stats)
- Coach/ClubManager: "⋯" menu → Add Coach Role / Remove Coach Role / Remove from Team

### S6b — Club Coach Invite Sheet (bottom sheet)
- Title: "Invite Coaches to [Club Name]"
- "Copy Coach Signup Link" button + expiry note ("Link expires in 7 days")
- Divider "or"
- Email input + "Send" button
- Pending coach list (awaiting team assignment)

### S8 — Coach: First Team Setup Screen
- Shown after signing up via club coach link with no team assigned
- Heading: "Create your first team"
- Name field + optional description
- "Submit for Approval" CTA
- Subtext: "Your team will be visible once approved by the club manager"

---

## Interaction Patterns

### Role Chips
| Role | Chip colour |
|---|---|
| ClubManager | Gold |
| Coach | Blue |
| Player | Grey |

### Invite Flow States
| State | Visual |
|---|---|
| Pending | Clock icon + "Awaiting" label |
| Accepted | Green check |
| Expired | Grey "Expired" + "Resend" link |
| Revoked | Strikethrough |

### Multiple Roles
- A user can hold Player + Coach simultaneously; both chips shown on roster rows and profiles
- Adding a role never removes existing roles; removal of a role is explicit ("Remove Coach Role")
- Role changes take effect immediately; no re-login required
- Toast shown to ClubManager: "Coach role added to [Name]"

### Archived Teams
- Greyed in list with "Archived" chip
- Events and data read-only
- Can be unarchived by ClubManager

---

## Edge Cases

| Scenario | Behaviour |
|---|---|
| Invite link expired | Landing screen: "This link has expired. Ask your coach for a new one." |
| Email already a member | "Already a member" error on invite send |
| Coach role removed, user still a player | Player role and membership unchanged |
| Last coach role removed from team | Blocked: "A team must have at least one coach" |
| Player in multiple teams leaves one | Only removed from that team; other memberships unchanged |
| Delete team with active events | Confirm dialog warns; events and attendance data preserved |
| ClubManager adds Coach role to self | Allowed; holds both roles simultaneously |
| Unregistered email invited | Invite email sent; account created on link follow |
| Coach signup link used, team rejected | Coach notified; can resubmit with different name or contact ClubManager |
| Coach signs up via club link, skips team creation | Can create team later; shown "Create Team" prompt on next login |
| Coach added via team-scoped link also wants Player role | ClubManager can add Player role separately via profile "⋯" menu |
