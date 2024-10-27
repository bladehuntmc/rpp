package net.bladehunt.rpp.processor

import net.bladehunt.rpp.build.BuildScope

interface PostProcessor<T> : Processor<T> {
    fun BuildScope.process()
}

internal fun <T> PostProcessor<T>.process(scope: BuildScope) = with(scope) { this.process() }