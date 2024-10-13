package net.bladehunt.rpp.task

import net.bladehunt.rpp.util.DirectoryWatcher
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.output.buildResourcePack
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

        println("Built resource pack in ${measureTime {
            buildResourcePack(
                logger,
                project.layout.projectDirectory.asFile.resolve(extension.sourceDirectory),
                project.layout.buildDirectory.asFile.get().resolve("rpp"),
                project.version.toString(),
                extension
            )
        }.inWholeMilliseconds}ms\n")
        try {
            DirectoryWatcher(Path(extension.sourceDirectory)) {
                println("File changed - rebuilding resource pack...")

                val elapsed = measureTime {
                    buildResourcePack(
                        logger,
                        project.layout.projectDirectory.asFile.resolve(extension.sourceDirectory),
                        project.layout.buildDirectory.asFile.get().resolve("rpp"),
                        project.version.toString(),
                        extension
                    )
                }

                println("Rebuilt resource pack in ${elapsed.inWholeMilliseconds}ms\n")
            }.watch()
        } catch (_: InterruptedException) { } finally {
            println("Stopped watching directory")
        }
    }
}