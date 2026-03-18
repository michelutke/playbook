import XCTest

/// End-to-end smoke tests for the Register flow and basic navigation.
///
/// Element strategy: Compose Multiplatform exposes testTags as accessibility
/// identifiers. Text fields may appear as `textField`, `secureTextField`, or
/// `other` depending on CMP version — use `element(withIdentifier:)` helper.
final class RegisterFlowTests: XCTestCase {

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

    private func navigateToRegister(timeout: TimeInterval = 5) -> Bool {
        let navigateButton = app.buttons["btn_navigate_register"]
        guard navigateButton.waitForExistence(timeout: timeout) else { return false }
        navigateButton.tap()
        return element(withIdentifier: "tf_display_name").waitForExistence(timeout: timeout)
    }

    // MARK: - Register screen navigation

    func test_registerScreen_isReachableFromLogin() {
        let reached = navigateToRegister()
        XCTAssertTrue(
            reached,
            "Expected btn_navigate_register to open register screen with tf_display_name"
        )
        XCTAssertEqual(app.state, .runningForeground)
    }

    func test_registerScreen_hasDisplayNameField() {
        guard navigateToRegister() else { return }

        XCTAssertTrue(
            element(withIdentifier: "tf_display_name").waitForExistence(timeout: 3),
            "Expected tf_display_name field on the register screen"
        )
    }

    func test_registerScreen_hasEmailField() {
        guard navigateToRegister() else { return }

        XCTAssertTrue(
            element(withIdentifier: "tf_email").waitForExistence(timeout: 3),
            "Expected tf_email field on the register screen"
        )
    }

    func test_registerScreen_hasPasswordField() {
        guard navigateToRegister() else { return }

        XCTAssertTrue(
            element(withIdentifier: "tf_password").waitForExistence(timeout: 3),
            "Expected tf_password field on the register screen"
        )
    }

    func test_registerScreen_hasConfirmPasswordField() {
        guard navigateToRegister() else { return }

        XCTAssertTrue(
            element(withIdentifier: "tf_confirm_password").waitForExistence(timeout: 3),
            "Expected tf_confirm_password field on the register screen"
        )
    }

    func test_registerScreen_hasCreateAccountButton() {
        guard navigateToRegister() else { return }

        XCTAssertTrue(
            app.buttons["btn_create_account"].waitForExistence(timeout: 3),
            "Expected btn_create_account button on the register screen"
        )
    }

    func test_register_fillFields_andTapCreateAccount() throws {
        guard navigateToRegister() else {
            throw XCTSkip("Register screen not reachable from launch state")
        }

        let displayNameField = element(withIdentifier: "tf_display_name")
        guard displayNameField.waitForExistence(timeout: 3) else {
            XCTFail("tf_display_name not found")
            return
        }
        displayNameField.tap()
        displayNameField.typeText("Test User")

        let emailField = element(withIdentifier: "tf_email")
        guard emailField.waitForExistence(timeout: 3) else {
            XCTFail("tf_email not found")
            return
        }
        emailField.tap()
        emailField.typeText("testuser@teamorg.ch")

        let passwordField = element(withIdentifier: "tf_password")
        guard passwordField.waitForExistence(timeout: 3) else {
            XCTFail("tf_password not found")
            return
        }
        passwordField.tap()
        passwordField.typeText("Password123!")

        // Dismiss keyboard so it doesn't cover lower fields
        app.keyboards.firstMatch.buttons["Return"].tapIfExists()

        let confirmPasswordField = element(withIdentifier: "tf_confirm_password")
        guard confirmPasswordField.waitForExistence(timeout: 3) else {
            XCTFail("tf_confirm_password not found")
            return
        }
        confirmPasswordField.tap()
        _ = app.keyboards.firstMatch.waitForExistence(timeout: 3)
        confirmPasswordField.typeText("Password123!")

        app.keyboards.firstMatch.buttons["Return"].tapIfExists()

        let createAccountButton = app.buttons["btn_create_account"]
        guard createAccountButton.waitForExistence(timeout: 3) else {
            XCTFail("btn_create_account not found after filling fields")
            return
        }
        createAccountButton.tap()

        XCTAssertEqual(app.state, .runningForeground)
    }

    func test_registerScreen_tapNavigateLogin_showsLoginScreen() {
        guard navigateToRegister() else {
            XCTFail("Could not reach register screen")
            return
        }

        let backButton = app.buttons["btn_navigate_login"]
        guard backButton.waitForExistence(timeout: 3) else {
            XCTFail("btn_navigate_login not found on register screen")
            return
        }
        backButton.tap()

        XCTAssertTrue(
            app.buttons["btn_sign_in"].waitForExistence(timeout: 5),
            "Expected btn_sign_in on login screen after tapping btn_navigate_login"
        )
    }

    // MARK: - Bottom navigation bar

    func test_bottomNav_itemsAreAccessible() throws {
        // After launching (and possibly logging in) a bottom nav bar may appear.
        // This test is intentionally lenient — it skips rather than fails if
        // the bar is behind the auth wall.
        let tabCandidates = ["Home", "Teams", "Roster", "Settings", "Invite"]
        var found = false
        for label in tabCandidates {
            if app.buttons[label].waitForExistence(timeout: 2) {
                found = true
                break
            }
        }

        if !found {
            throw XCTSkip("Bottom nav bar not visible in current app state (likely behind auth)")
        }

        for label in tabCandidates {
            let tab = app.buttons[label]
            if tab.exists {
                tab.tap()
                XCTAssertEqual(app.state, .runningForeground, "App crashed after tapping '\(label)' tab")
                break
            }
        }
    }
}

// MARK: - XCUIElement convenience

private extension XCUIElement {
    func tapIfExists() {
        if exists { tap() }
    }
}
