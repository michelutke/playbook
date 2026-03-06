package com.playbook.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.playbook.test.TestApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class)
class OfflineIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun offlineQueueBadge_visibleWhenCountGreaterThanZero() {
        composeTestRule.setContent {
            OfflineQueueBadge(pendingCount = 3)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun offlineQueueBadge_hiddenWhenCountIsZero() {
        composeTestRule.setContent {
            OfflineQueueBadge(pendingCount = 0)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun syncingSnackbar_showsWhenSyncing() {
        composeTestRule.setContent {
            SyncingSnackbar(isSyncing = true)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Syncing\u2026").assertIsDisplayed()
    }
}
