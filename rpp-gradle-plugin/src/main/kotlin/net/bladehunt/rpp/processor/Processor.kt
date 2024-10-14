package net.bladehunt.rpp.processor

import net.bladehunt.rpp.output.BuildContext

@JvmSynthetic
inline fun Processor(
    priority: Int = 0,
    crossinline process: (context: BuildContext) -> Unit
): Processor = object : Processor(priority) {
    override fun process(context: BuildContext) = process(context)
}

abstract class Processor(val priority: Int) {
    /**
     * @param context The current task's BuildContext
     */
    abstract fun process(context: BuildContext)
}