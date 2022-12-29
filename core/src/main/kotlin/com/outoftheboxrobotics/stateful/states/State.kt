package com.outoftheboxrobotics.stateful.states

import com.outoftheboxrobotics.stateful.statemachines.StateMachine
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Represents an individual state in a [StateMachine].
 *
 * Each State can hold a [value] representing some form of data associated with the individual states in a state
 * machine.
 *
 * @see StateRef
 */
abstract class State<out T> {
    internal var activeStateMachine: StateMachine<*>? = null

    abstract val value: T

    /**
     * Runs whatever code is associated with this state.
     *
     * @return The next state to go to (can be the same one).
     */
    abstract fun run(): State<T>

    /**
     * Delegate for creating Statemachine-local variables.
     */
    class StateVar<R> internal constructor(private val default: R) : ReadWriteProperty<State<*>, R> {
        private val stateVars = mutableMapOf<StateMachine<*>?, R>()

        override fun getValue(thisRef: State<*>, property: KProperty<*>) =
            stateVars.getOrDefault(thisRef.activeStateMachine, default)

        override fun setValue(thisRef: State<*>, property: KProperty<*>, value: R) {
            thisRef.activeStateMachine?.let { StateDataHandler.addStateVar(it, this) }
            stateVars[thisRef.activeStateMachine] = value
        }
        
        fun clearData(s: StateMachine<*>) = stateVars.remove(s)
    }

    /**
     * Creates a [StateVar] that can be used to create state machine-local variables.
     *
     * @param default The default value of the variable.
     */
    protected fun <R> stateVar(default: R) = StateVar(default)
}