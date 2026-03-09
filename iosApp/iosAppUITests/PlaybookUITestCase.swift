import XCTest

/// Base class for Playbook UI tests.
/// Provides app launch and a login helper that skips tests when the backend is unavailable.
class PlaybookUITestCase: XCTestCase {
    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    override func tearDownWithError() throws {
        app.terminate()
    }

    /// Attempts to log in with the given credentials.
    /// - Returns: `true` if the club dashboard was reached, `false` otherwise.
    @discardableResult
    func login(email: String = "test@playbook.test", password: String = "testpassword") -> Bool {
        guard app.textFields["email_field"].waitForExistence(timeout: 5) else { return false }
        app.textFields["email_field"].tap()
        app.textFields["email_field"].typeText(email)
        app.secureTextFields["password_field"].tap()
        app.secureTextFields["password_field"].typeText(password)
        app.buttons["login_button"].tap()
        return app.buttons["create_team_fab"].waitForExistence(timeout: 10)
    }
}
