package net.bladehunt.rpp.build

import java.io.File

data class FileData(
    val source: File,

    /**
     * Used for tracking modifications and deletions
     */
    val outputs: MutableList<File>
)