package com.github.gleyder42.monowire.persistence.sql

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import arrow.core.Either
import arrow.core.mapNotNull
import com.github.gleyder42.monowire.common.model.*
import com.github.gleyder42.monowire.persistence.Database
import com.github.gleyder42.monowire.persistence.catchingTransactionWithResult
import com.github.gleyder42.monowire.persistence.datasource.ModCatalogueDataSource
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.sqlite.SQLiteErrorCode

@Single
class DelightModCatalogueDataSource : ModCatalogueDataSource, KoinComponent {

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
            when (it.resultCode) {
                SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE -> ModCatalogueError.DuplicateModError(mod.descriptor)
                else -> throw it
            }
        }
    }

    override suspend fun removeMod(descriptor: ModDescriptor): Boolean {
        val removedCount =
            database.catalogueQueries.removeMod(descriptor.id.integer.toLong(), descriptor.version.string).awaitAsOne()
        return removedCount >= 1
    }

    override suspend fun getMod(descriptor: ModDescriptor): Mod? {
        val modDetail = database.catalogueQueries.getModDetail(descriptor.id.long, descriptor.version.string)
            .awaitAsOneOrNull() ?: return null

        val modFeatures = getModFeatures(descriptor)
        return Mod(
            descriptor,
            ModDetails(DisplayName(modDetail.displayName), modDetail.author?.let { ModAuthor(it) }),
            descriptor.version,
            modFeatures
        )
    }

    override suspend fun getModFeatures(descriptor: ModDescriptor): List<ModFeature> {
        val getMod = database.catalogueQueries.getCompleteMod(descriptor.id.long, descriptor.version.string)
            .awaitAsList()

        val entries = getMod
            .groupBy(
                { ModFeatureKey(it.key) to ModPath.of(it.modFiles) }
            ) {
                if (it.dependencyFeatureKey != null && it.dependencyId != null && it.dependencyVersion != null) {
                    ModFeatureKey(it.dependencyFeatureKey) to ModDescriptor(
                        ModId(it.dependencyId),
                        ModVersion(it.dependencyVersion)
                    )
                } else {
                    null
                }
            }.map {
                val dependencies = it.value.filterNotNull()
                it.key to dependencies
            }


        return entries.map {
            val (modFeatureKey, modPath) = it.first
            val modFeatureDescriptor = ModFeatureDescriptor(modFeatureKey, descriptor.id)

            val dependencies = it.second
                .mapNotNull { (modFeatureKey, modDescriptor) -> getModFeature(modFeatureKey, modDescriptor) }
                .toList()

            ModFeature(
                modPath,
                descriptor.version,
                modFeatureDescriptor,
                dependencies
            )
        }.toList()
    }

    override suspend fun getModFeature(key: ModFeatureKey, descriptor: ModDescriptor): ModFeature? {
        val modPath = database.catalogueQueries.getModFeature(
            version = descriptor.version.string,
            modId = descriptor.id.long,
            featureKey = key.string
        ).awaitAsOneOrNull() ?: return null

        return ModFeature(
            ModPath.of(modPath),
            descriptor.version,
            ModFeatureDescriptor(key, descriptor.id),
            emptyList()
        )
    }

    override suspend fun exists(descriptor: ModDescriptor): Boolean =
        database.catalogueQueries.exists(descriptor.id.integer.toLong(), descriptor.version.string).awaitAsOne() >= 1
}

sealed interface ModCatalogueError {

    data class DuplicateModError(val descriptor: ModDescriptor) : ModCatalogueError
}