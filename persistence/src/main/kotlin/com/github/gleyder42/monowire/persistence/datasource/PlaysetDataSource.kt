package com.github.gleyder42.monowire.persistence.datasource

import com.github.gleyder42.monowire.common.model.ModFeature
import com.github.gleyder42.monowire.common.model.Playset
import com.github.gleyder42.monowire.common.model.PlaysetKey

interface PlaysetDataSource {

    suspend fun addPlayset(playset: Playset)

    suspend fun getPlaysetMods(key: PlaysetKey): List<ModFeature>
}