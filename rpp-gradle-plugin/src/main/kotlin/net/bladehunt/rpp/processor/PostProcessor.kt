package net.bladehunt.rpp.processor

import net.bladehunt.rpp.build.BuildScope
import net.bladehunt.rpp.build.FileData
import net.bladehunt.rpp.util.tree.DiffResult

interface PostProcessor<T> : Processor<T> {
    fun BuildScope.process(diffResult: DiffResult<FileData>)
}

internal fun <T> PostProcessor<T>.process(scope: BuildScope, diff: DiffResult<FileData>) = with(scope) { this.process(diff) }