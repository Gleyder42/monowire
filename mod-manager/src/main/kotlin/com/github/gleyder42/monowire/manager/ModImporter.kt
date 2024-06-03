package com.github.gleyder42.monowire.manager

import com.github.gleyder42.monowire.common.model.*
import org.koin.core.component.KoinComponent
import kotlin.io.path.nameWithoutExtension

sealed interface ModImporterError : ModFeatureImporterError {

    data class ModAlreadyExists(val modId: ModId) : ModImporterError
}

sealed interface ModFeatureImporterError {

    data class CannotFindMod(val modDescriptor: ModDescriptor) : ModFeatureImporterError
}

object ModFeatureImporter : KoinComponent {

    fun importModFeatureAsMod(
        path: ReadOnlyPath,
        modId: ModId,
        modVersion: ModVersion,
        modName: DisplayName,
        author: ModAuthor? = null
    ): Mod {
        val details = ModDetails(modName, author)
        val descriptor = ModDescriptor(modId, modVersion)
        val feature = ModFeature(
            path,
            modVersion,
            details,
            ModFeatureDescriptor(ModFeatureKey(path.nameWithoutExtension), modId),
            emptyList()
        )

        return Mod(
            descriptor,
            details,
            modVersion,
            listOf(feature)
        )
    }
}