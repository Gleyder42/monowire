package com.github.gleyder42.monowire.persistence.sql

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.github.gleyder42.monowire.common.model.*
import com.github.gleyder42.monowire.persistence.Database
import com.github.gleyder42.monowire.persistence.datasource.ModInstanceLibraryDataSource
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Path
import java.util.UUID

@Single
class DelightModInstanceLibraryDataSource : ModInstanceLibraryDataSource, KoinComponent {

    private val database by inject<Database>()

    override suspend fun addModInstance(modInstance: ModInstance) {
        val modId = modInstance.modFeatureDescriptor.id.long
        val modVersion = modInstance.version.string

        val instanceId = UUID.randomUUID().toByteArray()
        database.libraryQueries.addModInstance(
            instanceId = instanceId,
            id = modId,
            version = modVersion,
            featureKey = modInstance.modFeatureDescriptor.key.string,
        )

        for (file in modInstance.installedFiles) {
            database.libraryQueries.addFileToModInstance(
                instanceId = instanceId,
                file = file.toString()
            )
        }
    }

    override suspend fun removeModInstance(descriptor: ModFeatureDescriptor) {
        database.libraryQueries.removeModInstance(
            id = descriptor.id.long,
            featureKey = descriptor.key.string
        )
    }

    override suspend fun getModInstance(descriptor: ModFeatureDescriptor): ModInstance? {
        val instance = database.libraryQueries.getModInstance(
            id = descriptor.id.long,
            featureKey = descriptor.key.string
        ).awaitAsOneOrNull() ?: return null

        val files = database.libraryQueries.getModInstanceFiles(instanceId = instance.instanceId).awaitAsList()

        return ModInstance(
            ModInstanceId(instance.instanceId),
            files.map { Path.of(it) }.toSet(),
            ModVersion(instance.version),
            ModFeatureDescriptor(ModFeatureKey(instance.featureKey), ModId(instance.modId))
        )
    }

    override suspend fun isInstalled(descriptor: ModFeatureDescriptor): Boolean {
        TODO("Not yet implemented")
    }
}