package net.bladehunt.rpp.task

import net.bladehunt.rpp.util.DirectoryWatcher
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.output.BuildContext
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

        val context = BuildContext.fromTask(this)

        context.buildTimed()
        try {
            DirectoryWatcher(Path(extension.sourceDirectory)) {
                logger.lifecycle("File changed - rebuilding resource pack...")

                context.buildTimed()
            }.watch()
        } catch (_: InterruptedException) { } finally {
            logger.lifecycle("Stopped watching directory")
        }
    }
}