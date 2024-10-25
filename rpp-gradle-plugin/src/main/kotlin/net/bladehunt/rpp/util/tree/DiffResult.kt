package net.bladehunt.rpp.util.tree

data class DiffResult<T>(
    val created: LinkedHashSet<Node<T>> = linkedSetOf(),
    val modified: LinkedHashSet<Node<T>> = linkedSetOf(),
    val deleted: LinkedHashSet<Node<T>> = linkedSetOf(),
)
