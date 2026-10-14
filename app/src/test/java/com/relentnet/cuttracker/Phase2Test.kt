package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.Block
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.engine.ProgramEngine
import kotlin.test.Test
import kotlin.test.assertEquals

class Phase2Test {
    @Test fun phase1Blocks() {
        val s = Settings()
        assertEquals(listOf("dips", "pushups"), ProgramEngine.exercisesFor(Block.CLIMB, s).map { it.id })
        assertEquals(listOf("roller", "legraise", "floorpress", "pullover"), ProgramEngine.exercisesFor(Block.OFF, s).map { it.id })
        assertEquals(3, ProgramEngine.setsFor(s.exercises.first { it.id == "dips" }, 1))
        assertEquals(2250, s.calories)
    }

    @Test fun phase2Blocks() {
        val s = Settings(phase = 2)
        val climb = ProgramEngine.exercisesFor(Block.CLIMB, s)
        assertEquals(4, ProgramEngine.setsFor(climb.first { it.id == "dips" }, 2))
        assertEquals(2, ProgramEngine.setsFor(climb.first { it.id == "pushups" }, 2))
        val off = ProgramEngine.exercisesFor(Block.OFF, s)
        assertEquals(listOf("roller", "legraise", "floorpress", "inclinepress", "pullover"), off.map { it.id })
        assertEquals(4, ProgramEngine.setsFor(off.first { it.id == "floorpress" }, 2))
        assertEquals(3, ProgramEngine.setsFor(off.first { it.id == "inclinepress" }, 2))
        assertEquals(2, ProgramEngine.setsFor(off.first { it.id == "pullover" }, 2))
        assertEquals(1, ProgramEngine.setsFor(off.first { it.id == "roller" }, 2))
        assertEquals(2700, s.calories)
    }
}
