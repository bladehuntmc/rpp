package net.bladehunt.rpp.task

import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.sse.SseClient
import net.bladehunt.rpp.util.DirectoryWatcher
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.output.buildResourcePack
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.measureTime

abstract class ServerTask : DefaultTask() {
    init {
        group = "resource pack"
        description = "Starts an HTTP server, watching for file changes"
    }

    @TaskAction
    fun startServer() {
        val extension = project.extensions.getByName("rpp") as RppExtension

        logger.lifecycle("Building resource pack...")

        val sourceDir = project.layout.projectDirectory.asFile.resolve(extension.sourceDirectory)
        val outputDir = project.layout.buildDirectory.asFile.get().resolve("rpp")
        val version = project.version.toString()

        buildResourcePack(
            logger,
            sourceDir,
            outputDir,
            version,
            extension
        )

        val outputName = extension.outputName ?: "resource_pack_${project.version}"
        val buildDir = project.layout.buildDirectory.get().dir("rpp")
        val hash = buildDir.file("$outputName.sha1").asFile
        val zip = buildDir.file("$outputName.zip").asFile

        val clients = ConcurrentLinkedQueue<SseClient>()

        val app = Javalin.create()
            .sse("/sse") { client ->
                client.keepAlive()
                clients.add(client)
                val port = client.ctx().port()
                logger.lifecycle("Client opened at port $port")
                if (hash.exists()) client.sendEvent("update", hash.inputStream().readAllBytes().decodeToString())
                client.onClose {
                    logger.lifecycle("Client at port $port closed")
                    clients.remove(client)
                }
            }
            .get("/pack") { ctx: Context ->
                ctx.contentType(ContentType.APPLICATION_ZIP)
                if (zip.exists()) ctx.result(zip.inputStream().readAllBytes())
            }
            .get("/hash") { ctx: Context ->
                ctx.contentType(ContentType.TEXT_PLAIN)
                if (hash.exists()) ctx.result(hash.inputStream().readAllBytes())
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
            DirectoryWatcher(sourceDir.toPath()) {
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

                val zipHash = if (hash.exists()) hash.inputStream().readAllBytes().decodeToString()
                    else return@DirectoryWatcher
                clients.forEach { it.sendEvent("update", zipHash) }
            }.watch()
        } catch (_: InterruptedException) { } finally {
            app.stop()
            future.cancel(true)
            logger.lifecycle("Resource pack server stopped")
        }
    }
}