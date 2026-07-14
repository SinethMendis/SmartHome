package com.example.smarthome.data

import com.example.smarthome.data.model.DeviceDto
import com.example.smarthome.domain.Device
import com.example.smarthome.domain.DeviceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapperTest {

    @Test
    fun `DeviceDto to Outlet domain mapping`() {
        val dto = DeviceDto(
            name = "Test Outlet",
            type = "outlet",
            state = "ON",
            positionX = 0.5,
            positionY = 0.8
        )
        
        val domain = dto.toDomain("test-id")
        
        assertTrue(domain is Device.Outlet)
        assertEquals("Test Outlet", domain.name)
        assertEquals(DeviceState.ON, domain.state)
        assertEquals(0.5f, domain.position.x)
        assertEquals(0.8f, domain.position.y)
    }

    @Test
    fun `DeviceDto with invalid state defaults to OFF`() {
        val dto = DeviceDto(
            type = "outlet",
            state = "INVALID_STATE"
        )
        
        val domain = dto.toDomain("test-id")
        assertEquals(DeviceState.OFF, domain.state)
    }
}
