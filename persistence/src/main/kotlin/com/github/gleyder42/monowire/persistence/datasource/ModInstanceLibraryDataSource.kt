package com.github.gleyder42.monowire.persistence.datasource

import com.github.gleyder42.monowire.common.model.ModFeatureDescriptor
import com.github.gleyder42.monowire.common.model.ModInstance

interface ModInstanceLibraryDataSource {

    suspend fun addModInstance(modInstance: ModInstance)

    suspend fun removeModInstance(descriptor: ModFeatureDescriptor)

    suspend fun getModInstance(descriptor: ModFeatureDescriptor): ModInstance

    suspend fun isInstalled(descriptor: ModFeatureDescriptor): Boolean
}