package com.playbook.haptic

enum class HapticType { Light, Medium, Heavy }

expect fun performHapticFeedback(type: HapticType = HapticType.Medium)
