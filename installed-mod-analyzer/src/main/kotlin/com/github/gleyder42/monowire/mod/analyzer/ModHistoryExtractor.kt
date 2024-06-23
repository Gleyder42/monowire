package com.github.gleyder42.monowire.mod.analyzer

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.gleyder42.monowire.common.model.ModAuthor
import com.github.gleyder42.monowire.common.model.ModId

class ModHistoryExtractor {

    // https://www.nexusmods.com/cyberpunk2077/Core/Libs/Common/Managers/Mods?GetDownloadHistory=

    fun extractDownloadedModHistory(input: String): List<DownloadedMod> {
        val mapper = JsonMapper().registerKotlinModule()
        val rawModHistory = mapper.readValue<RawModHistory>(input)

        return rawModHistory.data
            .map { rawMod ->
                DownloadedMod(
                    thumbnail = rawMod[0] as String,
                    name = rawMod[1] as String,
                    authorName = ModAuthor(rawMod[3] as String),
                    category = rawMod[4] as String,
                    gameId = (rawMod[7] as Int),
                    modId = ModId((rawMod[8] as Int).toLong()),
                    gameName = rawMod[9] as String,
                    authorId = rawMod[11] as String,
                    isAdult = rawMod[13] == "1"
                )
            }
    }

    data class RawModHistory(val data: List<List<Any>>)

    data class DownloadedMod(
        // https://staticdelivery.nexusmods.com/mods/{staticImageId}/images/thumbnails/{thumbnail}
        val thumbnail: String, // 0
        val name: String, // 1
        // 2
        val authorName: ModAuthor, // 3
        val category: String, // 4
        val gameId: Int, // 7
        val modId: ModId, // 8
        val gameName: String, // 9
        // https://www.nexusmods.com/{game}/users/{authorId}
        val authorId: String, // 11
        val isAdult: Boolean // 13
    )
}