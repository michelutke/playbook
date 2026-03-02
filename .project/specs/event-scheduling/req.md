---
template: req
version: 0.1.0
gate: READY
---
# Requirements: Event Scheduling

## Overview
Coaches create and manage events (trainings, matches, other) for their teams. Events are the anchor for attendance tracking and notifications. Recurring events reduce manual effort for regular schedules.

## Roles
- **Coach** — creates, edits, cancels events for their team(s)
- **Player** — views events, responds to attendance

## Functional Requirements

### Event Creation
- FR-ES-01: Coach can create an event with: title, type (training / match / other), start date/time, end date/time, location, optional description
- FR-ES-02: Events can span multiple days (e.g. tournament weekend) — end date/time may be on a different day than start
- FR-ES-03: Coach can create a **recurring event** — repeat pattern: daily, weekly on selected weekdays, custom interval; optional end date
- FR-ES-04: Coach assigns the event to one or more of their teams
- FR-ES-05: Coach can target an event at specific sub-groups within a team (see Sub-groups below)
- FR-ES-06: Coach can set a minimum required attendees count (optional)

### Event Management
- FR-ES-07: Coach can edit event details; players are notified of changes
- FR-ES-08: Coach can cancel an event; players are notified; attendance responses are preserved with a `cancelled` marker
- FR-ES-09: When editing a recurring event, coach can choose: edit this occurrence only / this and future / all occurrences
- FR-ES-10: Coach can duplicate an event as a quick-create shortcut

### Sub-groups
- FR-ES-11: Coach can define named sub-groups within a team (e.g. "Goalkeepers", "U16 squad")
- FR-ES-12: Players can be assigned to one or more sub-groups
- FR-ES-13: When creating an event, coach can restrict it to specific sub-groups; only those players receive the event and attendance request

### Player — Event View
- FR-ES-14: Player sees a chronological list of upcoming events for all their teams
- FR-ES-15: Player can view event details (location, time, description, attendance summary)
- FR-ES-16: Player can filter events by team and event type
- FR-ES-17: Player sees a calendar view of events (month / week)

### Event Types
- `training` — regular practice
- `match` / `game` — competitive event
- `other` — meeting, social event, etc.

## Non-Functional Requirements
- Calendar view must perform well with 100+ events loaded
- Recurring event expansion handled server-side (not storing every instance)
- Timezone: events stored in UTC, displayed in user's local timezone

## Out of Scope (MVP)
- External calendar sync (iCal / Google Calendar export)
- Event RSVP via calendar invite
- Video/streaming links for remote events

## Resolved
- Multi-day events: supported
- Sub-groups within a team: MVP feature
