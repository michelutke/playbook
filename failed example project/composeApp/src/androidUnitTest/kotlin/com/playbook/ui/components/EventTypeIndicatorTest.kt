package com.playbook.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.playbook.domain.EventType
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
class EventTypeIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun eventTypeChip_training_showsTrainingLabel() {
        composeTestRule.setContent {
            EventTypeChip(type = EventType.TRAINING)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("\uD83C\uDFC3 Training").assertIsDisplayed()
    }

    @Test
    fun eventTypeChip_match_showsMatchLabel() {
        composeTestRule.setContent {
            EventTypeChip(type = EventType.MATCH)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("\u26BD Match").assertIsDisplayed()
    }
}
