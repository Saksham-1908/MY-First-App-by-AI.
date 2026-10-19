package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TimeTrack", appName)
    }

    @Test
    fun `task item duration calculation`() {
        val task = TaskItem(
            title = "Team Standup",
            date = "2026-10-19",
            startTime = "09:00",
            endTime = "10:30"
        )
        assertEquals(90, task.calculateDurationMinutes())
        assertEquals("1h 30m", task.formattedDuration())
    }
}
