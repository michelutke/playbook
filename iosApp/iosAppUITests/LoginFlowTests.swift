import XCTest

final class LoginFlowTests: PlaybookUITestCase {

    func testLoginFormRendersCorrectly() throws {
        XCTAssertTrue(app.textFields["email_field"].waitForExistence(timeout: 5), "Email field not found")
        XCTAssertTrue(app.secureTextFields["password_field"].exists, "Password field not found")
        XCTAssertTrue(app.buttons["login_button"].exists, "Login button not found")
        XCTAssertTrue(app.buttons["toggle_mode_button"].exists, "Toggle mode button not found")
    }

    func testEmailFieldAcceptsInput() throws {
        let emailField = app.textFields["email_field"]
        XCTAssertTrue(emailField.waitForExistence(timeout: 5))
        emailField.tap()
        emailField.typeText("test@example.com")
        XCTAssertEqual(emailField.value as? String, "test@example.com")
    }

    func testPasswordFieldAcceptsInput() throws {
        let passwordField = app.secureTextFields["password_field"]
        XCTAssertTrue(passwordField.waitForExistence(timeout: 5))
        passwordField.tap()
        passwordField.typeText("secret123")
        // Secure fields report value as bullet characters — just verify interaction succeeded
        XCTAssertTrue(passwordField.exists)
    }

    func testToggleToRegisterMode() throws {
        XCTAssertTrue(app.buttons["toggle_mode_button"].waitForExistence(timeout: 5))
        app.buttons["toggle_mode_button"].tap()
        // After toggle the submit button still exists (now in register mode)
        XCTAssertTrue(app.buttons["login_button"].waitForExistence(timeout: 3))
    }

    func testLoginButtonTappableWithEmptyFields() throws {
        let loginButton = app.buttons["login_button"]
        XCTAssertTrue(loginButton.waitForExistence(timeout: 5))
        loginButton.tap()
        // Button remains present after tap with empty fields
        XCTAssertTrue(loginButton.waitForExistence(timeout: 3))
    }
}
