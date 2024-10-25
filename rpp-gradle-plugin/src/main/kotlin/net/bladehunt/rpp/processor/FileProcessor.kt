package net.bladehunt.rpp.processor

import net.bladehunt.rpp.build.BuildScope
import net.bladehunt.rpp.build.FileData

/**
 * A processor that takes advantage of rpp's file outputs
 */
interface FileProcessor<T> : Processor<T> {
    fun BuildScope.shouldExecute(file: FileData, context: T): Boolean

    fun BuildScope.process(file: FileData, context: T)
}

internal fun <T> FileProcessor<T>.shouldExecute(scope: BuildScope, file: FileData): Boolean =
    with(scope) { shouldExecute(file, scope.getOrCreateContext(this@shouldExecute)) }

internal fun <T> FileProcessor<T>.process(scope: BuildScope, file: FileData) =
    with(scope) { process(file, scope.getOrCreateContext(this@process)) }