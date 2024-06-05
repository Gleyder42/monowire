package com.github.gleyder42.monowire.common.model

import java.nio.file.Path

typealias ModPath = Path

typealias ModFiles = Set<Path>

@JvmInline
value class ModVersion(val string: String)

@JvmInline
value class ModId(val integer: Int)

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
    val features: List<ModFeature>
)

data class ModFeature(
    val modPath: ModPath,
    val version: ModVersion,
    val modDetails: ModDetails,
    val descriptor: ModFeatureDescriptor,
    val dependencies: List<ModFeature>,
)

data class ModFeatureDescriptor(
    val key: ModFeatureKey,
    val id: ModId
)

