package com.outoftheboxrobotics.stateful

import com.outoftheboxrobotics.stateful.builders.buildLinearStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrentTest {
    @Test
    fun launchScopeTest() {
        var s = ""
        buildLinearStateMachine {
            scope {
                launch{
                    waitMillis(50)
                    task { s += "2" }
                }

                task { s += "1" }
            }

            task { s += "3" }

            launch {
                waitMillis(50)
                task { s += "4" }
            }
        }.run {
            while (!isFinished) { update() }
            assertEquals("1234", s)
        }
    }

    @Test
    fun asyncTest() {
        var s = ""

        buildLinearStateMachine {
            val a = launch {
                waitMillis(100)
                task { s += "2" }
            }
            waitMillis(50)
            launch {
                waitMillis(20)
                task { s += "1" }
            }
            await(a)
            task { s += "3" }
        }.run {
            while (!isFinished) { update() }
            assertEquals("123", s)
        }
    }
}