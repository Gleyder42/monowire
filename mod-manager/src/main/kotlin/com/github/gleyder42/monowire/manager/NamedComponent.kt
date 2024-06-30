package com.github.gleyder42.monowire.manager

import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import java.nio.file.Path
import org.koin.core.component.inject

object NamedComponent : KoinComponent {

    object Key {
        const val GAME_DIRECTORY = "gameDirectory"
        const val TEMPORARY_DIRECTORY = "temporaryDirectory"
    }

    fun gameDirectory() = inject<Path>(named(Key.GAME_DIRECTORY))

    fun temporaryDirectory() = inject<Path>(named(Key.TEMPORARY_DIRECTORY))
}