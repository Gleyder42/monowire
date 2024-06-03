package com.github.gleyder42.monowire.persistence.datasource

import arrow.core.Either
import com.github.gleyder42.monowire.common.model.Mod
import com.github.gleyder42.monowire.common.model.ModDescriptor
import com.github.gleyder42.monowire.persistence.sql.ModCatalogueError

interface ModCatalogueDataSource {

    suspend fun addMod(mod: Mod): Either<ModCatalogueError, Unit>

    suspend fun removeMod(descriptor: ModDescriptor): Boolean

    suspend fun getMod(descriptor: ModDescriptor): Mod?

    suspend fun exists(descriptor: ModDescriptor): Boolean
}