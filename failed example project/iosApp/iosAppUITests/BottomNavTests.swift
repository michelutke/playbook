import XCTest

final class BottomNavTests: PlaybookUITestCase {

    override func setUpWithError() throws {
        try super.setUpWithError()
        guard login() else {
            throw XCTSkip("Backend not available — skipping bottom nav tests")
        }
    }

    func testHomeTabVisible() throws {
        XCTAssertTrue(app.buttons["home_tab"].waitForExistence(timeout: 5), "Home tab not found in bottom nav")
    }

    func testNotificationsTabVisible() throws {
        XCTAssertTrue(app.buttons["notifications_tab"].waitForExistence(timeout: 5), "Notifications tab not found in bottom nav")
    }

    func testNotificationsTabNavigation() throws {
        let notificationsTab = app.buttons["notifications_tab"]
        XCTAssertTrue(notificationsTab.waitForExistence(timeout: 5))
        notificationsTab.tap()
        // Notifications screen is now selected; home tab still visible
        XCTAssertTrue(app.buttons["home_tab"].waitForExistence(timeout: 3), "Home tab should remain after switching to notifications")
    }

    func testHomeTabNavigatesBackToDashboard() throws {
        // Navigate to notifications then back to home
        let notificationsTab = app.buttons["notifications_tab"]
        XCTAssertTrue(notificationsTab.waitForExistence(timeout: 5))
        notificationsTab.tap()
        app.buttons["home_tab"].tap()
        // Dashboard FAB should be visible again
        XCTAssertTrue(app.buttons["create_team_fab"].waitForExistence(timeout: 5), "Dashboard FAB not found after tapping Home tab")
    }
}
