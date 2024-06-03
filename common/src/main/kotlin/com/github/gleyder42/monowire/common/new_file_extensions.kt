package com.github.gleyder42.monowire.common

import arrow.core.*
import arrow.core.raise.either
import arrow.core.raise.ensure
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*

/**
 * Infix function for [Path.resolve]
 */
infix fun Path.`|`(other: Path): Path = this.resolve(other)

/**
 * Infix function for [Path.resolve]
 */
infix fun Path.`|`(other: String): Path = this.resolve(other)

data class ListPathError(val message: String)


/**
 * List all paths recursively inside the directory.
 *
 * Returns [Either.Left] if the path does not exist, otherwise a list of all paths.
 */
fun Path.listPathsRecursively(filter: (Path) -> Boolean): Either<ListPathError, List<Path>> = either {
    val path = this@listPathsRecursively

    ensure(path.exists()) {
        ListPathError("Cannot list paths because $path does not exist")
    }

    Files.walk(path).use { it.skip(1).filter(filter).toList() }
}

/**
 * List all paths recursively inside the directory which point to files.
 *
 * Returns [Either.Left] if the path does not exist, otherwise a list of all paths.
 */
fun Path.listFilesRecursively(): Either<ListPathError, List<Path>>  {
    return this.listPathsRecursively { !it.isDirectory() }
}

/**
 * Copies all directory siblings to [dest].
 *
 * Missing parent directories are automatically created.
 *
 * # Example
 * ```
 * Src:
 * a
 * + b.txt
 * + c.txt
 * + d
 *   + g.txt
 *
 * Dest:
 * b
 * + b.txt
 * + c.txt
 * + d
 *   + g.txt
 * ```
 */
fun Path.copyDirectorySiblingsRecursively(dest: Path) = walkFileTree(CopyDirectorySiblingsFileVisitor(this, dest))

/**
 * Copies all directory to [dest].
 *
 * Missing parent directories are automatically created.
 *
 * # Example
 * Src:
 * ```
 * a
 * + b.txt
 * + c.txt
 * + d
 *   + g.txt
 *
 * Dest:
 * b
 * + a
 *   + b.txt
 *   + c.txt
 *   + d
 *     + g.txt
 * ```
 */
fun Path.copyDirectoryRecursivelyTo(dest: Path) = walkFileTree(CopyDirectoryFileVisitor(this, dest))

/**
 * Moves all directory siblings to [dest].
 *
 * Missing parent directories are automatically created.
 * All siblings are delete, but the source directory itself will not be deleted.
 *
 * # Example
 *
 * ```
 * Src:
 * a
 * + b.txt
 * + c.txt
 * + d
 *   + g.txt
 *
 * Dest:
 * b
 * + b.txt
 * + c.txt
 * + d
 *   + g.txt
 * ```
 */
fun Path.moveDirectorySiblingsRecursively(dest: Path) = walkFileTree(MoveDirectorySiblingsFileVisitor(this, dest))


/**
 * Copies all directory to [dest].
 *
 * Missing parent directories are automatically created.
 * The whole source directory will be deleted.
 *
 * # Example
 * ```
 * Src:
 * a
 * + b.txt
 * + c.txt
 * + d
 *   + g.txt
 *
 * Dest:
 * b
 * + a
 *   + b.txt
 *   + c.txt
 *   + d
 *     + g.txt
 * ```
 */
fun Path.moveDirectoryRecursively(dest: Path) = walkFileTree(MoveDirectoryFileVisitor(this, dest))

class SafeDeleteError(val deletedFiles: List<Path>, val unableToDeleteFiles: List<Path>)

fun Path.safeDeleteRecursively(): Either<SafeDeleteError, List<Path>> = TODO()

/**
 * Result class for a copy or move operation
 */
data class CopyResult(
    /**
     * Contains the source paths which were moved or copied.
     */
    val fromSrc: List<Path>,

    /**
     * Contains the destination paths which were moved or copied.
     */
    val toDest: List<Path>
)

data class FileVisitorError(val failedFile: Path, val exception: IOException)

private fun Path.walkFileTree(visitor: CopyFileVisitor): Ior<NonEmptyList<FileVisitorError>, CopyResult> {
    Files.walkFileTree(this@walkFileTree, visitor)
    val errors = visitor.errors.toNonEmptyListOrNull()
    if (errors != null) {
        return errors.leftIor()
    }

    return visitor.result.rightIor()
}

private sealed class CopyFileVisitor(protected val src: Path, private val dest: Path) : FileVisitor<Path> {

    protected val ioExceptions = mutableListOf<FileVisitorError>()

    val fromSrc: MutableList<Path> = mutableListOf()
    val toDest: MutableList<Path> = mutableListOf()

    val errors: List<FileVisitorError>
        get() = ioExceptions.toList()

    val result: CopyResult
        get() = CopyResult(fromSrc, toDest)

    abstract val anchor: Path

    abstract fun action(srcPath: Path, destPath: Path)

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        val relativePath = anchor.relativize(file)
        val destPath = dest `|` relativePath

        action(file, destPath)
        fromSrc.add(file)
        toDest.add(destPath)

        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
        ioExceptions.add(FileVisitorError(file, exc))
        return FileVisitResult.CONTINUE
    }
}

private open class CopyDirectorySiblingsFileVisitor(src: Path, dest: Path) : CopyFileVisitor(src, dest) {

    override val anchor: Path
        get() = src

    override fun action(srcPath: Path, destPath: Path) {
        destPath.createParentDirectories()
        srcPath.copyTo(destPath)
    }

    override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        // Nothing because we copy
        return FileVisitResult.CONTINUE
    }
}

private class CopyDirectoryFileVisitor(src: Path, dest: Path) : CopyDirectorySiblingsFileVisitor(src, dest) {

    override val anchor: Path
        get() = src.parent
}

private open class MoveDirectorySiblingsFileVisitor(
    src: Path,
    dest: Path,
    private val deleteSrc: Boolean = false
) : CopyFileVisitor(src, dest) {

    override val anchor: Path
        get() = src

    override fun action(srcPath: Path, destPath: Path) {
        destPath.createParentDirectories()
        srcPath.moveTo(destPath)
    }

    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
        if (exc == null && (dir != src || deleteSrc)) {
            try {
                dir.deleteExisting()
            } catch (exception: DirectoryNotEmptyException) {
                ioExceptions.add(FileVisitorError(dir, exception))
            }
        }

        // Nothing because we copy
        return FileVisitResult.CONTINUE
    }
}

private class MoveDirectoryFileVisitor(src: Path, dest: Path): MoveDirectorySiblingsFileVisitor(src, dest, true) {

    override val anchor: Path
        get() = src.parent
}
