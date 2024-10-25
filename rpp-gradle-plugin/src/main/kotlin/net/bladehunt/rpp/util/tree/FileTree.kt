package net.bladehunt.rpp.util.tree

import net.bladehunt.rpp.util.ignore.IgnoreFile
import net.bladehunt.rpp.util.sha1
import org.gradle.internal.impldep.org.apache.http.impl.cookie.IgnoreSpec
import java.io.File

private const val IGNORE_NAME = ".rppignore"

class FileTree<T> private constructor(
    val rootFile: File,
    private val root: Parent<T>
) : Parent<T> by root {
    constructor(rootFile: File) : this(rootFile, Node.Root(IgnoreFile.fromFile(rootFile.resolve(IGNORE_NAME))))

    private fun diffCreateUnsafe(
        parent: Parent<T>, file: File, result: DiffResult<T>,
        modified: (file: File, Node.Leaf<T>) -> Boolean,
        provider: (file: File) -> T
    ) {
        if (!file.isDirectory) return

        val files = file.listFiles() ?: return

        for (child in files) {
            if (
                run {
                    val path = child.toRelativeString(rootFile)

                    var currentParent: Parent<T>? = parent
                    var ignores = false
                    while (currentParent != null) {
                        if (!ignores) ignores = currentParent.ignoreSpec?.ignores(path) ?: false
                        if (currentParent.ignoreSpec?.negates(path) == true) return@run true
                        currentParent = currentParent.parent
                    }
                    ignores
                }
            ) continue

            val name = child.name
            when {
                child.isFile -> {
                    val leaf = parent.createLeaf(name, provider(child))
                    result.created.add(leaf)
                }

                child.isDirectory -> {
                    val branch = parent.createBranch(name, IgnoreFile.fromFile(child.resolve(IGNORE_NAME)))
                    result.created.add(branch)
                    diffCreateUnsafe(branch, child, result, modified, provider)
                }
            }
        }
    }

    private fun diff(
        parent: Parent<T>, file: File, result: DiffResult<T>,
        modified: (file: File, Node.Leaf<T>) -> Boolean,
        provider: (file: File) -> T
    ) {
        if (!file.isDirectory) return
        val toRemove = parent.values.toMutableSet()

        val files = file.listFiles() ?: return
        for (child in files) {
            val name = child.name

            if (
                run {
                    val path = child.relativeTo(rootFile).path
                    var currentParent: Parent<T>? = parent
                    var ignores = false
                    while (currentParent != null) {
                        if (!ignores) ignores = currentParent.ignoreSpec?.ignores(path) ?: false
                        if (currentParent.ignoreSpec?.negates(path) == true) return@run true
                        currentParent = currentParent.parent
                    }
                    ignores
                }
            ) {
                parent.remove(name)?.let { current ->
                    result.deleted.add(current)
                }

                continue
            }

            when {
                child.isFile -> {
                    when (val node = parent[name]) {
                        // exists as a parent, must be modified
                        is Parent -> {
                            toRemove.remove(node)
                            parent.remove(name)
                            result.deleted.add(node)
                            val leaf = parent.createLeaf(name, provider(child))
                            result.created.add(leaf)
                        }
                        is Node.Leaf -> {
                            toRemove.remove(node)
                            if (!modified(file, node)) continue
                            result.modified.add(node)
                        }
                        null -> {
                            if (child.name == IGNORE_NAME) continue

                            val leaf = parent.createLeaf(name, provider(child))

                            result.created.add(leaf)
                        }
                    }
                }

                child.isDirectory -> {
                    when (val node = parent[name]) {
                        // already exists as a directory (branch), diff
                        is Parent -> {
                            toRemove.remove(node)
                            val ignoreFile = child.resolve(IGNORE_NAME)
                            val newHash = ignoreFile.let { if (it.exists()) it.sha1() else null }
                            if (newHash != node.ignoreSpec?.sha1Hash) node.ignoreSpec = IgnoreFile.fromFile(ignoreFile)

                            diff(node, child, result, modified, provider)
                        }
                        // already exists as a file, delete it and create as a branch
                        is Node.Leaf -> {
                            toRemove.remove(node)
                            parent.remove(name)
                            result.deleted.add(node)
                            val branch = parent.createBranch(name, IgnoreFile.fromFile(child.resolve(IGNORE_NAME)))
                            result.created.add(branch)
                        }
                        // new branch, use faster algorithm
                        null -> {
                            val branch = parent.createBranch(name, IgnoreFile.fromFile(child.resolve(IGNORE_NAME)))
                            result.created.add(branch)
                            diffCreateUnsafe(branch, child, result, modified, provider)
                        }
                    }
                }
            }
        }
        toRemove.forEach {
            parent.remove(it.name)
            result.deleted.add(it)
        }
    }

    fun refresh(
        modified: (file: File, Node.Leaf<T>) -> Boolean,
        provider: (file: File) -> T
    ): DiffResult<T> {
        require(rootFile.isDirectory) { "File must be a directory" }

        val result = DiffResult<T>()

        diff(this, rootFile, result, modified, provider)

        return result
    }
}