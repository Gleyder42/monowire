package com.github.gleyder42.monowire.common

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

typealias PathBuilderFunction = PathBuilder.() -> Unit

fun dir(path: Path, builder: PathBuilderFunction = {}): Path {
    path.createDirectories()
    builder(PathBuilder(path))
    return path
}

class PathBuilder(private val path: Path) {

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