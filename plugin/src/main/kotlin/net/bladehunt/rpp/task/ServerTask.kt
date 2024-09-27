package net.bladehunt.rpp.task

import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.sse.SseClient
import net.bladehunt.rpp.util.DirectoryWatcher
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.util.buildResourcePack
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.time.measureTime

abstract class ServerTask : DefaultTask() {
    init {
        group = "resource pack"
        description = "Starts an HTTP server, watching for file changes"
    }

    @TaskAction
    fun startServer() {
        val extension = project.extensions.getByName("rpp") as RppExtension

        println("Building resource pack...")
        project.buildResourcePack(extension)

        val outputName = extension.outputName ?: "resource_pack_${project.version}"
        val buildDir = project.layout.buildDirectory.get().dir("rpp")
        val hash = buildDir.file("$outputName.sha1").asFile
        val zip = buildDir.file("$outputName.zip").asFile

        val clients = ConcurrentLinkedQueue<SseClient>()

        val app = Javalin.create()
            .sse("/sse") { client ->
                client.keepAlive()
                clients.add(client)
                println("Client opened ${client.ctx().port()}")
                client.sendEvent("update", hash.inputStream().readAllBytes())
                client.onClose {
                    println("Client closed")
                    clients.remove(client)
                }
            }
            .get("/pack") { ctx: Context ->
                ctx.contentType(ContentType.APPLICATION_ZIP)
                ctx.result(zip.inputStream().readAllBytes())
            }
            .get("/hash") { ctx: Context ->
                ctx.contentType(ContentType.TEXT_PLAIN)
                ctx.result(hash.inputStream().readAllBytes())
            }
            .start(extension.server.address.hostString, extension.server.address.port)

        println("Resource pack server started at ${extension.server.address}")

        val future = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            { clients.forEach { it.sendEvent("heartbeat") } },
            0,
            5,
            TimeUnit.SECONDS
        )

        try {
            DirectoryWatcher(Path(extension.sourceDirectory)) {
                println("File changed - rebuilding resource pack...")

                val elapsed = measureTime {
                    project.buildResourcePack(extension)
                }

                println("Rebuilt resource pack in ${elapsed.inWholeMilliseconds}ms")

                val zipHash = hash.inputStream().readAllBytes().decodeToString()
                clients.forEach { it.sendEvent("update", zipHash) }
                println()
            }.watch()
        } catch (_: InterruptedException) { } finally {
            app.stop()
            future.cancel(true)
            println("Resource pack server stopped")
        }
    }
}