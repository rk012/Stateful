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

    @Test
    fun waitUntilTest() {
        var a = false
        var s = ""

        buildLinearStateMachine {
            task { s += '1' }
            waitUntil { a }
            task { s += '2' }
        }.run {
            update()
            assertEquals("1", s)
            update()
            assertEquals("1", s)
            a = true
            update()
            update()
            assertEquals("12", s)
        }
    }

    @Test
    fun loopInvokeTest() {
        var c = 0
        buildLinearStateMachine {
            loopWhile({ c < 3 }) {
                runStateMachine(buildLinearStateMachine {
                    task { c++ }
                })
            }
        }.run {
            while (!isFinished) { update() }
            assertEquals(3, c)
        }
    }

    @Test
    fun loopTest() {
        var i = 0
        var c = 0

        val fsm = buildLinearStateMachine {
            loopWhile({ i < 3 }) {
                task { c++ }
                task { i++ }
            }
        }

        repeat(10) { fsm.update() }
        assertEquals(3, c)

        val fsm2 = fsm.createNew()
        i = 0

        repeat(10) { fsm2.update() }
        assertEquals(6, c)

        // Should check while condition first
        val fsm3 = buildLinearStateMachine {
            loopWhile({ false }) {
                task { c++ }
            }
        }

        repeat(10) { fsm3.update() }
        assertEquals(6, c)
    }

    @Test
    fun conditionalTest() {
        // Conditionals and loops may take multiple updates to complete
        val n = 5

        var foo: Boolean? = null
        var bar: Boolean? = true

        buildLinearStateMachine {
            loopWhile({ true }) {
                runIf({ foo == true }) {
                    task { bar = true }
                }.elif({ foo == false }) {
                    task { bar = false }
                } elseRun {
                    task { bar = null }
                }
            }
        }.run {
            repeat(n) { update() }
            assertEquals(null, bar)

            foo = true
            repeat(n) { update() }
            assertEquals(true, bar)

            foo = false
            repeat(n) { update() }
            assertEquals(false, bar)

            foo = null
            createNew().let { fsm ->
                repeat(n) { fsm.update() }
                assertEquals(null, bar)
            }
        }
    }

    @Test
    fun concurrentLocalVarTest() {
        var i = 0

        val fsm = buildLinearStateMachine {
            waitMillis(100)
            task { i++ }
        }

        val machines = List(100) { fsm.createNew() }

        val t = measureTimeMillis {
            val start = System.currentTimeMillis()

            while (System.currentTimeMillis() - start < 200 && machines.any { !it.isFinished }) {
                machines.forEach { it.update() }
            }
        }

        assertEquals(100, i)
        assertTrue(t in 50..150)
    }

    @Test
    fun updateCountTest() {
        var i = 0
        var j = 0

        buildLinearStateMachine {
            task { i++}

            runStateMachine(buildLinearStateMachine {
                task { i++ }
            })
            task { i++}

            runIf({ false }) {
                task { i++ }
            }.elif({ true }) {
                task { i++ }
            } elseRun {
                task { i++ }
            }

            task { i++ }

            runIf({ false }) {
                task { i++ }
            }.elif({ false }) {
                task { i++ }
            }

            loopTaskWhile({ j < 3 }) {
                i++
                j++
            }

            task { i++ }
            task { i++ }
        }.run {
            repeat(11) {
                update()
            }
            // Lags behind by two due to second if statement and loop end
            assertEquals(9, i)
            update()
            assertEquals(10, i)
        }
    }
}