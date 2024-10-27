package net.bladehunt.rpp.util

import org.gradle.api.logging.Logging
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.StandardWatchEventKinds
import kotlin.io.path.exists

private val LOGGER = Logging.getLogger(FileLock::class.java)

private fun acquireFileLock(file: Path): FileChannel {
    if (file.exists()) {
        LOGGER.lifecycle("Awaiting file lock...")

        val service = FileSystems.getDefault().newWatchService()
        file.parent.register(
            service,
            arrayOf(StandardWatchEventKinds.ENTRY_DELETE)
        )

        service.use {
            while (true) {
                val key = it.take()
                for (event in key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE &&
                        (event.context() as Path) == file) {
                        return@use
                    }
                }
                if (!key.reset()) break
            }
        }
    }

    return FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE).apply {
        try {
            tryLock() ?: throw IllegalStateException("Unable to acquire lock")
            LOGGER.lifecycle("Acquired file lock")
        } catch (e: Exception) {
            close()
            throw e
        }
    }
}

class FileLock(private val file: File) : AutoCloseable {
    private val channel = acquireFileLock(file.toPath())

    override fun close() {
        try {
            channel.close()
        } finally {
            try {
                file.delete()
            } catch (e: Exception) {
                LOGGER.error("{} while deleting lock file: {}", e::class.simpleName, e.message)
            }
        }
    }
}