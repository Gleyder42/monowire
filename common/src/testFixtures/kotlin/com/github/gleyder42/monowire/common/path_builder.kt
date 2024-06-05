package com.github.gleyder42.monowire.common

import java.io.Closeable
import java.io.FileInputStream
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

typealias PathBuilderFunction = PathBuilder.() -> Unit

fun dir(path: Path, builder: PathBuilderFunction = {}): Path {
    path.createDirectories()
    builder(PathBuilder(path))
    return path
}

fun scopedDir(path: Path, builder: ScopedPathBuilder.()->Unit = {}): Pair<Path, ScopedPathBuilder> {
    path.createDirectories()
    val pathBuilder = ScopedPathBuilder(path)
    builder(pathBuilder)
    return path to pathBuilder
}

open class PathBuilder(protected val path: Path) {

    fun file(name: String) {
        path.resolve(name).createFile()
    }

    fun dir(name: String, builder: PathBuilderFunction = {}): PathBuilder {
        val dir = path.resolve(name)
        dir.createDirectories()
        val pathBuilder = PathBuilder(dir)
        builder(pathBuilder)
        return pathBuilder
    }
}

class ScopedPathBuilder(path: Path) : PathBuilder(path), Closeable {

    private val openFiles = mutableListOf<FileInputStream>()

    fun file(name: String, lockFile: Boolean = false) {
        val file = path.resolve(name)
        file.createFile()

        if (lockFile) {
            openFiles.add(FileInputStream(file.toFile()))
        }
    }

    override fun close() {
        openFiles.forEach { it.close() }
    }
}