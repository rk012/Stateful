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

            s0.resolveState {
                str += value.toString()
                s1
            }

            s1.resolveState {
                str += value.toString()

                if (str.length == 2) s3 else s2
            }

            s2.resolveState {
                str += value.toString()
                this
            }

            s3.resolveState {
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

                s.resolveState { this }
            }
        }

        assertThrows<IllegalStateException> {
            buildStateMachine {
                createState(Unit).run()
            }
        }
    }

    @Test
    @Suppress("RemoveExplicitTypeArguments")
    fun stateMachineMatcherTest() {
        var str = ""
        var count = 0

        var temp = 25

        buildStateMachine<IntRange> {
            val s = createState(-100 until 0)
            val l = createState(0 until 100)
            val g = createState(100 until 1000)

            // Solid or gas
            where { it.value.last < 0 || it.value.first >= 100 } resolveState {
                if (temp in value) this else l.also { str += "l" }
            }

            // Fallback to liquid
            allStates resolveState {
                when {
                    temp in value -> this
                    temp >= value.last -> g.also { str += 'g' }
                    else -> s.also { str += 's' }
                }
            }

            // Increase count 2 times per update
            repeat(2) {
                allStates alsoRun {
                    count++
                }
            }

            g.alsoRun {
                if (temp > 200) str += 'G'
            }

            startingState = l
        }.run {
            update()
            temp = -2
            repeat(2) { update() }
            temp = 106
            repeat(3) { update() }
            temp = 72
            repeat(6) { update() }

            assertEquals("slgl", str)
            assertEquals(24, count)

            temp = 201
            repeat(3) { update() }
            assertEquals("slglgGG", str)
        }
    }
}