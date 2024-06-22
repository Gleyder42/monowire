package com.github.gleyder42.monowire.common

import arrow.core.*
import arrow.core.raise.catch
import arrow.core.raise.either
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*
import kotlin.io.path.deleteExisting
import kotlin.io.path.listDirectoryEntries as untypedDirectoryEntries;
import kotlin.io.path.deleteExisting as untypedDeleteExisting;

typealias IOResult<T> = Either<IOException, T>


/**
 * Infix function for [Path]
 */
infix fun Path.`⫽`(other: Path): Path = this.resolve(other)

/**
 * Infix function for [Path]
 */
infix fun Path.`⫽`(other: String): Path = this.resolve(other)

/**
 * List all paths recursively inside the directory.
 *
 * Returns [Either.Left] if the path does not exist, otherwise a list of all paths.
 */
fun Path.listPathsRecursively(filter: (Path) -> Boolean = { true }): IOResult<List<Path>> = either {
    catch({
        Files.walk(this@listPathsRecursively).use { it.skip(1).filter(filter).toList() }
    }) { exception: IOException -> raise(exception) }
}

fun Path.listDirectoryEntries(glob: String = "*"): IOResult<List<Path>> = either {
    catch({ this@listDirectoryEntries.untypedDirectoryEntries(glob) }) { exc: IOException -> raise(exc) }
}

/**
 * List all paths recursively inside the directory which point to files.
 *
 * Returns [Either.Left] if the path does not exist, otherwise a list of all paths.
 */
fun Path.listFilesRecursively(): IOResult<List<Path>> {
    return this.listPathsRecursively { !it.isDirectory() }
}

fun Path.deleteExisting(): IOResult<Unit> = either {
    catch({this@deleteExisting.untypedDeleteExisting()}) { exc: IOException -> raise(exc) }
}

fun Path.listFilesRecursivelyOrEmpty(): List<Path> = this.listFilesRecursively().getOrElse { emptyList() }


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
fun Path.copyDirectoryRecursivelyTo(dest: Path) =
    walkFileTree(dest = dest, anchor = parent, action = ::copy, postVisitDirectory = ::nothing)

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
fun Path.moveDirectorySiblingsRecursivelyTo(dest: Path) =
    walkFileTree(
        dest = dest,
        anchor = this,
        action = ::move,
        postVisitDirectory = { errors, dir, exc -> deleteDirectory(errors = errors, src = this, deleteSrc = false, dir = dir, exc = exc) }
    )

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
fun Path.moveDirectoryRecursivelyTo(dest: Path) =
    walkFileTree(
        dest = dest,
        anchor = parent,
        action = ::move,
        postVisitDirectory = { errors, dir, exc ->
            deleteDirectory(
                errors = errors,
                src = this,
                deleteSrc = true,
                dir = dir,
                exc = exc
            )
        }
    )

/*
 * The purpose of the PathHelper is to be mocked.
 * The methods of the PathHelper are extension functions for java.nio.file.Path.
 * The extension functions could also exist without the PathHelper, but top level extension functions cannot be
 * mocked individually. Only a whole file can be mocked.
 * Therefore, the Path extension functions are inside the PathHelper, because PathHelper can be mocked or be spied and
 * the extension function can be mocked individually.
 */
@Single
class PathHelper {

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
    fun Path.copyDirectorySiblingsRecursivelyTo(dest: Path) =
        walkFileTree(dest = dest, anchor = this, action = ::copy, postVisitDirectory = ::nothing)


    fun Path.safeDelete(): Either<FileError, Path> = either {
        catch({
            this@safeDelete.deleteExisting()
            this@safeDelete
        }) { exception: IOException -> raise(FileError(this@safeDelete, exception)) }
    }

    fun safeDeleteRecursively(path: Path, deleteSource: Boolean = false): IorNel<FileError, Set<Path>> {
        val errors: FileVisitorErrorSet = mutableSetOf()
        val deleted = mutableSetOf<Path>()

        val visitor = CompositeFileVisitor(
            preVisitDirectory = ::nothing,
            visitFile = { file: Path, _: BasicFileAttributes ->
                file.deleteExisting()
                deleted.add(file)
                FileVisitResult.CONTINUE
            },
            visitFileFailed = { file, exc -> addError(errors = errors, file = file, exc = exc) },
            postVisitDirectory = { dir, exc -> deleteDirectory(errors, src = path, deleteSource, dir, exc) }
        )
        Files.walkFileTree(path, visitor)

        val nonEmptyErrors = errors.toNonEmptyListOrNull()
        return if (nonEmptyErrors != null) {
            (nonEmptyErrors to deleted).bothIor()
        } else {
            deleted.rightIor()
        }
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

    companion object {

        fun both(paths: Set<Path>) = CopyResult(paths, paths)
    }
}

data class FileError(val failedFile: Path, val exception: IOException) {

    override fun toString() = "FileError(\n\tfailedFile=$failedFile,\n\texception=$exception)"
}

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

typealias FileVisitorErrorSet = MutableSet<FileError>

// This method should do nothing, therefore parameters are not used
@Suppress("UNUSED_PARAMETER")
private fun nothing(path: Path, attr: BasicFileAttributes) = FileVisitResult.CONTINUE

// This method should do nothing, therefore parameters are not used
@Suppress("UNUSED_PARAMETER")
private fun nothing(errors: FileVisitorErrorSet, dir: Path, exc: IOException?) = FileVisitResult.CONTINUE

private fun deleteDirectory(
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
            errors.add(FileError(dir, exception))
        } catch (exception: NoSuchFileException) {
            // NoSuchFileException should never be thrown.
            // dir must exist because it is supplied by the FileVisitor
            // visiting a not existing file does not make sense
            errors.add(FileError(dir, exception))
        }
    }
    return FileVisitResult.CONTINUE
}

private fun Path.walkFileTree(
    createVisitor: (fromSrc: MutableSet<Path>, toDest: MutableSet<Path>, errors: FileVisitorErrorSet) -> FileVisitor<Path>
): IorNel<FileError, CopyResult> {
    val fromSrc: MutableSet<Path> = mutableSetOf()
    val toDest: MutableSet<Path> = mutableSetOf()
    val errors: FileVisitorErrorSet = mutableSetOf()

    val visitor = createVisitor(fromSrc, toDest, errors)
    Files.walkFileTree(this, visitor)

    val result = CopyResult(fromSrc = fromSrc, toDest = toDest)

    return (errors to result).ior { result.isEmpty }
}

private fun Path.walkFileTree(
    dest: Path,
    anchor: Path,
    action: (srcPath: Path, destPath: Path) -> Unit,
    postVisitDirectory: (errors: FileVisitorErrorSet, dir: Path, exc: IOException?) -> FileVisitResult
): IorNel<FileError, CopyResult> {
    return this@walkFileTree.walkFileTree { fromSrc, toDest, errors ->
        CompositeFileVisitor(
            preVisitDirectory = ::nothing,
            visitFile = { file: Path, _: BasicFileAttributes ->
                addFileOperations(
                    action = { srcPath, destPath ->
                        try {
                            action(srcPath, destPath)
                            true
                        } catch (exception: IOException) {
                            // We create an error for both src and dest
                            errors.add(FileError(srcPath, exception))
                            errors.add(FileError(destPath, exception))
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
            visitFileFailed = { file, exc -> addError(errors = errors, file = file, exc = exc) },
            postVisitDirectory = { dir, exc -> postVisitDirectory(errors, dir, exc) }
        )
    }
}

private fun addFileOperations(
    action: (srcPath: Path, destPath: Path) -> Boolean,
    file: Path,
    dest: Path,
    anchor: Path,
    fromSrc: MutableSet<Path>,
    toDest: MutableSet<Path>
): FileVisitResult {
    val relativePath = anchor.relativize(file)
    val destPath = dest `⫽` relativePath

    val successful = action(file, destPath)
    if (successful) {
        fromSrc.add(file)
        toDest.add(destPath)
    }

    return FileVisitResult.CONTINUE
}

private fun copy(srcPath: Path, destPath: Path) {
    destPath.createParentDirectories()
    srcPath.copyTo(destPath)
}

private fun move(srcPath: Path, destPath: Path) {
    destPath.createParentDirectories()
    srcPath.moveTo(destPath)
}

private fun addError(errors: FileVisitorErrorSet, file: Path, exc: IOException): FileVisitResult {
    errors.add(FileError(file, exc))
    return FileVisitResult.CONTINUE
}

fun Iterable<Path>.relativizeTo(path: Path): Iterable<Path> = this.map { path.relativize(it) }

fun Path.relativizeTo(path: Path): Path = path.relativize(this)