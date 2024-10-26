package net.bladehunt.rpp.task

import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.sse.SseClient
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.build.ResourcePackProcessor
import net.bladehunt.rpp.util.DirectoryWatcher
import net.bladehunt.rpp.util.tree.TreePath
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

abstract class ServerTask : DefaultTask() {
    init {
        group = "resource pack"
        description = "Starts an HTTP server, watching for file changes"
    }

    @TaskAction
    fun startServer() {
        val extension = project.extensions.getByName("rpp") as RppExtension

        val processor = ResourcePackProcessor.fromTask(this)

        processor.cleanOutputs()

        logger.lifecycle("Built resource pack in ${ measureNanoTime { processor.build() } / 1_000_000 }ms")

        val archiveId = extension.server.archiveId

        val clients = ConcurrentLinkedQueue<SseClient>()

        val app = Javalin.create()
            .sse("/sse") { client ->
                client.keepAlive()
                clients.add(client)
                val port = client.ctx().port()
                logger.lifecycle("Client opened at port $port")

                val archive = processor.getArchive(archiveId)
                if (archive != null) client.sendEvent("update", archive.sha1Hash)
                else logger.warn("Archive $archiveId was not found")

                client.onClose {
                    logger.lifecycle("Client at port $port closed")
                    clients.remove(client)
                }
            }
            .get("/pack") { ctx: Context ->
                ctx.contentType(ContentType.APPLICATION_ZIP)

                val archive = processor.getArchive(archiveId)
                if (archive != null) archive.file.inputStream().use { input ->
                    ctx.result(input)
                }
                else logger.warn("Archive $archiveId was not found")
            }
            .get("/hash") { ctx: Context ->
                ctx.contentType(ContentType.TEXT_PLAIN)

                val archive = processor.getArchive(extension.server.archiveId)

                if (archive != null) ctx.result(archive.sha1Hash)
                else logger.warn("Archive $archiveId was not found")
            }
            .start(extension.server.address.hostString, extension.server.address.port)

        logger.lifecycle("Resource pack server started at ${extension.server.address}")

        val future = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            { clients.forEach { it.sendEvent("heartbeat") } },
            0,
            5,
            TimeUnit.SECONDS
        )

        try {
            DirectoryWatcher(
                processor.layout.source.toPath(),
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
                                processor.invalidate(TreePath(processor.layout.source, actualPath.toFile()))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    polled = it.poll()
                }

                logger.lifecycle("Built resource pack in ${ measureNanoTime { processor.build() } / 1_000_000.0 }ms")

                val archiveResult = processor.getArchive(archiveId)

                if (archiveResult == null) {
                    logger.warn("Archive $archiveId was not found")
                    return@DirectoryWatcher
                }
                clients.forEach { client -> client.sendEvent("update", archiveResult.sha1Hash) }
            }.watch()
        } catch (_: InterruptedException) { } finally {
            app.stop()
            future.cancel(true)
            logger.lifecycle("Resource pack server stopped")
        }
    }
}