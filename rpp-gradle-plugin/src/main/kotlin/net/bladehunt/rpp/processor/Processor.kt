package net.bladehunt.rpp.processor

import net.bladehunt.rpp.build.BuildScope

interface Processor<T> {
    /**
     * Context is shared while the current session is active
     */
    fun BuildScope.createContext(): T
}

internal fun <T> Processor<T>.createContext(scope: BuildScope): T = with(scope) { createContext() }