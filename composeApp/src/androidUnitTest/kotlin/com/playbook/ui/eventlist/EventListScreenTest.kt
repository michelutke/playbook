package com.playbook.ui.eventlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.playbook.test.FakeEventRepository
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
class EventListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private fun makeViewModel() = EventListViewModel(
        teamId = null,
        eventRepository = FakeEventRepository(events = emptyList()),
    )

    @Test
    fun emptyState_showsNoEventsText() {
        val vm = makeViewModel()
        composeTestRule.setContent {
            EventListScreen(
                teamId = null,
                onNavigateBack = null,
                onNavigateToDetail = {},
                onNavigateToCreate = {},
                onNavigateToCalendar = {},
                viewModel = vm,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No events").assertIsDisplayed()
    }

    @Test
    fun calendarButton_isShown() {
        val vm = makeViewModel()
        composeTestRule.setContent {
            EventListScreen(
                teamId = null,
                onNavigateBack = null,
                onNavigateToDetail = {},
                onNavigateToCreate = {},
                onNavigateToCalendar = {},
                viewModel = vm,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Calendar view").assertIsDisplayed()
    }
}
