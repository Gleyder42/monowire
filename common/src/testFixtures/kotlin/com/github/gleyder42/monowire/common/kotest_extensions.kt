package com.github.gleyder42.monowire.common

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.engine.spec.tempdir
import org.koin.core.context.loadKoinModules
import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.nio.file.Path


fun ShouldSpec.tempPath(): Path {
    return tempdir().toPath()
}

fun KoinTest.declare(configuration: ModuleDeclaration) {
    loadKoinModules(module(moduleDeclaration = configuration))
}