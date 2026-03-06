# Maestro E2E Flows

Mobile smoke tests for the Playbook Android app using [Maestro](https://maestro.mobile.dev).

## Prerequisites

- Maestro CLI installed: `curl -Ls "https://get.maestro.mobile.dev" | bash`
- Android emulator running with the Playbook app installed (`com.playbook.android`)
- A running backend (local or staging)

## Setup

```bash
cp maestro/.env.example maestro/.env
# Edit maestro/.env with real test credentials
```

## Running flows

```bash
# Single flow
maestro test maestro/flows/coach-register.yaml --env COACH_EMAIL=... --env COACH_PASSWORD=...

# All flows (reads from .env automatically when using --env-file)
maestro test maestro/flows/ --env-file maestro/.env

# With Maestro Studio (interactive)
maestro studio
```

## Flows

| File | Task | Description |
|------|------|-------------|
| `coach-register.yaml` | TS-044 | Register new coach and reach dashboard |
| `member-invite.yaml` | TS-044 | Accept a deep-link invite as a member |
| `event-attendance.yaml` | TS-045 | Coach creates event and opens attendance list |
| `sa-audit.yaml` | TS-045 | Verify no SA entry point exists in mobile app |
| `full-journey.yaml` | TS-045 | Login, dashboard, team, notifications, back |

## Notes

- `INVITE_TOKEN` must be obtained from your test backend before running `member-invite.yaml`.
- Flows use `optional: true` on taps that depend on data state (e.g., existing teams).
- These are smoke flows — they verify navigation and key UI text, not full business logic.
- CI: add `maestro test maestro/flows/ --env-file maestro/.env` as a post-deploy step against a staging build.
