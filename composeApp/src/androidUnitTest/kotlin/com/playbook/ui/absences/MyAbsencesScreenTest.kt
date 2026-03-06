package com.playbook.ui.absences

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.playbook.test.FakeAbwesenheitRepository
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
class MyAbsencesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private fun makeViewModel() = MyAbsencesViewModel(
        abwesenheitRepository = FakeAbwesenheitRepository(rules = emptyList()),
    )

    @Test
    fun emptyState_showsNoAbsencesDeclaredMessage() {
        val vm = makeViewModel()
        composeTestRule.setContent {
            MyAbsencesScreen(onNavigateBack = {}, viewModel = vm)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No absences declared").assertIsDisplayed()
    }

    @Test
    fun fab_isVisible() {
        val vm = makeViewModel()
        composeTestRule.setContent {
            MyAbsencesScreen(onNavigateBack = {}, viewModel = vm)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Add absence").assertIsDisplayed()
    }
}
