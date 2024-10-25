package net.bladehunt.rpp.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

abstract class CleanTask : DefaultTask() {
    init {
        group = "resource pack"
        description = "Cleans resource pack sources"
    }

    @TaskAction
    fun compile() {
    }
}