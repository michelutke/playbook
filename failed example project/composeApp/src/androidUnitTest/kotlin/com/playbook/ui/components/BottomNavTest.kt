package com.playbook.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class BottomNavTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun homeTab_isSelectedWhenIsHomeSelectedTrue() {
        composeTestRule.setContent {
            PlaybookBottomBar(
                isHomeSelected = true,
                isNotificationsSelected = false,
                unreadCount = 0,
                onHomeClick = {},
                onNotificationsClick = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Home").assertIsSelected()
    }

    @Test
    fun notificationBadge_showsUnreadCount() {
        composeTestRule.setContent {
            PlaybookBottomBar(
                isHomeSelected = false,
                isNotificationsSelected = false,
                unreadCount = 5,
                onHomeClick = {},
                onNotificationsClick = {},
            )
        }
        composeTestRule.waitForIdle()
        // Badge text node exists in the tree (may be small but present)
        composeTestRule.onNodeWithText("5", useUnmergedTree = true).assertExists()
    }

    @Test
    fun notificationBadge_hiddenWhenUnreadCountIsZero() {
        composeTestRule.setContent {
            PlaybookBottomBar(
                isHomeSelected = false,
                isNotificationsSelected = false,
                unreadCount = 0,
                onHomeClick = {},
                onNotificationsClick = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("0", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun notificationsTab_click_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            PlaybookBottomBar(
                isHomeSelected = true,
                isNotificationsSelected = false,
                unreadCount = 0,
                onHomeClick = {},
                onNotificationsClick = { clicked = true },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Notifications").performClick()
        composeTestRule.waitForIdle()
        assert(clicked) { "onNotificationsClick should have been invoked" }
    }
}
