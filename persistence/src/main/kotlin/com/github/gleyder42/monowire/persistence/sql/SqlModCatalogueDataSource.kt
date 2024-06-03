package com.github.gleyder42.monowire.persistence.sql

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import arrow.core.Either
import com.github.gleyder42.monowire.common.model.*
import com.github.gleyder42.monowire.persistence.Database
import com.github.gleyder42.monowire.persistence.catchingTransactionWithResult
import com.github.gleyder42.monowire.persistence.datasource.ModCatalogueDataSource
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.sqlite.SQLiteErrorCode

@Single
class SqlModCatalogueDataSource : ModCatalogueDataSource, KoinComponent {

    private val database by inject<Database>()

    override suspend fun addMod(mod: Mod): Either<ModCatalogueError, Unit> {
        return database.catchingTransactionWithResult {
            val detail = mod.details
            database.catalogueQueries.addMod(
                mod.descriptor.id.integer.toLong(),
                mod.version.string,
                detail.displayName.name,
                detail.author?.string
            )

            val modLongId = mod.descriptor.id.integer.toLong()
            for (modFeature in mod.features) {
                database.catalogueQueries.addModFeature(
                    modFeature.descriptor.key.string,
                    modLongId,
                    modFeature.modPath.toString(),
                    mod.version.string
                )

                for (dependency in modFeature.dependencies) {
                    database.catalogueQueries.addModFeatureDependency(
                        modLongId,
                        mod.version.string,
                        modFeature.descriptor.key.string,
                        dependency.descriptor.id.integer.toLong(),
                        dependency.version.string,
                        dependency.descriptor.key.string,
                    )
                }
            }
        }.mapLeft {
            // Only errors which are considered part of the domain are return as Either
            when(it.resultCode) {
                SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE -> ModCatalogueError.DuplicateModError(mod.descriptor)
                else -> throw it
            }
        }
    }

    override suspend fun removeMod(descriptor: ModDescriptor): Boolean {
        val removedCount = database.catalogueQueries.removeMod(descriptor.id.integer.toLong(), descriptor.version.string).awaitAsOne()
        return removedCount >= 1
    }

    override suspend fun getMod(descriptor: ModDescriptor): Mod? {
        val awaitAsOneOrNull = database.catalogueQueries.getMod().awaitAsOneOrNull() ?: return null

        return with(awaitAsOneOrNull) {
            val modId = ModId(id.toInt())
            val modVersion = ModVersion(version)
            val modAuthor = author?.let { ModAuthor(it) }

            val modDetails = ModDetails(DisplayName(displayName), modAuthor)

            val modFeatures = database.catalogueQueries.getModFeatures(version, id) { key, modFiles ->
                ModFeature(
                    ModPath.of(modFiles),
                    modVersion,
                    modDetails,
                    ModFeatureDescriptor(ModFeatureKey(key), modId),
                    emptyList()
                )
            }.awaitAsList()

            Mod(
                ModDescriptor(modId, modVersion),
                modDetails,
                modVersion,
                modFeatures
            )
        }
    }


    override suspend fun exists(descriptor: ModDescriptor): Boolean =
        database.catalogueQueries.exists(descriptor.id.integer.toLong(), descriptor.version.string).awaitAsOne() >= 1

}

sealed interface ModCatalogueError {

    data class DuplicateModError(val descriptor: ModDescriptor) : ModCatalogueError
}