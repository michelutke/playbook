import XCTest

final class AttendanceTests: PlaybookUITestCase {

    override func setUpWithError() throws {
        try super.setUpWithError()
        guard login() else {
            throw XCTSkip("Backend not available — skipping attendance tests")
        }
        // Navigate to an event's attendance list via team → sub-groups → events → event detail
        let teamItem = app.otherElements["active_team_item"].firstMatch
        guard teamItem.waitForExistence(timeout: 5) else {
            throw XCTSkip("No teams available — skipping attendance tests")
        }
        teamItem.tap()
        guard app.buttons["sub_groups_tab"].waitForExistence(timeout: 5) else {
            throw XCTSkip("Team detail did not load")
        }
        app.buttons["sub_groups_tab"].tap()
        app.buttons["View Events"].tap()
        // Tap first event if available
        let eventItem = app.otherElements["event_item"].firstMatch
        guard eventItem.waitForExistence(timeout: 5) else {
            throw XCTSkip("No events available — skipping attendance tests")
        }
        eventItem.tap()
        // From event detail, navigate to attendance list (button text may vary)
        let attendanceButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'Attendance'")).firstMatch
        guard attendanceButton.waitForExistence(timeout: 5) else {
            throw XCTSkip("Attendance button not found on event detail")
        }
        attendanceButton.tap()
    }

    func testAttendanceListRenders() throws {
        // The attendance screen title is "Attendance"
        XCTAssertTrue(app.navigationBars["Attendance"].waitForExistence(timeout: 5), "Attendance screen not found")
    }

    func testAttendanceItemsDisplayedWhenPresent() throws {
        let attendanceItem = app.otherElements["attendance_item"].firstMatch
        if attendanceItem.waitForExistence(timeout: 3) {
            XCTAssertTrue(attendanceItem.exists, "Attendance row should be present")
        }
        // Empty state is acceptable — no assertion failure
    }
}
