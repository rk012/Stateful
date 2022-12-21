package com.outoftheboxrobotics.stateful

import com.outoftheboxrobotics.stateful.builders.buildLinearStateMachine
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
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

            while (System.currentTimeMillis() - start < 500 && !fsm.isFinished) fsm.update()
        }

        assertEquals("123", s)
        assertTrue(t in 200..400)

        val fsm2 = buildLinearStateMachine {
            runStateMachine(fsm)
        }

        val t2 = measureTimeMillis {
            val start = System.currentTimeMillis()

            while (System.currentTimeMillis() - start < 500 && !fsm2.isFinished) fsm2.update()
        }

        assertTrue(t2 in 200..400)
        assertEquals("123123", s)
    }
}