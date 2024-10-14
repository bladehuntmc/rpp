package net.bladehunt.rpp.processor.file

import net.bladehunt.rpp.output.BuildContext
import net.bladehunt.rpp.output.FileProcessResult
import java.io.File
import java.io.FileFilter

@JvmSynthetic
inline fun FileProcessor(
    fileFilter: FileFilter = FileFilter { true },
    priority: Int = 0,
    crossinline processFile: (context: BuildContext, source: File) -> FileProcessResult
): FileProcessor = object : FileProcessor(fileFilter, priority) {
    override fun processFile(context: BuildContext, source: File): FileProcessResult =
        processFile(context, source)
}

abstract class FileProcessor(val fileFilter: FileFilter, val priority: Int) {
    /**
     * @param context The current task's BuildContext
     * @param source The copied file from the source
     */
    abstract fun processFile(context: BuildContext, source: File): FileProcessResult
}