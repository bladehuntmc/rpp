package net.bladehunt.rpp.processor

interface Processor<T> {
    /**
     * Context is shared while the current session is active
     */
    fun createContext(): T
}