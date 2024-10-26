package net.bladehunt.rpp.processor

import net.bladehunt.rpp.build.BuildScope
import net.bladehunt.rpp.build.FileData

/**
 * A processor that takes advantage of rpp's file outputs
 */
interface FileProcessor<T> : Processor<T> {
    fun BuildScope.shouldExecute(file: FileData): Boolean

    fun BuildScope.process(file: FileData)
}

internal fun <T> FileProcessor<T>.shouldExecute(scope: BuildScope, file: FileData): Boolean =
    with(scope) { shouldExecute(file) }

internal fun <T> FileProcessor<T>.process(scope: BuildScope, file: FileData) =
    with(scope) { process(file) }