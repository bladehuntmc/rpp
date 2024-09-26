package net.bladehunt.rpp.util

import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent

class DirectoryWatcher(
    path: Path,
    private val onEvent: (WatchEvent<*>) -> Unit
) {
    private val watchService = path.fileSystem.newWatchService().also {
        path.register(it, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
    }

    fun watch() {
        while (true) {
            val key = watchService.take()

            for (event in key.pollEvents()) {
                onEvent(event)
            }

            if (!key.reset()) {
                break
            }
        }
    }
}