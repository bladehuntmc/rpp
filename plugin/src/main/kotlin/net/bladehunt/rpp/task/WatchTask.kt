package net.bladehunt.rpp.task

import net.bladehunt.rpp.util.DirectoryWatcher
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.util.buildResourcePack
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.Path
import kotlin.time.measureTime

abstract class WatchTask : DefaultTask() {
    init {
        group = "resource pack"
        description = "Watches for file changes and rebuilds the resource pack"
    }

    @TaskAction
    fun startWatching() {
        val extension = project.extensions.getByName("rpp") as RppExtension

        println("Built resource pack in ${measureTime { project.buildResourcePack(extension) }.inWholeMilliseconds}ms")
        println()
        try {
            DirectoryWatcher(Path(extension.sourceDirectory)) {
                println("File changed - rebuilding resource pack...")

                val elapsed = measureTime {
                    project.buildResourcePack(extension)
                }

                println("Rebuilt resource pack in ${elapsed.inWholeMilliseconds}ms")
                println()
            }.watch()
        } catch (_: InterruptedException) { } finally {
            println("Quit watching directory")
        }
    }
}