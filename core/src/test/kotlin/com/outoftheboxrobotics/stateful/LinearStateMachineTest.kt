package com.outoftheboxrobotics.stateful

import com.outoftheboxrobotics.stateful.builders.buildLinearStateMachine
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinearStateMachineTest {
    @Test
    fun taskDelayTest() {
        var s = ""

        val fsm = buildLinearStateMachine {
            task { s += '1' }
            waitMillis(100)
            task { s += '2' }
            waitMillis(200)
            task { s += '3' }
        }

        val t = measureTimeMillis {
            val start = System.currentTimeMillis()

            while (System.currentTimeMillis() - start < 400 && !fsm.isFinished) fsm.update()
        }

        assertEquals("123", s)
        assertTrue(t in 200..400)

        val fsm2 = buildLinearStateMachine {
            runStateMachine(fsm)
        }

        fsm2.update()

        assertTrue(fsm.isFinished)
        assertFalse(fsm2.isFinished)
        assertEquals("1231", s)
    }
}