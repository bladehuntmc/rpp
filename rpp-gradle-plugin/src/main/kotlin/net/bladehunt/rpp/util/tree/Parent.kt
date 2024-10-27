package net.bladehunt.rpp.util.tree

import net.bladehunt.rpp.util.ignore.IgnoreFile
import net.bladehunt.rpp.util.tree.Node.Branch

typealias Visitor<T> = (Node<T>) -> Unit

interface Parent<T> : Node<T> {
    val children: Map<String, Node<T>>

    val keys: Collection<String>

    val values: Collection<Node<T>>

    var ignoreSpec: IgnoreFile?

    operator fun get(next: String): Node<T>?

    operator fun get(path: TreePath): Node<T>? {
        if (path.path.isEmpty()) return null

        if (path.path.size == 1) return get(path.path.first())

        var current: Parent<T> = this

        for (i in 0 until path.path.size - 1) {
            current = current[path.path[i]] as? Parent<T>? ?: return null
        }

        return current[path.path.last()]
    }

    fun createBranch(next: String, ignoreSpec: IgnoreFile?): Branch<T>

    fun getOrCreateBranch(path: TreePath): Parent<T> {
        require(path.path.isNotEmpty()) { "Path must not be empty" }

        if (path.path.size == 1) {
            val current = this[path.path.first()] ?: return this.createBranch(path.path.first(), null)

            return requireNotNull(current as? Branch<T>) { "$path (child of ${this.path}) was not of type Branch" }
        }

        var current = this

        for (part in path.path.indices) {
            current = current[path.path[part]]?.let { branch ->
                if (branch !is Branch) throw IllegalStateException("$part (child of ${current.path}) was not of type Branch")
                branch
            } ?: current.createBranch(path.path[part], null)
        }

        return current
    }

    fun createLeaf(next: String, data: T): Node.Leaf<T>

    operator fun contains(next: String): Boolean = get(next) != null

    operator fun contains(path: TreePath): Boolean = get(path) != null

    operator fun contains(data: T): Boolean = get(path) != null

    fun remove(next: String): Node<T>?

    fun remove(path: TreePath): Node<T>? {
        if (path.path.isEmpty()) return null

        if (path.path.size == 1) return remove(path.path.first())

        var parent: Parent<T> = this

        for (i in 0 until path.path.size - 1) {
            parent = parent[path.path[i]] as? Parent<T>? ?: return null
        }

        return parent.remove(path.path.last())
    }
}