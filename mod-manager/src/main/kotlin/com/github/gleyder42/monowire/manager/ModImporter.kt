package com.github.gleyder42.monowire.manager

import com.github.gleyder42.monowire.common.model.*
import org.koin.core.component.KoinComponent
import kotlin.io.path.nameWithoutExtension

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
            ModFeatureDescriptor(fromPathName(path), modId),
            emptyList()
        )

        return Mod(
            descriptor,
            details,
            modVersion,
            setOf(feature)
        )
    }

    private fun fromPathName(path: ReadOnlyPath) = ModFeatureKey(path.nameWithoutExtension)
}
