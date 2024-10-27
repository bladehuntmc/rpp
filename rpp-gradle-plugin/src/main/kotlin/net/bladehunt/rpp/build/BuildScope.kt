package net.bladehunt.rpp.build

import net.bladehunt.rpp.processor.Processor
import net.bladehunt.rpp.util.tree.DiffResult

class BuildScope(
    val rpp: ResourcePackProcessor,
    val diff: DiffResult<FileData>,
    private val _archives: MutableMap<String, Archive> = linkedMapOf()
) : AutoCloseable {
    val archives get() = _archives.values

    private val resources: MutableMap<String, AutoCloseable> = linkedMapOf()

    fun <T : AutoCloseable> getAutoCloseable(id: String): T? = resources[id] as T?

    fun <T : AutoCloseable> install(id: String, autoCloseable: T): T {
        resources[id] = autoCloseable

        return autoCloseable
    }

    @JvmName("_getOrCreateContext")
    @JvmSynthetic
    fun <T> Processor<T>.getOrCreateContext() =
        rpp.getContext(this) ?: rpp.createContext(this, createContext(rpp))

    fun <T> getOrCreateContext(processor: Processor<T>) =
        rpp.getContext(processor) ?: rpp.createContext(processor, processor.createContext(rpp))

    fun deleteArchive(id: String): Archive? = _archives.remove(id)?.also {
        it.file.delete()
    }

    fun getArchive(id: String) = _archives[id]

    fun addArchive(archive: Archive) {
        _archives[archive.id] = archive
    }

    inline fun <T : AutoCloseable> install(id: String, provider: () -> T): T =
        getAutoCloseable(id) ?: install(id, provider())

    override fun close() {
        resources.values.forEach {
            try {
                it.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}