package net.bladehunt.rpp.task

import net.bladehunt.rpp.output.BuildContext
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

abstract class BuildTask : DefaultTask() {
    init {
        group = "resource pack"
        description = "Archives the resource pack and generates a hash"
    }

    @TaskAction
    fun compile() {
        BuildContext.fromTask(this).buildTimed()
    }
}