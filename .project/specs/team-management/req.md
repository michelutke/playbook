---
template: req
version: 0.1.0
gate: READY
---
# Requirements: Team Management

## Overview
A Club has one or more Teams. A ClubManager sets up the club structure by creating teams and assigning coaches. Coaches manage their teams day-to-day. Players belong to one or more teams.

## Role Hierarchy
```
SuperAdmin
  └── ClubManager (manages club structure — teams & coach assignments)
        └── Coach (manages day-to-day team operations; ClubManager may also hold this role)
              └── Player (member of one or more teams)
```

## Functional Requirements

### Club Setup (ClubManager)
- FR-TM-01: ClubManager can create and edit club profile (name, logo, sport type, location)
- FR-TM-02: ClubManager can create teams within the club
- FR-TM-03: ClubManager can archive or delete a team
- FR-TM-04: ClubManager can invite a user as coach for a team by email or invite link
- FR-TM-05: ClubManager can remove a coach from a team
- FR-TM-06: ClubManager can view all teams, their members, and assigned coaches
- FR-TM-07: ClubManager can promote a player to coach role within a team
- FR-TM-08: ClubManager can also be assigned as a coach on any team within the club (roles overlap)

### Team Management (Coach)
- FR-TM-09: Coach can edit team details (name, description)
- FR-TM-10: Coach can invite players to the team by email or shareable invite link
- FR-TM-11: Coach can remove a player from the team
- FR-TM-12: Coach can view the full team roster with member profiles
- FR-TM-13: Coach can assign player numbers / positions (optional fields)
- FR-TM-14: Coach can create and manage sub-groups within a team (see event-scheduling spec for usage)

### Membership
- FR-TM-15: A player can belong to multiple teams (e.g. U18 + first team)
- FR-TM-16: A coach can manage multiple teams
- FR-TM-17: Invited users receive a notification/email with a join link
- FR-TM-18: Users without an account are prompted to register when following an invite link
- FR-TM-19: A player can leave a team themselves

### Player Profile (within team context)
- FR-TM-20: Player has a profile: name, avatar, jersey number (optional), position (optional), contact info (visible to all team members)
- FR-TM-21: Player profile shows attendance statistics summary

## Non-Functional Requirements
- Invite links expire (configurable, default 7 days)
- Role changes take effect immediately without requiring re-login
- Removing a member preserves their historical attendance data

## Out of Scope (MVP)
- Automated promotion/relegation between teams
- Public team profiles / discovery

## Resolved
- ClubManager can also hold a Coach role on any team in the club
- Player contact info visible to all team members
