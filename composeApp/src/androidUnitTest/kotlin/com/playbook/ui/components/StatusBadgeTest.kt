package com.playbook.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.playbook.domain.AttendanceRecordStatus
import com.playbook.domain.AttendanceResponseStatus
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
class StatusBadgeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun statusBadge_confirmed_showsConfirmedLabel() {
        composeTestRule.setContent {
            StatusBadge(status = AttendanceResponseStatus.CONFIRMED)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Confirmed").assertIsDisplayed()
    }

    @Test
    fun statusBadge_declined_showsDeclinedLabel() {
        composeTestRule.setContent {
            StatusBadge(status = AttendanceResponseStatus.DECLINED)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Declined").assertIsDisplayed()
    }

    @Test
    fun statusBadge_noResponse_showsNoResponseLabel() {
        composeTestRule.setContent {
            StatusBadge(status = AttendanceResponseStatus.NO_RESPONSE)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No response").assertIsDisplayed()
    }

    @Test
    fun attendanceRecordBadge_present_showsPresentLabel() {
        composeTestRule.setContent {
            AttendanceRecordBadge(status = AttendanceRecordStatus.PRESENT)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Present").assertIsDisplayed()
    }
}
