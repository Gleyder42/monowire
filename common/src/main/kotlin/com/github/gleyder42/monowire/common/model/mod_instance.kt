package com.github.gleyder42.monowire.common.model

data class ModInstance(
    val installedFiles: ModFiles,
    val version: ModVersion,
    val modFeatureDescriptor: ModFeatureDescriptor
)
