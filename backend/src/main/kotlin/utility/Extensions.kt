package utility

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javaparser.Position
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import model.CloneType
import java.io.File
import java.nio.file.Path
import java.util.*


fun Any.toJson(): String = ObjectMapper().writeValueAsString(this)


suspend fun <A, B> Iterable<A>.mapConcurrently(transform: suspend (A) -> B): List<B> = coroutineScope {
    map { async { transform(it) } }.awaitAll()
}


fun <T> Collection<T>.cartesianProduct(): List<Pair<T, T>> {
    val pairs: ArrayList<Pair<T, T>> = arrayListOf()
    for (item1 in this) {
        for (item2 in this) {
            if (item1 == item2) continue
            if (pairs.contains(Pair(item1, item2)) || pairs.contains(Pair(item2, item1))) continue
            pairs.add(Pair(item1, item2))
        }
    }
    return pairs.toList()
}


fun String.isBlankLine(): Boolean = this.isBlank()

fun String.isSingleLineJavaCommentLine(): Boolean = this.trim().startsWith(SinglelineCommentToken)

fun String.filterOutBlankLinesAndJavaComments(): String = this.replace("\r", "").replace(MultilineCommentRegex, "").split("\n").filter { !it.isBlankLine() }.filter { !it.isSingleLineJavaCommentLine() }.joinToString("\n")

fun String.countJavaSloc(): Int = this.filterOutBlankLinesAndJavaComments().split("\n").count()


fun <T : Any> Optional<T>.toNullable(): T? = this.orElse(null)


operator fun File.plus(other: File): File = File(this, other.absolutePath.removePrefix(this.absolutePath))


fun Path.convertToPackageIdentifier(basePath: String): String {
    return this.toFile().absolutePath.substringAfterLast(basePath).removePrefix("/").replace('/', '.').removeSuffix(".$JavaFileExtension")
}


inline fun <T, R> Pair<T, T>.map(transform: (T) -> R): Pair<R, R> {
    val pairList: List<R> = this.toList().map { transform(it) }
    return Pair(pairList.first(), pairList.last())
}


fun Node.retrieveLocation(): Path {
    return if (this.parentNode.isEmpty) {
        (this as CompilationUnit).storage.get().path
    } else {
        this.parentNode.get().retrieveLocation()
    }
}

fun Node.getAllParentNodes(): Set<Node> {
    if (this.parentNode.isEmpty) return emptySet()

    val parentNodes: Set<Node> = mutableSetOf()
    val parentNodeQueue: Queue<Node> = ArrayDeque()
    parentNodeQueue.add(this.parentNode.get())
    while (parentNodeQueue.isNotEmpty()) {
        val parentNode = parentNodeQueue.remove()
        parentNodes.plus(parentNode)
        if (parentNode.parentNode.isPresent) parentNodeQueue.add(parentNode.parentNode.get())
    }
    return parentNodes
}

fun Node.getAllLineSiblings(): List<Node> {
    return if (this.parentNode.isEmpty) {
        listOf()
    } else {
        this.parentNode.get().childNodes.sortedWith(Comparator { s1, s2 ->
            val beginS1: Position = s1.range.get().begin
            val beginS2: Position = s2.range.get().begin
            if (beginS1.line == beginS2.line) {
                return@Comparator beginS1.column.compareTo(beginS2.column)
            } else {
                return@Comparator beginS1.line.compareTo(beginS2.line)
            }
        })
    }
}

fun Node.lenientHashCode(cloneType: CloneType = CloneType.ONE): Int {
    LenientHashCodeVisitor.setCloneType(cloneType)
    return LenientHashCodeVisitor.hashCode(this)
}

fun Node.calculateMass(): Int = 1 + this.childNodes.sumBy { it.calculateMass() }
