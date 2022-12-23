package com.outoftheboxrobotics.stateful

import com.outoftheboxrobotics.stateful.builders.buildStateMachine
import com.outoftheboxrobotics.stateful.states.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkedStateMachineTest {
    @Test
    fun linkedUpdateTest() {
        var s = ""

        val a = buildStateMachine {
            startingState = createState(Unit).apply {
                onRun { s += "a"; this }
            }
        }

        val b = buildStateMachine {
            startingState = createState(Unit).apply {
                onRun { s += "b"; this }
            }
        }

        val l = a + b

        l.update()
        assertEquals("ab", s)

        s = ""
        (l + a).update()
        assertEquals("aba", s)

        s = ""
        (b + l).update()
        assertEquals("bab", s)

        s = ""
        (l + (b + a)).update()
        assertEquals("abba", s)
    }
}