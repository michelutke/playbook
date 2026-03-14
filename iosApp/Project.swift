import ProjectDescription

// KMP framework built by: ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
// Output: composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework
let composeFramework: Path = "../../composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework"

let project = Project(
    name: "iosApp",
    targets: [
        .target(
            name: "iosApp",
            destinations: .iOS,
            product: .app,
            bundleId: "com.playbook.ios",
            deploymentTargets: .iOS("16.0"),
            infoPlist: .default,
            sources: ["iosApp/**"],
            dependencies: [
                .framework(path: composeFramework, status: .optional)
            ]
        ),
        .target(
            name: "iosAppTests",
            destinations: .iOS,
            product: .unitTests,
            bundleId: "com.playbook.ios.tests",
            deploymentTargets: .iOS("16.0"),
            sources: ["iosAppTests/**"],
            dependencies: [
                .target(name: "iosApp")
            ]
        )
    ]
)
