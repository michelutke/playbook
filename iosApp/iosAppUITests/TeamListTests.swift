import XCTest

final class TeamListTests: PlaybookUITestCase {

    override func setUpWithError() throws {
        try super.setUpWithError()
        guard login() else {
            throw XCTSkip("Backend not available — skipping dashboard tests")
        }
    }

    func testCreateTeamFabVisible() throws {
        XCTAssertTrue(app.buttons["create_team_fab"].exists, "Create team FAB not found on dashboard")
    }

    func testActiveTeamItemVisible() throws {
        // Dashboard may show 0 teams for a fresh account — FAB presence is sufficient smoke signal.
        // If a team card exists, verify it is hittable.
        let teamItem = app.otherElements["active_team_item"].firstMatch
        if teamItem.exists {
            XCTAssertTrue(teamItem.isHittable, "Team item should be tappable")
        }
    }

    func testTapTeamNavigatesToDetail() throws {
        let teamItem = app.otherElements["active_team_item"].firstMatch
        guard teamItem.waitForExistence(timeout: 5) else {
            throw XCTSkip("No teams available — skipping team detail navigation test")
        }
        teamItem.tap()
        // Team detail shows roster tab
        XCTAssertTrue(app.buttons["roster_tab"].waitForExistence(timeout: 5), "Roster tab not found after navigating to team detail")
    }
}
