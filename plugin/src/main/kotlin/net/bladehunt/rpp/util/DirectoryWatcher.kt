package net.bladehunt.rpp.util

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.isDirectory

private val LOGGER = LoggerFactory.getLogger(DirectoryWatcher::class.java)

// Based on https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java
class DirectoryWatcher(
    path: Path,
    private val onChange: () -> Unit
) {
    private val keys: MutableMap<WatchKey, Path> = hashMapOf()

    private val watchService = path.fileSystem.newWatchService()

    private var isDebounceActive = false

    private val executor = Executors.newSingleThreadScheduledExecutor()

    init {
        registerAll(path)
    }

    private fun registerAll(start: Path) {
        Files.walkFileTree(start, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
                keys[key] = dir
                return FileVisitResult.CONTINUE
            }
        })
    }

    fun watch() {
        while (true) {
            val key = watchService.take()

            val dir = keys[key]

            if (dir == null) {
                LOGGER.error("WatchKey not recognized")
                continue
            }

            val events = key.pollEvents()
            for (event in events) {
                val kind = event.kind()

                if (kind === OVERFLOW) {
                    continue
                }

                event as WatchEvent<Path>

                val name = event.context()
                val child = dir.resolve(name)

                if (kind === ENTRY_CREATE) {
                    try {
                        if (child.isDirectory(LinkOption.NOFOLLOW_LINKS)) registerAll(child)
                    } catch (_: IOException) { }
                }
            }

            if (!isDebounceActive) {
                isDebounceActive = true
                executor.schedule({
                    onChange()
                    isDebounceActive = false
                }, 50, TimeUnit.MILLISECONDS)
            }

            if (!key.reset()) {
                keys.remove(key)

                if (keys.isEmpty()) break
            }
        }
    }
}