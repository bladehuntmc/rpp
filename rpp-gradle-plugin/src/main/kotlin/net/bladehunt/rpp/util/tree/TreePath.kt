package net.bladehunt.rpp.util.tree

import java.io.File

@JvmInline
value class TreePath(val path: List<String>) {
    constructor(path: String) : this(path.split(File.separator))

    constructor(path: File) : this(path.path.split(File.separator))

    constructor(root: File, path: File) : this(path.toRelativeString(root))

    operator fun plus(path: String) = TreePath(this.path + path.split(File.separator))

    operator fun plus(other: TreePath) = TreePath(path + other.path)

    fun toFile(root: File) = root.resolve(toString())

    override fun toString(): String = path.joinToString(File.separator)
}