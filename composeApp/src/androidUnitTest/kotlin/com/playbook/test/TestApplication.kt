package com.playbook.test

import android.app.Application

/**
 * Minimal Application for Robolectric tests.
 * Does NOT start Koin — tests instantiate ViewModels directly.
 */
class TestApplication : Application()
