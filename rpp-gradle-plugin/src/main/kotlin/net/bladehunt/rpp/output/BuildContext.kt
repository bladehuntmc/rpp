package net.bladehunt.rpp.output

import net.bladehunt.rpp.processor.file.FileProcessor
import net.bladehunt.rpp.processor.Processor
import org.slf4j.Logger
import java.io.File

data class BuildContext(
    val logger: Logger,

    val buildDirectory: File,
    val outputDirectory: File,
    val sourcesDirectory: File,

    val fileProcessors: List<FileProcessor>,
    val outputProcessors: List<Processor>,
    val archiveProcessors: List<Processor>,

    val generatedArchives: MutableList<Archive> = arrayListOf()
) {
    /**
     * Processes the file, stopping after it is deleted.
     *
     * @return True if successfully completed without deletion, otherwise false
     */
    fun processFile(file: File) = fileProcessors
        .filter { it.fileFilter.accept(file) }
        .all { it.processFile(this, file) == FileProcessResult.CONTINUE }
}