package net.bladehunt.rpp.build

import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.processor.FileProcessor
import net.bladehunt.rpp.processor.process
import net.bladehunt.rpp.processor.shouldExecute
import net.bladehunt.rpp.util.FileLock
import net.bladehunt.rpp.util.tree.*
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging
import java.io.File

private val LOGGER = Logging.getLogger(ResourcePackProcessor::class.java)

class ResourcePackProcessor(
    val layout: Layout,
    private val fileProcessors: List<FileProcessor>
) {
    private val sourceTree: FileTree<FileData> = FileTree(layout.source)

    fun build() {
        layout.build.root.mkdirs()

        FileLock(layout.build.root.resolve("rpp.lock")).use { _ ->
            layout.build.output.mkdirs()

            val result = sourceTree.refresh({ _, _ -> false }) {
                FileData(it, ArrayList(1))
            }

            updateTrackedSource(result)

            BuildScope(this).use { scope ->
                try {
                    result.created.forEach {
                        if (it !is Node.Leaf) return@forEach

                        fileProcessors.forEach proc@{ processor ->
                            if (!processor.shouldExecute(scope, it.data)) return@proc

                            processor.process(scope, it.data)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun invalidateAll() {
        sourceTree.keys.forEach(sourceTree::remove)

        layout.build.output.deleteRecursively()
    }

    fun invalidate(path: TreePath) {
        LOGGER.debug("Invalidating {}", path)

        sourceTree.remove(path)?.traverse {
            if (it !is Node.Leaf) return@traverse

            it.data.outputs.forEach { output -> output.deleteRecursively() }
        }
    }

    fun clean() {
        layout.build.root.deleteRecursively()
    }

    private fun updateTrackedSource(result: DiffResult<FileData>) {
        result.deleted.forEach { node ->
            node.traverse {
                if (it !is Node.Leaf) return@traverse
                it.data.outputs.forEach { output -> output.deleteRecursively() }
            }
        }

        result.modified.forEach { node ->
            throw UnsupportedOperationException("Modification diffs are currently unsupported")
        }

        result.created.forEach { node ->
            val pathString = node.path.toString()
            val source = layout.source.resolve(pathString)
            val outputCopy = layout.build.output.resolve(pathString)

            when (node) {
                is Parent -> {
                    outputCopy.mkdir()
                }
                is Node.Leaf -> {
                    node.data.outputs.add(outputCopy)
                    source.copyTo(outputCopy)
                }
            }
        }
    }

    data class Layout(
        val source: File,
        val build: Build
    ) {
        data class Build(
            val root: File,
            val output: File = root.resolve("output"),
            val generated: Generated = Generated(root.resolve("generated"))
        ) {
            data class Generated(
                val root: File,
                val java: File = root.resolve("java"),
                val kotlin: File = root.resolve("kotlin")
            )
        }

        companion object {
            internal fun fromProject(
                project: Project,
                extension: RppExtension = (project.extensions.getByName("rpp") as RppExtension)
            ): Layout =
                Layout(
                    project.layout.projectDirectory.asFile.resolve(
                        extension.sourceDirectory
                    ),
                    Build(project.layout.buildDirectory.get().asFile.resolve("rpp"))
                )
        }
    }

    companion object {
        internal fun fromTask(task: Task): ResourcePackProcessor {
            val extension = task.project.extensions.getByName("rpp") as RppExtension
            return ResourcePackProcessor(Layout.fromProject(task.project, extension), extension.fileProcessors)
        }
    }
}