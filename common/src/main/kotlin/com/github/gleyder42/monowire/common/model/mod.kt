package com.github.gleyder42.monowire.common.model

import java.nio.file.Path

typealias ModPath = Path

typealias ModFiles = Set<Path>

@JvmInline
value class ModVersion(val string: String)

@JvmInline
value class ModId(val integer: Int) {

    constructor(long: Long) : this(long.toInt())

    val long: Long
        get() = integer.toLong()
}

@JvmInline
value class ModFeatureKey(val string: String)

@JvmInline
value class DisplayName(val name: String)

@JvmInline
value class ModAuthor(val string: String)

typealias ReadOnlyPath = Path

data class ModDetails(
    val displayName: DisplayName,
    val author: ModAuthor?
)

data class ModDescriptor(val id: ModId, val version: ModVersion)

data class Mod(
    val descriptor: ModDescriptor,
    val details: ModDetails,
    val version: ModVersion,
    val features: Set<ModFeature>
) {

    companion object {

        operator fun invoke(
            modId: ModId,
            version: ModVersion,
            name: DisplayName,
            author: ModAuthor? = null,
            features: Set<ModFeature> = emptySet()
        ): Mod {
            val modDescriptor = ModDescriptor(modId, version)
            val modDetails = ModDetails(name, author)

            return Mod(
                modDescriptor,
                modDetails,
                version,
                features
            )
        }
    }
}

data class PartialMod(
    val name: DisplayName,
    val features: Set<PartialModFeature>
) {
    fun toMod(modDescriptor: ModDescriptor, author: ModAuthor? = null)
        = toMod(modDescriptor.id, modDescriptor.version, author)

    fun toMod(modId: ModId, version: ModVersion, author: ModAuthor? = null): Mod {
        val modDescriptor = ModDescriptor(modId, version)
        val modDetails = ModDetails(this.name, author)
        val modFeatures = this.features.map {
            val modFeatureDescriptor = ModFeatureDescriptor(it.modFeatureKey, modId)
            ModFeature(it.modPath, version, modFeatureDescriptor, emptyList())
        }.toSet()

        return Mod(
            modDescriptor,
            modDetails,
            version,
            modFeatures,
        )
    }
}

data class PartialModFeature(
    val modPath: ModPath,
    val modFeatureKey: ModFeatureKey,
)

data class ModFeature(
    val modPath: ModPath,
    val version: ModVersion,
    val descriptor: ModFeatureDescriptor,
    val dependencies: List<ModFeature>,
) {

    companion object {

        operator fun invoke(
            modPath: ModPath,
            id: ModId,
            version: ModVersion,
            featureKey: ModFeatureKey,
            dependencies: List<ModFeature> = emptyList()
        ): ModFeature {
            return ModFeature(
                modPath,
                version,
                ModFeatureDescriptor(featureKey, id),
                dependencies
            )
        }
    }
}

data class ModFeatureDescriptor(
    val key: ModFeatureKey,
    val id: ModId
)

