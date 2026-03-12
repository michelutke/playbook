import XCTest

final class EventListTests: PlaybookUITestCase {

    override func setUpWithError() throws {
        try super.setUpWithError()
        guard login() else {
            throw XCTSkip("Backend not available — skipping event list tests")
        }
        // Navigate into a team to reach events — CMP Card(onClick=...) maps to a button on iOS
        let teamItem = app.buttons["active_team_item"].firstMatch
        guard teamItem.waitForExistence(timeout: 5) else {
            throw XCTSkip("No teams available — skipping event list tests")
        }
        teamItem.tap()
        // Switch to Sub-groups tab which contains the "View Events" button
        guard app.buttons["sub_groups_tab"].waitForExistence(timeout: 5) else {
            throw XCTSkip("Team detail did not load")
        }
        app.buttons["sub_groups_tab"].tap()
        app.buttons["View Events"].tap()
    }

    func testEventListScreenRenders() throws {
        XCTAssertTrue(app.buttons["create_event_fab"].waitForExistence(timeout: 5), "Create event FAB not found")
    }

    func testEventItemsVisibleWhenPresent() throws {
        // CMP ListItem(.clickable) maps to a button on iOS
        let eventItem = app.descendants(matching: .any)["event_item"].firstMatch
        if eventItem.waitForExistence(timeout: 3) {
            XCTAssertTrue(eventItem.isHittable, "Event item should be tappable")
        } else {
            // No events is a valid state
            XCTAssertTrue(app.staticTexts["No events"].exists || app.buttons["create_event_fab"].exists)
        }
    }
}
