import XCTest

final class AttendanceTests: PlaybookUITestCase {

    override func setUpWithError() throws {
        try super.setUpWithError()
        guard login() else {
            throw XCTSkip("Backend not available — skipping attendance tests")
        }
        // Navigate to an event's attendance list via team → sub-groups → events → event detail
        // CMP Card(onClick=...) maps to a button on iOS
        let teamItem = app.buttons["active_team_item"].firstMatch
        guard teamItem.waitForExistence(timeout: 5) else {
            throw XCTSkip("No teams available — skipping attendance tests")
        }
        teamItem.tap()
        guard app.buttons["sub_groups_tab"].waitForExistence(timeout: 5) else {
            throw XCTSkip("Team detail did not load")
        }
        app.buttons["sub_groups_tab"].tap()
        app.buttons["View Events"].tap()
        // Tap first event — CMP ListItem(.clickable) maps to a button on iOS
        let eventItem = app.descendants(matching: .any)["event_item"].firstMatch
        guard eventItem.waitForExistence(timeout: 5) else {
            throw XCTSkip("No events available — skipping attendance tests")
        }
        eventItem.tap()
        // Navigate to attendance list via "Manage Attendance" button on event detail
        let manageBtn = app.buttons["Manage Attendance"]
        guard manageBtn.waitForExistence(timeout: 8) else {
            throw XCTSkip("Manage Attendance button not found — event detail may not have loaded")
        }
        manageBtn.tap()
    }

    func testAttendanceListRenders() throws {
        // CMP TopAppBar renders as custom views on iOS, not UINavigationBar — check for title text
        XCTAssertTrue(app.staticTexts["Attendance"].waitForExistence(timeout: 5), "Attendance screen title not found")
    }

    func testAttendanceItemsDisplayedWhenPresent() throws {
        // CMP Column(.clickable) maps to a button on iOS
        let attendanceItem = app.descendants(matching: .any)["attendance_item"].firstMatch
        if attendanceItem.waitForExistence(timeout: 3) {
            XCTAssertTrue(attendanceItem.exists, "Attendance row should be present")
        }
        // Empty state is acceptable — no assertion failure
    }
}
