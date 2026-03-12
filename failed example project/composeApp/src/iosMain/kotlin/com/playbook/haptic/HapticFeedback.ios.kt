package com.playbook.haptic

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual fun performHapticFeedback(type: HapticType) {
    val style = when (type) {
        HapticType.Light -> UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
        HapticType.Medium -> UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
        HapticType.Heavy -> UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
    }
    UIImpactFeedbackGenerator(style).impactOccurred()
}
