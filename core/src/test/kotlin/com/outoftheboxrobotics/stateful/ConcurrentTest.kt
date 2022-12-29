package com.outoftheboxrobotics.stateful

import com.outoftheboxrobotics.stateful.builders.buildLinearStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrentTest {
    @Test
    fun launchScopeTest() {
        var s = ""
        buildLinearStateMachine {
            runStateMachine(buildLinearStateMachine {
                launch(buildLinearStateMachine {
                    waitMillis(50)
                    task { s += "2" }
                })

                task { s += "1" }
            })

            task { s += "3" }

            launch(buildLinearStateMachine {
                waitMillis(50)
                task { s += "4" }
            })
        }.run {
            while (!isFinished) { update() }
            assertEquals("1234", s)
        }
    }
}