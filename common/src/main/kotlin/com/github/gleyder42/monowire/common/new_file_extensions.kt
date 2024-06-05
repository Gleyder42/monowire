package com.github.gleyder42.monowire.common

import arrow.core.*
import arrow.core.raise.catch
import arrow.core.raise.either
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*

/**
 * Infix function for [Path.resolve]
 */
infix fun Path.resolve(other: Path): Path = this.resolve(other)

/**
 * Infix function for [Path.resolve]
 */
infix fun Path.resolve(other: String): Path = this.resolve(other)

/**
 * List all paths recursively inside the directory.
 *
 * Returns [Either.Left] if the path does not exist, otherwise a list of all paths.
 */
fun Path.listPathsRecursively(filter: (Path) -> Boolean): Either<IOException, List<Path>> = either {
    val path = this@listPathsRecursively

    catch({Files.walk(path).use { it.skip(1).filter(filter).toList() }}) { exception: IOException -> raise(exception) }
}

/**
 * List all paths recursively inside the directory which point to files.
 *
 * Returns [Either.Left] if the path does not exist, otherwise a list of all paths.
 */
fun Path.listFilesRecursively(): Either<IOException, List<Path>> {
    return this.listPathsRecursively { !it.isDirectory() }
}

fun Path.listFilesRecursivelyOrEmpty(): List<Path> = this.listFilesRecursively().getOrElse { emptyList() }

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
fun Path.copyDirectorySiblingsRecursivelyTo(dest: Path) = test(dest, this, ::copyAction, ::keepDirectory)

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
fun Path.copyDirectoryRecursivelyTo(dest: Path) = test(dest, this.parent, ::copyAction, ::keepDirectory)

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
fun Path.moveDirectorySiblingsRecursivelyTo(dest: Path) = test(
    dest,
    this,
    ::moveAction
) { errors, dir, exc -> deleteDirectory(errors = errors, src = this, deleteSrc = false, dir = dir, exc = exc) }


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
fun Path.moveDirectoryRecursivelyTo(dest: Path) = test(dest, this.parent, ::moveAction) { errors, dir, exc ->
    deleteDirectory(
        errors = errors,
        src = this,
        deleteSrc = true,
        dir = dir,
        exc = exc
    )
}

fun Path.safeDeleteRecursively(deleteSource: Boolean = false): Ior<NonEmptyList<FileVisitorError>, List<Path>> {
    val errors: FileVisitorErrorSet = mutableSetOf()
    val deleted = mutableListOf<Path>()

    val visitor = CompositeFileVisitor(
        preVisitDirectory = ::keepDirectory,
        visitFile = { file: Path, _: BasicFileAttributes ->
            file.deleteExisting()
            deleted.add(file)
            FileVisitResult.CONTINUE
        },
        visitFileFailed = { file, exc -> recordFailError(errors = errors, file = file, exc = exc) },
        postVisitDirectory = { dir, exc -> deleteDirectory(errors, src = this, deleteSource, dir, exc) }
    )
    Files.walkFileTree(this, visitor)

    val nonEmptyErrors = errors.toNonEmptyListOrNull()
    return if (nonEmptyErrors != null) {
        (nonEmptyErrors to deleted).bothIor()
    } else {
        deleted.rightIor()
    }
}

/**
 * Result class for a copy or move operation
 */
data class CopyResult(
    /**
     * Contains the source paths which were moved or copied.
     */
    val fromSrc: Set<Path>,

    /**
     * Contains the destination paths which were moved or copied.
     */
    val toDest: Set<Path>
) {
    val isEmpty: Boolean
        get() = fromSrc.isEmpty() && toDest.isEmpty()
}

data class FileVisitorError(val failedFile: Path, val exception: IOException)

private class CompositeFileVisitor(
    private val preVisitDirectory: (dir: Path, attrs: BasicFileAttributes) -> FileVisitResult,
    private val visitFile: (file: Path, attrs: BasicFileAttributes) -> FileVisitResult,
    private val visitFileFailed: (file: Path, exc: IOException) -> FileVisitResult,
    private val postVisitDirectory: (dir: Path, exc: IOException?) -> FileVisitResult
) : FileVisitor<Path> {
    // Use .invoke() here so Kotlin can differentiate between function and property
    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = preVisitDirectory.invoke(dir, attrs)

    override fun visitFile(file: Path, attrs: BasicFileAttributes) = visitFile.invoke(file, attrs)

    override fun visitFileFailed(file: Path, exc: IOException) = visitFileFailed.invoke(file, exc)

    override fun postVisitDirectory(dir: Path, exc: IOException?) = postVisitDirectory.invoke(dir, exc)
}

typealias FileVisitorErrorSet = MutableSet<FileVisitorError>

// This method should do nothing, therefore parameters are not used
@Suppress("UNUSED_PARAMETER")
fun keepDirectory(path: Path, attr: BasicFileAttributes) = FileVisitResult.CONTINUE

// This method should do nothing, therefore parameters are not used
@Suppress("UNUSED_PARAMETER")
fun keepDirectory(errors: FileVisitorErrorSet, dir: Path, exc: IOException?) = FileVisitResult.CONTINUE

fun deleteDirectory(
    errors: FileVisitorErrorSet,
    src: Path,
    deleteSrc: Boolean,
    dir: Path,
    exc: IOException?
): FileVisitResult {
    if (exc == null && (dir != src || deleteSrc)) {
        try {
            dir.deleteExisting()
        } catch (exception: DirectoryNotEmptyException) {
            errors.add(FileVisitorError(dir, exception))
        } catch (exception: NoSuchFileException) {
            // NoSuchFileException should never be thrown.
            // dir must exist because it is supplied by the FileVisitor
            // visiting a not existing file does not make sense
            errors.add(FileVisitorError(dir, exception))
        }
    }
    return FileVisitResult.CONTINUE
}

fun Path.setup(setup: (fromSrc: MutableSet<Path>, toDest: MutableSet<Path>, errors: FileVisitorErrorSet) -> FileVisitor<Path>): Ior<NonEmptyList<FileVisitorError>, CopyResult> {
    val fromSrc: MutableSet<Path> = mutableSetOf()
    val toDest: MutableSet<Path> = mutableSetOf()
    val errors: FileVisitorErrorSet = mutableSetOf()

    val visitor = setup(fromSrc, toDest, errors)
    Files.walkFileTree(this, visitor)

    val result = CopyResult(fromSrc = fromSrc, toDest = toDest)
    val nonEmptyErrors = errors.toNonEmptyListOrNull()
    return when {
        nonEmptyErrors == null -> result.rightIor()
        result.isEmpty -> nonEmptyErrors.leftIor()
        else -> (nonEmptyErrors to result).bothIor()
    }
}

fun Path.test(
    dest: Path,
    anchor: Path,
    action: (srcPath: Path, destPath: Path) -> Unit,
    postVisitDirectory: (errors: FileVisitorErrorSet, dir: Path, exc: IOException?) -> FileVisitResult
): Ior<NonEmptyList<FileVisitorError>, CopyResult> {
    return setup { fromSrc, toDest, errors ->
        CompositeFileVisitor(
            preVisitDirectory = ::keepDirectory,
            visitFile = { file: Path, _: BasicFileAttributes ->
                visitAndLogFile(
                    action = { srcPath, destPath ->
                        try {
                            action(srcPath, destPath)
                            true
                        } catch (exception: IOException) {
                            // We create an error for both src and dest
                            errors.add(FileVisitorError(srcPath, exception))
                            errors.add(FileVisitorError(destPath, exception))
                            false
                        }
                    },
                    file = file,
                    dest = dest,
                    anchor = anchor,
                    fromSrc = fromSrc,
                    toDest = toDest
                )
            },
            visitFileFailed = { file, exc -> recordFailError(errors = errors, file = file, exc = exc) },
            postVisitDirectory = { dir, exc -> postVisitDirectory(errors, dir, exc) }
        )
    }
}

fun visitAndLogFile(
    action: (srcPath: Path, destPath: Path) -> Boolean,
    file: Path,
    dest: Path,
    anchor: Path,
    fromSrc: MutableSet<Path>,
    toDest: MutableSet<Path>
): FileVisitResult {
    val relativePath = anchor.relativize(file)
    val destPath = dest resolve relativePath

    val successful = action(file, destPath)
    if (successful) {
        fromSrc.add(file)
        toDest.add(destPath)
    }

    return FileVisitResult.CONTINUE
}

fun copyAction(srcPath: Path, destPath: Path) {
    destPath.createParentDirectories()
    srcPath.copyTo(destPath)
}

fun moveAction(srcPath: Path, destPath: Path) {
    destPath.createParentDirectories()
    srcPath.moveTo(destPath)
}

fun recordFailError(errors: FileVisitorErrorSet, file: Path, exc: IOException): FileVisitResult {
    errors.add(FileVisitorError(file, exc))
    return FileVisitResult.CONTINUE
}


