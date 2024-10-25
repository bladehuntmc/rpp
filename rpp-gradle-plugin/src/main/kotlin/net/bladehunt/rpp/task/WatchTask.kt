package net.bladehunt.rpp.task

import net.bladehunt.rpp.util.DirectoryWatcher
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.build.ResourcePackProcessor
import net.bladehunt.rpp.util.tree.TreePath
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

abstract class WatchTask : DefaultTask() {
    init {
        group = "resource pack"
        description = "Watches for file changes and rebuilds the resource pack"
    }

    @TaskAction
    fun startWatching() {
        val extension = project.extensions.getByName("rpp") as RppExtension

        val processor = ResourcePackProcessor.fromTask(this)

        processor.layout.build.output.deleteRecursively()

        logger.lifecycle("Built resource pack in ${ measureNanoTime { processor.build() } / 1_000_000 }ms")
        try {
            val source = processor.layout.source

            DirectoryWatcher(
                source.toPath(),
                { processor.invalidateAll(); it.clear() }
            ) {
                logger.lifecycle("File changed - rebuilding resource pack...")

                var polled = it.poll()
                while (polled != null) {
                    val (event, actualPath) = polled
                    when (event.kind()) {
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            try {
                                processor.invalidate(TreePath(source, actualPath.toFile()))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    polled = it.poll()
                }

                logger.lifecycle("Built resource pack in ${ measureNanoTime { processor.build() } / 1_000_000 }ms")
            }.watch()
        } catch (_: InterruptedException) { } finally {
            logger.lifecycle("Stopped watching directory")
        }
    }
}