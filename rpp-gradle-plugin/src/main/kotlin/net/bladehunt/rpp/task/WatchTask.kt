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

        val sourceDir = project.layout.projectDirectory.asFile.resolve(extension.sourceDirectory)
        val outputDir = project.layout.buildDirectory.asFile.get().resolve("rpp")
        val version = project.version.toString()

        logger.lifecycle("Built resource pack in ${measureTime {
            buildResourcePack(
                logger,
                sourceDir,
                project.layout.buildDirectory.asFile.get().resolve("rpp"),
                project.version.toString(),
                extension
            )
        }.inWholeMilliseconds}ms\n")
        try {
            DirectoryWatcher(Path(extension.sourceDirectory)) {
                logger.lifecycle("File changed - rebuilding resource pack...")

                val elapsed = measureTime {
                    buildResourcePack(
                        logger,
                        sourceDir,
                        outputDir,
                        version,
                        extension
                    )
                }

                logger.lifecycle("Rebuilt resource pack in ${elapsed.inWholeMilliseconds}ms\n")
            }.watch()
        } catch (_: InterruptedException) { } finally {
            logger.lifecycle("Stopped watching directory")
        }
    }
}