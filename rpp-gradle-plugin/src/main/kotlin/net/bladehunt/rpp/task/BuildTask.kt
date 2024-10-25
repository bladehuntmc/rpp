package net.bladehunt.rpp.task

import net.bladehunt.rpp.build.ResourcePackProcessor
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import kotlin.system.measureNanoTime

abstract class BuildTask : DefaultTask() {
    init {
        group = "resource pack"
        description = "Archives the resource pack and generates a hash"
    }

    @TaskAction
    fun compile() {
        val processor = ResourcePackProcessor.fromTask(this)

        processor.layout.build.output.deleteRecursively()

        logger.lifecycle("Built resource pack in ${ measureNanoTime { processor.build() } / 1_000_000 }ms")
    }
}