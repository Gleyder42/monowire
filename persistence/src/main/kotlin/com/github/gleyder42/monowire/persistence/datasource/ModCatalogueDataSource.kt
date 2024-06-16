package com.github.gleyder42.monowire.persistence.datasource

import arrow.core.Either
import com.github.gleyder42.monowire.common.model.Mod
import com.github.gleyder42.monowire.common.model.ModDescriptor
import com.github.gleyder42.monowire.common.model.ModFeature
import com.github.gleyder42.monowire.common.model.ModFeatureKey
import com.github.gleyder42.monowire.persistence.sql.ModCatalogueError

interface ModCatalogueDataSource {

    suspend fun addMod(mod: Mod): Either<ModCatalogueError, Unit>

    suspend fun removeMod(descriptor: ModDescriptor): Boolean

    suspend fun getMod(descriptor: ModDescriptor): Mod?

    suspend fun getModFeatures(descriptor: ModDescriptor): Set<ModFeature>

    suspend fun getModFeature(key: ModFeatureKey, descriptor: ModDescriptor): ModFeature?

    suspend fun exists(descriptor: ModDescriptor): Boolean
}