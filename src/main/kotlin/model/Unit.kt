package model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.javaparser.Position
import com.github.javaparser.Range
import com.github.javaparser.ast.Node
import utility.*


data class Unit(
    @JsonIgnore val node: Node? = null,
    @JsonIgnore val nodeSequence: List<Node>? = null,
    val content: String,
    val range: Range,
    val identifier: String,
    val hash: Int,
    val mass: Int,
    var id: Int? = null
) {
    init {
        id = hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Unit

        if (content != other.content) return false
        if (range != other.range) return false
        if (identifier != other.identifier) return false

        return true
    }

    override fun hashCode(): Int {
        var result: Int = content.hashCode()
        result = 31 * result + range.hashCode()
        result = 31 * result + identifier.hashCode()
        return result
    }

    override fun toString(): String {
        return "Unit(id='$id', content='$content', range=$range, identifier=$identifier)"
    }

    fun contains(clone: Unit): Boolean = when {
        this == clone      -> false
        clone.node != null -> containsNode(clone.node) // Check if current Unit contains single node
        else               -> containsNodeSequence(clone.nodeSequence!!) // Check if current Unit contains sequence node
    }

    private fun containsNodeSequence(findNodeSequence: List<Node>): Boolean {
        return if (node != null) { // Current unit is a single node
            findNodeSequence.all { node.isAncestorOf(it) }
        } else { // Current unit is a node sequence
            findNodeSequence.all { findNode -> nodeSequence!!.any { it == findNode || it.isAncestorOf(findNode) } }
        }
    }

    private fun containsNode(findNode: Node): Boolean {
        return if (node != null) { // Current unit is a single node
            node == findNode || node.isAncestorOf(findNode)
        } else { // Current unit is a node sequence
            nodeSequence!!.any { it == findNode || it.isAncestorOf(findNode) }
        }
    }

    companion object {
        private val DEFAULT_CLONETYPE = CloneType.ONE

        fun fromNode(node: Node, basePath: String, cloneType: CloneType = DEFAULT_CLONETYPE): Unit {
            return Unit(
                node = node,
                content = node.tokenRange.get().toString().filterOutComments(),
                range = node.range.get(),
                identifier = node.retrieveLocation().convertToPackageIdentifier(basePath),
                hash = node.leniantHashCode(cloneType),
                mass = node.calculateMass()
            )
        }

        fun fromNodeSequence(nodeSequence: List<Node>, basePath: String, cloneType: CloneType = DEFAULT_CLONETYPE): Unit {
            return Unit(
                nodeSequence = nodeSequence,
                content = calculateNodeSequenceContent(nodeSequence),
                range = calculateNodeSequenceRange(nodeSequence),
                identifier = nodeSequence.first().retrieveLocation().convertToPackageIdentifier(basePath),
                hash = nodeSequence.map { it.leniantHashCode(cloneType) }.hashCode(),
                mass = nodeSequence.sumBy { it.calculateMass() } + nodeSequence.size
            )
        }

        private fun calculateNodeSequenceContent(nodeSequence: List<Node>): String {
            var result: String = nodeSequence[0].tokenRange.get().toString()
            for(i: Int in (1 until nodeSequence.size)) {
                result += if (nodeSequence[i - 1].range.get().end.line < nodeSequence[i].range.get().begin.line) "\n" else  " "
                result += nodeSequence[i].tokenRange.get().toString()
            }

            return result
        }

        private fun calculateNodeSequenceRange(nodeSequence: List<Node>): Range { // TODO: Add lineCount
            val initialPosition = nodeSequence[0].range.get().begin
            val finalPosition = nodeSequence[nodeSequence.size - 1].range.get().end
            return Range(Position.pos(initialPosition.line, initialPosition.column), Position.pos(finalPosition.line, finalPosition.column))
        }
    }
}
