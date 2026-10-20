package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.data.liftW
import com.relentnet.cuttracker.data.stepped
import com.relentnet.cuttracker.data.toLb
import com.relentnet.cuttracker.data.w
import com.relentnet.cuttracker.data.wu
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    private val lb = Settings()
    private val kg = Settings(kg = true)

    @Test fun poundsShowAsStored() {
        assertEquals("182.4", lb.w(182.4))
        assertEquals("182.4 lb", lb.wu(182.4))
        assertEquals(182.4, lb.toLb(182.4), 0.0)
    }

    @Test fun kilosConvertBothWays() {
        assertEquals("82.7 kg", kg.wu(182.4))
        assertEquals(100.0, kg.toLb(45.359237), 1e-9)
        assertEquals("80", kg.liftW(kg.toLb(80.0)))
    }

    @Test fun steppersLandOnAHalfInTheShownUnit() {
        assertEquals("83.0", kg.w(kg.stepped(182.4, 1)))
        assertEquals("82.5", kg.w(kg.stepped(182.4, -1)))
        assertEquals(182.9, lb.stepped(182.4, 1), 1e-9)
        assertEquals("83.5", kg.w(kg.stepped(kg.toLb(83.0), 1)))
    }
}
