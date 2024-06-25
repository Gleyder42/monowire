package com.github.gleyder42.monowire.mod.analyzer

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.mapOrAccumulate
import arrow.core.nel
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.gleyder42.monowire.common.model.ModAuthor
import com.github.gleyder42.monowire.common.model.ModId
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class ModHistoryExtractor {

    // https://www.nexusmods.com/cyberpunk2077/Core/Libs/Common/Managers/Mods?GetDownloadHistory=

    sealed interface Error {

        data class CannotCastField(val exception: ClassCastException) : Error

        data class CannotParseInput(val exception: JacksonException) : Error
    }

    fun extractDownloadedModHistory(input: String): EitherNel<Error, List<DownloadedMod>> = either {
        val mapper = JsonMapper().registerKotlinModule()

        val rawModHistory = catch({ mapper.readValue<RawModHistory>(input) }) { exc: JacksonException -> raise(Error.CannotParseInput(exc).nel())}

        val castToLocalDateTime: Raise<Error>.(Any)->LocalDateTime = { value ->
            LocalDateTime.ofInstant(
                Instant.ofEpochSecond(
                    value.castTo<Int>().bind().toLong()
                ), ZoneId.systemDefault()
            )
        }

        return rawModHistory.data
            .mapOrAccumulate { rawMod ->
                DownloadedMod(
                    thumbnail = rawMod[0].castTo<String>().bind(),
                    name = rawMod[1].castTo<String>().bind(),
                    lastDownloaded = castToLocalDateTime(rawMod[2]),
                    authorName = ModAuthor(rawMod[3].castTo<String>().bind()),
                    category = rawMod[4].castTo<String>().bind(),
                    updated = castToLocalDateTime(rawMod[2]),
                    gameId = rawMod[7].castTo<Int>().bind(),
                    modId = ModId(rawMod[8].castTo<Int>().bind().toLong()),
                    gameName = rawMod[9].castTo<String>().bind(),
                    authorId = rawMod[11].castTo<String>().bind(),
                    isAdult = rawMod[13] == "1"
                )
            }
    }

    private inline fun <reified T> Any.castTo(): Either<Error, T> = either {
        catch({ this@castTo as T }) { exc: ClassCastException -> raise(Error.CannotCastField(exc)) }
    }

    data class RawModHistory(val data: List<List<Any>>)

    data class DownloadedMod(
        // https://staticdelivery.nexusmods.com/mods/{staticImageId}/images/thumbnails/{thumbnail}
        val thumbnail: String, // 0
        val name: String, // 1
        val lastDownloaded: LocalDateTime,   // 2
        val authorName: ModAuthor, // 3
        val category: String, // 4
        val updated: LocalDateTime, // 5
        val gameId: Int, // 7
        val modId: ModId, // 8
        val gameName: String, // 9
        // https://www.nexusmods.com/{game}/users/{authorId}
        val authorId: String, // 11
        val isAdult: Boolean // 13
    )
}