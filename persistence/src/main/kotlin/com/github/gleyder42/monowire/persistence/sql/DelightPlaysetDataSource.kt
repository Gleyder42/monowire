package com.github.gleyder42.monowire.persistence.sql

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.github.gleyder42.monowire.common.model.*
import com.github.gleyder42.monowire.persistence.Database
import com.github.gleyder42.monowire.persistence.datasource.ModCatalogueDataSource
import com.github.gleyder42.monowire.persistence.datasource.PlaysetDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class DelightPlaysetDataSource : PlaysetDataSource, KoinComponent {

    private val database by inject<Database>()
    private val modCatalogue by inject<ModCatalogueDataSource>()

    override suspend fun addPlayset(playset: Playset) {
        database.playsetQueries.createPlayset(playset.key.value)

        for (mod in playset.mods) {
            database.playsetQueries.addModToPlayset(
                playset.key.value,
                mod.descriptor.key.string,
                mod.descriptor.id.long,
                mod.version.string
            )
        }
    }

    @ExperimentalCoroutinesApi
    override suspend fun getPlaysetMods(key: PlaysetKey): List<ModFeature> {
        return database.playsetQueries.getPlaysetModFeatures(key.value)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .flatMapConcat { it.asFlow() }
            .mapNotNull {
                val modDescriptor = ModDescriptor(ModId(it.modId), ModVersion(it.version))
                val featureKey = ModFeatureKey(it.featureKey)
                modCatalogue.getModFeature(featureKey, modDescriptor)
            }.toList(mutableListOf())
    }
}