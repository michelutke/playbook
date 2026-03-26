import XCTest

/// End-to-end smoke tests for the Login flow.
///
/// Element strategy: Compose Multiplatform exposes testTags as accessibility
/// identifiers. Text fields may appear as `textField`, `secureTextField`, or
/// `other` depending on CMP version — use `element(withIdentifier:)` helper.
final class LoginFlowTests: XCTestCase {

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

    // MARK: - Helpers

    /// Finds any element by accessibility identifier, regardless of type.
    private func element(withIdentifier id: String) -> XCUIElement {
        let predicate = NSPredicate(format: "identifier == %@", id)
        return app.descendants(matching: .any).matching(predicate).firstMatch
    }

    private func waitForLoginScreen(timeout: TimeInterval = 5) -> Bool {
        app.buttons["btn_sign_in"].waitForExistence(timeout: timeout)
    }

    // MARK: - Tests

    func test_loginScreen_emailFieldVisible() {
        guard waitForLoginScreen() else {
            XCTFail("Login screen did not appear within timeout")
            return
        }

        XCTAssertTrue(
            element(withIdentifier: "tf_email").waitForExistence(timeout: 3),
            "Expected tf_email field on the login screen"
        )
    }

    func test_loginScreen_passwordFieldVisible() {
        guard waitForLoginScreen() else {
            XCTFail("Login screen did not appear within timeout")
            return
        }

        XCTAssertTrue(
            element(withIdentifier: "tf_password").waitForExistence(timeout: 3),
            "Expected tf_password field on the login screen"
        )
    }

    func test_loginScreen_loginButtonVisible() {
        guard waitForLoginScreen() else {
            XCTFail("Login screen did not appear within timeout")
            return
        }

        XCTAssertTrue(
            app.buttons["btn_sign_in"].waitForExistence(timeout: 3),
            "Expected btn_sign_in button on the login screen"
        )
    }

    func test_login_enterCredentials_andTapLogin() {
        guard waitForLoginScreen() else {
            XCTFail("Login screen did not appear within timeout")
            return
        }

        let emailField = element(withIdentifier: "tf_email")
        guard emailField.waitForExistence(timeout: 3) else {
            XCTFail("tf_email field not found")
            return
        }
        emailField.tap()
        emailField.typeText("testuser@teamorg.ch")

        let passwordField = element(withIdentifier: "tf_password")
        guard passwordField.waitForExistence(timeout: 3) else {
            XCTFail("tf_password field not found")
            return
        }
        passwordField.tap()
        passwordField.typeText("Password123!")

        // Dismiss keyboard by tapping outside the text field
        app.tap()

        let loginButton = app.buttons["btn_sign_in"]
        guard loginButton.waitForExistence(timeout: 3) else {
            XCTFail("btn_sign_in button not found")
            return
        }
        loginButton.tap()

        // After tapping login the app should either navigate away or show an
        // error — both mean the interaction worked without crashing.
        XCTAssertEqual(app.state, .runningForeground)
    }

    func test_loginScreen_tapNavigateRegister_showsRegisterScreen() {
        guard waitForLoginScreen() else {
            XCTFail("Login screen did not appear within timeout")
            return
        }

        let navigateButton = app.buttons["btn_navigate_register"]
        guard navigateButton.waitForExistence(timeout: 3) else {
            XCTFail("btn_navigate_register not found on login screen")
            return
        }
        navigateButton.tap()

        XCTAssertTrue(
            element(withIdentifier: "tf_display_name").waitForExistence(timeout: 5),
            "Expected tf_display_name on register screen after tapping btn_navigate_register"
        )
    }
}

// MARK: - XCUIElement convenience

private extension XCUIElement {
    /// Taps the element only if it exists, ignoring it otherwise.
    func tapIfExists() {
        if exists { tap() }
    }
}
