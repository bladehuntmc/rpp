package net.bladehunt.rpp.build

class BuildScope(
    val rpp: ResourcePackProcessor
) : AutoCloseable {
    private val resources: MutableMap<String, AutoCloseable> = linkedMapOf()

    fun <T : AutoCloseable> getAutoCloseable(id: String): T? = resources[id] as T?

    fun <T : AutoCloseable> install(id: String, autoCloseable: T): T {
        resources[id] = autoCloseable

        return autoCloseable
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