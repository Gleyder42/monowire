package com.github.gleyder42.monowire.manager

import arrow.core.Either
import com.github.gleyder42.monowire.common.model.Mod
import com.github.gleyder42.monowire.common.model.ModDescriptor
import com.github.gleyder42.monowire.persistence.datasource.ModCatalogueDataSource
import com.github.gleyder42.monowire.persistence.sql.ModCatalogueError
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ModCatalogue : KoinComponent {

    private val dataSource by inject<ModCatalogueDataSource>()

    suspend fun addModIfNotExist(mod: Mod) {
        if (!dataSource.exists(mod.descriptor)) {
            dataSource.addMod(mod)
        }
    }

    suspend fun addMod(mod: Mod): Either<ModCatalogueError, Unit> {
        return dataSource.addMod(mod)
    }

    suspend fun getMod(descriptor: ModDescriptor): Mod? {
        return dataSource.getMod(descriptor)
    }
}