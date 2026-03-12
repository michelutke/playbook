package com.playbook.haptic

actual fun performHapticFeedback(type: HapticType) {
    // Android Compose callers use LocalHapticFeedback directly in composable scope.
    // This no-op satisfies the expect for non-composable callsites.
}
