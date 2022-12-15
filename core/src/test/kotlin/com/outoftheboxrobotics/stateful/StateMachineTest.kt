package com.outoftheboxrobotics.stateful

import com.outoftheboxrobotics.stateful.builders.buildStateMachine
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class StateMachineTest {
    @Test
    fun stateMachineBuilderTest() {
        var str = ""

        val fsm = buildStateMachine {
            val s0 = createState(0)
            val s1 = createState(1)
            val s2 = createState(2)
            val s3 = createState(3)

            s0.onRun {
                str += value.toString()
                s1
            }

            s1.onRun {
                str += value.toString()

                if (str.length == 2) s3 else s2
            }

            s2.onRun {
                str += value.toString()
                this
            }

            s3.onRun {
                str += value.toString()
                s2
            }

            startingState = s0
        }

        fsm.update()
        assertEquals("0", str)

        fsm.update()
        fsm.update()
        assertEquals("013", str)

        fsm.update()
        assertEquals("0132", str)
        assertEquals(2, fsm.currentState.value)
    }

    @Test
    fun stateMachineExceptionTest() {
        assertThrows<IllegalArgumentException> {
            buildStateMachine {
                val s = createState(0)

                startingState = s
            }
        }

        assertThrows<IllegalArgumentException> {
            buildStateMachine {
                val s = createState(0)

                s.onRun { this }
            }
        }
    }
}