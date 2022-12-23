package com.outoftheboxrobotics.stateful.states

import com.outoftheboxrobotics.stateful.statemachines.StateMachine

/**
 * A [StateMachine] that runs other state machines synchronously when updated.
 *
 * @param stateMachines State machines that are updated when this state machine updates.
 */
class LinkedStateMachine<out T>(
    vararg val stateMachines: StateMachine<out T>
) : StateMachine<Array<out StateMachine<*>>>(
    object : State<Array<out StateMachine<*>>> {
        override val value = stateMachines

        override fun run() = also { stateMachines.forEach { it.update() } }
    }
)

// Operators for generating a LinkedStateMachine
operator fun <C, T : C, R : C> StateMachine<T>.plus(other: StateMachine<R>) =
    LinkedStateMachine(this, other)
operator fun <C, T : C, R : C> LinkedStateMachine<T>.plus(other: LinkedStateMachine<R>) =
    LinkedStateMachine(*this.stateMachines, *other.stateMachines)
operator fun <C, T : C, R : C> LinkedStateMachine<T>.plus(other: StateMachine<R>) =
    LinkedStateMachine(*this.stateMachines, other)
operator fun <C, T : C, R : C> StateMachine<T>.plus(other: LinkedStateMachine<R>) =
    LinkedStateMachine(this, *other.stateMachines)