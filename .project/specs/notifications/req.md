---
template: req
version: 0.1.0
gate: READY
---
# Requirements: Notifications

## Overview
Push and in-app notifications keep players and coaches informed about upcoming events, attendance changes, and team updates. Users control which notifications they receive.

## Roles
- **Player** — receives event and attendance-related notifications
- **Coach** — receives team response updates, reminders before events
- **ClubManager** — receives club-level notifications

## Functional Requirements

### Event Notifications (to Players)
- FR-NO-01: Player receives a push notification when a new event is created for their team
- FR-NO-02: Player receives a reminder notification before an event (configurable lead time, e.g. 1 day, 2 hours)
- FR-NO-03: Player receives a notification when an event is edited (time/location changes)
- FR-NO-04: Player receives a notification when an event is cancelled

### Attendance Notifications (to Coach)
- FR-NO-05: Coach can choose to receive a notification for each individual player response (confirm / decline / unsure)
- FR-NO-06: Coach can choose to receive a pre-event summary of pending (no-response) players instead
- FR-NO-07: Both modes can be active simultaneously; coach configures per notification type

### Abwesenheit Notifications
- FR-NO-08: Coach receives a notification when a player adds or modifies an Abwesenheit entry that affects upcoming events

### Notification Settings
- FR-NO-09: Each user can enable/disable notification types independently (e.g. new events, reminders, attendance updates)
- FR-NO-10: User can configure reminder lead time (e.g. 2h, 1 day, 2 days before event)

### Delivery
- FR-NO-11: Push notifications on iOS and Android
- FR-NO-12: In-app notification feed / inbox (so notifications are accessible without push enabled)
- FR-NO-13: Web push notifications (best effort — browser support varies)

## Non-Functional Requirements
- Notification delivery target: < 30 seconds after trigger event
- No duplicate notifications for the same event
- Notifications must not be sent to removed/inactive team members

## Out of Scope (MVP)
- Email notifications
- SMS notifications
- Digest / weekly summary emails

## Resolved
- Coach notification mode: user's choice — per-response and/or summary, configurable
- OQ-1: Push provider → **OneSignal** (unified FCM + APNs + web push); implementation hidden behind `PushService` interface so provider is swappable
