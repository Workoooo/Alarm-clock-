package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.Alarm
import com.example.ui.screens.AlarmCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun alarm_card_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AlarmCard(
          alarm = Alarm(
            id = 1,
            hour = 7,
            minute = 30,
            isEnabled = true,
            label = "Rise & Shine",
            isVibrate = true,
            isSound = true,
            daysOfWeek = "1,2,3,4,5",
            snoozeDurationMinutes = 10
          ),
          onToggle = {},
          onEdit = {},
          onDelete = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
