package com.relentnet.cuttracker

import com.relentnet.cuttracker.ui.weight.chartRange
import org.junit.Assert.assertEquals
import org.junit.Test

/** The Weight chart's y range stretches to the projection, but never more than 5 lb past the start or the goal. */
class ChartRangeTest {
    @Test fun startToGoalWithNoReadings() {
        assertEquals(182.0 to 170.0, chartRange(181.8, 170.0, emptyList(), null))
    }

    @Test fun projectionBelowGoalStretchesTheBottom() {
        assertEquals(182.0 to 168.0, chartRange(181.8, 170.0, listOf(181.0), 168.4))
    }

    @Test fun wildProjectionIsClampedToFiveLb() {
        assertEquals(182.0 to 165.0, chartRange(181.8, 170.0, listOf(181.0), 150.0))
        assertEquals(187.0 to 170.0, chartRange(181.8, 170.0, listOf(181.0), 199.0))
    }
}
