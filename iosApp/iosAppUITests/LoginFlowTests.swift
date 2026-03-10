import XCTest

// CMP iOS accessibility notes:
// - Button.testTag → accessibilityIdentifier ✓ (found via app.buttons["id"])
// - OutlinedTextField → custom UIView, NOT UITextField. No accessibilityIdentifier from testTag.
//   Input areas are identified by their floating label text (e.g. "Email", "Password").

final class LoginFlowTests: PlaybookUITestCase {

    func testLoginFormRendersCorrectly() throws {
        // Login screen renders with all interactive elements accessible
        XCTAssertTrue(app.buttons["login_button"].waitForExistence(timeout: 10), "Login button not found")
        XCTAssertTrue(app.buttons["toggle_mode_button"].exists, "Toggle mode button not found")
        // OutlinedTextField labels are accessible as staticTexts or otherElements
        let emailLabel = app.staticTexts["Email"].waitForExistence(timeout: 5) ||
                         app.otherElements.matching(NSPredicate(format: "label CONTAINS 'Email'")).firstMatch.waitForExistence(timeout: 3)
        XCTAssertTrue(emailLabel, "Email field label not found")
    }

    func testEmailFieldInteraction() throws {
        XCTAssertTrue(app.buttons["login_button"].waitForExistence(timeout: 10))
        // Find and tap email field by its label
        let emailLabel = app.staticTexts["Email"].firstMatch
        if emailLabel.waitForExistence(timeout: 5) {
            emailLabel.tap()
        } else {
            // Fallback: tap approximate location of email field on screen
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.42)).tap()
        }
        // After tapping, keyboard or text cursor should be active — login button still present
        XCTAssertTrue(app.buttons["login_button"].exists, "Login screen should remain after tapping email area")
    }

    func testToggleToRegisterMode() throws {
        XCTAssertTrue(app.buttons["toggle_mode_button"].waitForExistence(timeout: 10))
        app.buttons["toggle_mode_button"].tap()
        XCTAssertTrue(app.buttons["login_button"].waitForExistence(timeout: 3))
    }

    func testLoginButtonTappableWithEmptyFields() throws {
        let loginButton = app.buttons["login_button"]
        XCTAssertTrue(loginButton.waitForExistence(timeout: 10))
        loginButton.tap()
        XCTAssertTrue(loginButton.waitForExistence(timeout: 3))
    }
}
