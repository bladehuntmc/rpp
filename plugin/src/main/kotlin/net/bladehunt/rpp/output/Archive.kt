package net.bladehunt.rpp.output

import net.bladehunt.rpp.util.sha1
import java.io.File

data class Archive(
    val id: String,
    val file: File,
    val sha1Hash: String = file.sha1()
)
