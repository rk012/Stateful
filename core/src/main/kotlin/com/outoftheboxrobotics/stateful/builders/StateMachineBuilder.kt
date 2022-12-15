package com.outoftheboxrobotics.stateful.builders

import com.outoftheboxrobotics.stateful.StateRef

@StateMachineDslMarker
class StateMachineBuilder<T> internal constructor() {
    fun createState(value: T) = StateRef(value)
}