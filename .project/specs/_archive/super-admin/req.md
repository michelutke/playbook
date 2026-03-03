---
template: req
version: 0.1.0
gate: READY
---
# Requirements: Super Admin

## Overview
SuperAdmin is a platform-level role held by the Playbook operators. SuperAdmins onboard clubs, manage ClubManagers, and monitor platform health.

## Roles
- **SuperAdmin** — Playbook platform operator; access to club-level data (not individual player data)

## Functional Requirements

### Club Management
- FR-SA-01: SuperAdmin can create a new club account and optionally invite one or more ClubManagers at creation time
- FR-SA-02: SuperAdmin can deactivate / reactivate a club (data preserved)
- FR-SA-03: SuperAdmin can view all clubs with status, team count, and member count
- FR-SA-04: SuperAdmin can edit club details (name, metadata)
- FR-SA-05: SuperAdmin can delete a club permanently (with confirmation; irreversible)

### ClubManager Management
- FR-SA-06: SuperAdmin can invite one or more ClubManagers by email and assign them to a club (a club may have multiple ClubManagers)
- FR-SA-07: SuperAdmin can remove a ClubManager from a club without removing the club or other managers
- FR-SA-08: SuperAdmin can impersonate a ClubManager for support purposes (audit-logged)

### Platform Overview
- FR-SA-10: SuperAdmin dashboard shows: total clubs, total users, active events today, recent sign-ups
- FR-SA-11: SuperAdmin can search for any user by name or email and view their club memberships (not personal/player data)
- FR-SA-12: SuperAdmin can view an audit log of all admin actions

### Access Control
- FR-SA-13: SuperAdmin role is not accessible via normal registration — must be granted manually
- FR-SA-14: All SuperAdmin actions are written to an immutable audit log

## Non-Functional Requirements
- SuperAdmin UI is a separate, protected section inaccessible to regular users
- Impersonation sessions are time-limited (max 1 hour) and clearly indicated in UI
- Audit logs retained for minimum 2 years

## Out of Scope (MVP)
- Multi-tier admin roles (e.g. support agent vs full admin)
- Billing / subscription management UI
- Self-service club signup

## Resolved
- SuperAdmin sees club-level data only; no access to individual player data
- A club can have multiple ClubManagers; all share equal permissions within their club

## Open Questions
- OQ-1: Self-service club signup in v2 — does SuperAdmin still need to manually onboard, or does it become optional?
