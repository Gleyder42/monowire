package com.github.gleyder42.monowire.common.model

@JvmInline
value class PlaysetKey(val value: String)

@JvmInline
value class PlaysetName(val value: String)

data class Playset(
    val name: PlaysetName,
    val key: PlaysetKey,
    val mods: List<ModFeature>
)