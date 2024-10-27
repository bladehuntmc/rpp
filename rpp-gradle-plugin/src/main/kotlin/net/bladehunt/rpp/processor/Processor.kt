package net.bladehunt.rpp.processor

import net.bladehunt.rpp.build.ResourcePackProcessor

interface Processor<T> {
    /**
     * Context is shared while the current session is active
     */
    fun createContext(rpp: ResourcePackProcessor): T
}