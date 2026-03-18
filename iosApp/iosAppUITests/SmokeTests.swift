import XCTest

/// Smoke tests that verify the app launches and a recognisable screen appears.
/// Elements are located via accessibility identifiers exposed by Compose testTags
/// (testTagsAsResourceId = true).
final class SmokeTests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    override func tearDownWithError() throws {
        app.terminate()
        app = nil
    }

    // MARK: - Launch

    func test_appLaunches_withoutCrashing() {
        XCTAssertEqual(app.state, .runningForeground)
    }

    func test_appLaunches_showsAuthScreen() {
        // Either the login button or the create-account button should be visible.
        let loginVisible = app.buttons["btn_sign_in"].waitForExistence(timeout: 5)
        let registerVisible = app.buttons["btn_create_account"].waitForExistence(timeout: 1)

        XCTAssertTrue(
            loginVisible || registerVisible,
            "Expected an auth screen (btn_sign_in or btn_create_account) to be visible on launch"
        )
    }
}
