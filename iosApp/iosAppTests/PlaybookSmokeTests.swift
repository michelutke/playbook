import XCTest

/// Smoke tests against the running Ktor backend.
/// API_BASE_URL env var is set by the CI workflow (defaults to localhost:8080).
final class PlaybookSmokeTests: XCTestCase {

    private var baseURL: String {
        ProcessInfo.processInfo.environment["API_BASE_URL"] ?? "http://localhost:8080"
    }

    // MARK: - Health

    func testHealthEndpoint() async throws {
        let url = URL(string: "\(baseURL)/health")!
        let (_, response) = try await URLSession.shared.data(from: url)
        XCTAssertEqual((response as? HTTPURLResponse)?.statusCode, 200)
    }

    // MARK: - Auth

    func testRegisterNewUser() async throws {
        let url = URL(string: "\(baseURL)/auth/register")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "email": "smoke-\(UUID().uuidString)@playbook.test",
            "password": "testpassword123",
            "displayName": "Smoke Test User"
        ])

        let (data, response) = try await URLSession.shared.data(for: request)
        XCTAssertEqual((response as? HTTPURLResponse)?.statusCode, 200)

        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        XCTAssertNotNil(json?["token"], "Response must contain a JWT token")
        XCTAssertNotNil(json?["userId"], "Response must contain a userId")
    }

    func testLoginWithSeededUser() async throws {
        let url = URL(string: "\(baseURL)/auth/login")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "email": "test@playbook.test",
            "password": "testpassword"
        ])

        let (data, response) = try await URLSession.shared.data(for: request)
        XCTAssertEqual((response as? HTTPURLResponse)?.statusCode, 200)

        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        XCTAssertNotNil(json?["token"])
    }

    // MARK: - Clubs (authenticated)

    func testFetchClubsRequiresAuth() async throws {
        let url = URL(string: "\(baseURL)/clubs")!
        let (_, response) = try await URLSession.shared.data(from: url)
        // Unauthenticated request should be rejected
        XCTAssertEqual((response as? HTTPURLResponse)?.statusCode, 401)
    }
}
