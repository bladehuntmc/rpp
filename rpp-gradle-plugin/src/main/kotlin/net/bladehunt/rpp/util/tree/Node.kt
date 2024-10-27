package net.bladehunt.rpp.util.tree

import net.bladehunt.rpp.util.ignore.IgnoreFile
import java.io.File

interface Node<T> {
    val parent: Parent<T>?

    val name: String

    val path: TreePath

    fun toFile(root: File?) = root?.resolve(path.toString()) ?: File(path.toString())

    fun traverse(visitor: Visitor<T>)

    class Root<T>(override var ignoreSpec: IgnoreFile?) : Parent<T> {
        private val _children: LinkedHashMap<String, Node<T>> = linkedMapOf()

        override val children: Map<String, Node<T>>
            get() = _children.toMap()

        override val keys: Collection<String>
            get() = _children.keys

        override val values: Collection<Node<T>>
            get() = _children.values

        override val parent: Parent<T>? = null

        override val name: String = ""

        override val path: TreePath = TreePath(emptyList())

        override fun traverse(visitor: Visitor<T>) {
            visitor(this)

            values.forEach {
                it.traverse(visitor)
            }
        }

        override fun get(next: String): Node<T>?  = _children[next]

        override fun remove(next: String): Node<T>? = _children.remove(next)

        override fun createBranch(next: String, ignoreSpec: IgnoreFile?): Branch<T> {
            require(!_children.contains(next)) { "Branch already exists" }

            val branch = Branch(this, next, ignoreSpec)

            _children[next] = branch

            return branch
        }

        override fun createLeaf(next: String, data: T): Leaf<T> {
            require(!_children.contains(next)) { "Leaf already exists" }

            val leaf = Leaf(this, next, data)

            _children[next] = leaf

            return leaf
        }
    }

    data class Branch<T>(
        override val parent: Parent<T>,
        override val name: String,
        override var ignoreSpec: IgnoreFile?
    ) : Parent<T> {
        private val _children: LinkedHashMap<String, Node<T>> = linkedMapOf()

        override val path: TreePath = parent.path.plus(name)

        override val children: Map<String, Node<T>>
            get() = _children.toMap()

        override val keys: Collection<String>
            get() = _children.keys

        override val values: Collection<Node<T>>
            get() = children.values

        override fun traverse(visitor: Visitor<T>) {
            visitor(this)

            values.forEach {
                it.traverse(visitor)
            }
        }

        override fun get(next: String): Node<T>?  = _children[next]

        override fun remove(next: String): Node<T>? = _children.remove(next)

        override fun createBranch(next: String, ignoreSpec: IgnoreFile?): Branch<T> {
            require(!_children.contains(next)) { "Branch already exists" }

            val branch = Branch(this, next, ignoreSpec)

            _children[next] = branch

            return branch
        }

        override fun createLeaf(next: String, data: T): Leaf<T> {
            require(!_children.contains(next)) { "Leaf already exists" }

            val leaf = Leaf(this, next, data)

            _children[next] = leaf

            return leaf
        }
    }

    data class Leaf<T>(override val parent: Parent<T>, override val name: String, var data: T) : Node<T> {
        override val path: TreePath = parent.path.plus(name)

        override fun traverse(visitor: Visitor<T>) = visitor(this)
    }
}