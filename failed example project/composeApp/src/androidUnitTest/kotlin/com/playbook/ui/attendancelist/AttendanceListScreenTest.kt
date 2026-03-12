package com.playbook.ui.attendancelist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.playbook.domain.AttendanceEntry
import com.playbook.domain.TeamAttendanceView
import com.playbook.test.FakeAttendanceRepository
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
class AttendanceListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun confirmedEntry_showsConfirmedSectionHeader() {
        val confirmedEntry = AttendanceEntry(userId = "user-1", displayName = "Alice")
        val vm = AttendanceListViewModel(
            eventId = "event-1",
            attendanceRepository = FakeAttendanceRepository(
                teamAttendanceView = TeamAttendanceView(confirmed = listOf(confirmedEntry)),
            ),
        )
        composeTestRule.setContent {
            AttendanceListScreen(
                eventId = "event-1",
                onNavigateBack = {},
                viewModel = vm,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Confirmed").assertIsDisplayed()
    }

    @Test
    fun noResponseEntry_showsNoResponseSection() {
        val noResponseEntry = AttendanceEntry(userId = "user-2", displayName = "Bob")
        val vm = AttendanceListViewModel(
            eventId = "event-1",
            attendanceRepository = FakeAttendanceRepository(
                teamAttendanceView = TeamAttendanceView(noResponse = listOf(noResponseEntry)),
            ),
        )
        composeTestRule.setContent {
            AttendanceListScreen(
                eventId = "event-1",
                onNavigateBack = {},
                viewModel = vm,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No Response").assertIsDisplayed()
    }
}
