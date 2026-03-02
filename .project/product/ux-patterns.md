---
template: ux-patterns
version: 0.1.0
---
# Global UX Patterns

App-wide interaction patterns. Feature specs reference these by name rather than redefining them.

---

## Snackbar System

Anchored to the **top** of the screen (below status bar / app bar). Single snackbar at a time; new one replaces any current one.

### Severity Levels

| Level | Colour | Use when |
|---|---|---|
| `success` | Green | Action completed; data saved; invite sent |
| `warning` | Orange | Partial success; non-blocking issue; recoverable state |
| `error` | Red | Action failed; data not saved; server error |
| `info` | Neutral (system default) | Neutral status update; no action needed |

### Behaviour

| Property | Value |
|---|---|
| Position | Top of screen, below status bar |
| Auto-dismiss (success / info) | 3 seconds |
| Auto-dismiss (warning) | 5 seconds |
| Persistent (error) | Yes — stays until dismissed or action taken |
| Dismiss gesture | Swipe up |
| Action button (optional) | Right-aligned label, e.g. "Retry", "Undo" |

### Usage Examples

| Scenario | Level | Message |
|---|---|---|
| Attendance response saved | `success` | "Response saved" |
| Optimistic update reverted on sync failure | `error` | "Couldn't save — tap to retry" |
| Invite sent | `success` | "Invite sent to [email]" |
| Invite link copied | `info` | "Link copied to clipboard" |
| Team request rejected by ClubManager | `warning` | "Team request rejected" |
| Event creation failed | `error` | "Couldn't create event. Try again." |
| Push permission denied, fallback active | `warning` | "Push disabled — notifications visible in-app only" |
| Impersonation session about to expire | `warning` | "Session ends in 5 minutes" |
| Action rolled back after session expiry | `error` | "Session expired — action not saved" |

### Rules

- Never show technical error details (stack traces, HTTP codes) to the user
- Error messages must be actionable or at least explanatory
- Success snackbars for destructive actions (delete, remove member) may include an "Undo" action (where technically feasible)
- Do not stack multiple snackbars; if two events fire simultaneously, show the higher severity one

---

## Toast System

Anchored to the **bottom** of the screen (above bottom nav bar). Used for lightweight, non-critical feedback only.

| Property | Value |
|---|---|
| Severity | `info` only (no colours) |
| Auto-dismiss | 3 seconds |
| Dismiss gesture | Swipe down |
| Action button | None |

**Use toast for:** clipboard copy confirmation, passive status messages (e.g. "Content no longer available").
**Use snackbar for:** everything requiring colour-coded severity feedback.

---

## Persistent Banners

Used for sustained states (not transient events). Displayed inline within the affected screen, not overlaid.

| Scenario | Level | Message |
|---|---|---|
| Push notifications denied | `warning` | "Enable notifications in device settings" + "Open Settings" |
| Club has no managers assigned | `warning` | "This club has no manager" |
| Event cancelled | `info` | "This event has been cancelled" |
| Impersonation active (SuperAdmin) | `info` | "⚠ Impersonating [Name] — [MM:SS] · Exit" |
