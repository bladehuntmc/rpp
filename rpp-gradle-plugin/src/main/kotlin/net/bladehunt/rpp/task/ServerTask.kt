package net.bladehunt.rpp.task

import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.sse.SseClient
import net.bladehunt.rpp.util.DirectoryWatcher
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.output.BuildContext
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class ServerTask : DefaultTask() {
    init {
        group = "resource pack"
        description = "Starts an HTTP server, watching for file changes"
    }

    @TaskAction
    fun startServer() {
        val extension = project.extensions.getByName("rpp") as RppExtension

        val context = BuildContext.fromTask(this)
        context.buildTimed()

        val clients = ConcurrentLinkedQueue<SseClient>()

        val app = Javalin.create()
            .sse("/sse") { client ->
                client.keepAlive()
                clients.add(client)
                val port = client.ctx().port()
                logger.lifecycle("Client opened at port $port")

                val archive = context.generatedArchives[extension.server.archiveId]
                if (archive != null) client.sendEvent("update", archive.sha1Hash)

                client.onClose {
                    logger.lifecycle("Client at port $port closed")
                    clients.remove(client)
                }
            }
            .get("/pack") { ctx: Context ->
                ctx.contentType(ContentType.APPLICATION_ZIP)

                val archive = context.generatedArchives[extension.server.archiveId]
                archive?.file?.inputStream()?.use { input ->
                    ctx.result(input)
                }
            }
            .get("/hash") { ctx: Context ->
                ctx.contentType(ContentType.TEXT_PLAIN)

                val archive = context.generatedArchives[extension.server.archiveId]
                archive?.sha1Hash?.let { ctx.result(it) }
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
            DirectoryWatcher(context.sourcesDirectory.toPath()) {
                logger.lifecycle("File changed - rebuilding resource pack...")

                val startArchive = context.generatedArchives[extension.server.archiveId]

                if (startArchive == null) {
                    logger.warn("Failed to find an archive with ID ${extension.server.archiveId} before building")
                }

                context.buildTimed()

                val endArchive = context.generatedArchives[extension.server.archiveId]

                if (endArchive == null) {
                    logger.error("Failed to find an archive with ID ${extension.server.archiveId} after building")
                } else {
                    clients.forEach { it.sendEvent("update", endArchive.sha1Hash) }
                }
            }.watch()
        } catch (_: InterruptedException) { } finally {
            app.stop()
            future.cancel(true)
            logger.lifecycle("Resource pack server stopped")
        }
    }
}