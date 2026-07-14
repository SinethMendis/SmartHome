package com.example.smarthome.domain

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class IronCountdownTest {

    @Test
    fun `calculate iron time left correctly`() {
        val maxDuration = 30
        val now = Instant.now()
        val turnedOnAt = now.minus(Duration.ofMinutes(10))
        
        val iron = Device.Iron(
            id = "1",
            name = "Test Iron",
            state = DeviceState.ON,
            position = Offset(0f, 0f),
            maxOnDurationMin = maxDuration,
            turnedOnAt = turnedOnAt
        )

        val elapsed = Duration.between(iron.turnedOnAt, now)
        val max = Duration.ofMinutes(iron.maxOnDurationMin.toLong())
        val timeLeft = max.minus(elapsed)

        assertEquals(20, timeLeft.toMinutes())
    }
}
