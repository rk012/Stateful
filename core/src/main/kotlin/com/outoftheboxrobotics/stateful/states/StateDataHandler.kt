package com.outoftheboxrobotics.stateful.states

import com.outoftheboxrobotics.stateful.statemachines.StateMachine

internal object StateDataHandler {
    private val stateDataHandles = mutableMapOf<StateMachine<*>, Set<State.StateVar<*>>>()

    internal fun addStateVar(stateMachine: StateMachine<*>, stateVar: State.StateVar<*>) {
        stateDataHandles[stateMachine] = stateDataHandles[stateMachine]?.let {
            it + stateVar
        } ?: setOf(stateVar)
    }

    internal fun clearData(s: StateMachine<*>) {
        stateDataHandles[s]?.let { stateVars ->
            stateVars.forEach { it.clearData(s) }
        }

        stateDataHandles.remove(s)
    }
}