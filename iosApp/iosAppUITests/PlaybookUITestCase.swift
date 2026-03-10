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
        // CMP OutlinedTextField → custom UIView, NOT UITextField.
        // Find fields by label text; fallback to coordinates.
        guard app.buttons["login_button"].waitForExistence(timeout: 10) else { return false }
        let emailField = app.staticTexts["Email"].firstMatch
        if emailField.waitForExistence(timeout: 5) {
            emailField.tap()
        } else {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.42)).tap()
        }
        app.typeText(email)
        let passwordField = app.staticTexts["Password"].firstMatch
        if passwordField.exists {
            passwordField.tap()
        } else {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.52)).tap()
        }
        app.typeText(password)
        app.buttons["login_button"].tap()
        return app.buttons["create_team_fab"].waitForExistence(timeout: 10)
    }
}
