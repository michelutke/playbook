---
template: nfrs
version: 0.1.0
---
# Non-Functional Requirements: Playbook

## Performance
- App must be responsive even on slow mobile connections (sports venues often have poor signal)
- Web app must load quickly — target < 3s on 4G
- Push notifications must deliver within seconds of trigger

## Security
- Authentication required for all user data
- Role-based access: coach/manager vs player
- No sensitive data exposed in client-side code or logs

## Privacy
- GDPR + Swiss nDSG compliance
- Special care for minors' data (youth sports teams)
- Data minimisation — only collect what's needed for J+S compliance

## Accessibility
- Mobile apps: follow iOS/Android accessibility guidelines
- Web: WCAG 2.1 AA minimum

## Reliability
- Offline support: users can view schedules and submit attendance without connectivity; sync when online
- 99.5% uptime target for backend

## UX Standards
- All user-facing feedback (success, warning, error) must use the global Snackbar system defined in `product/ux-patterns.md`
- Snackbars appear at the top of the screen; dismissed by swiping up
- Severity is colour-coded: green = success, orange = warning, red = error
- Errors are persistent until dismissed or resolved; success/info auto-dismiss after 3s
- Toast messages (bottom, info-only, no colour) are reserved for passive lightweight feedback only
- Technical error details (HTTP codes, stack traces) must never be shown to the user
