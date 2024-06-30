package com.github.gleyder42.monowire.manager

import arrow.core.IorNel
import arrow.core.nel
import arrow.core.raise.ensureNotNull
import arrow.core.raise.iorNel
import com.github.gleyder42.monowire.common.model.ModDescriptor
import com.github.gleyder42.monowire.common.model.ModFeatureDescriptor
import com.github.gleyder42.monowire.common.model.ModId
import com.github.gleyder42.monowire.nexus.*
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URL
import java.nio.file.Path

@Single
class ModRecognizer : KoinComponent {

    private val gameDirectory by NamedComponent.temporaryDirectory()
    private val modCatalogue by inject<ModCatalogue>()
    private val nexusService by inject<NexusService>()
    private val nexusClient by inject<NexusClient>()

    suspend fun recognizeMod(modLinks: List<URL>): IorNel<CannotRecognizeModError, List<RecognizedMod>> = iorNel {
        for (modLink in modLinks) {
            val queryMap = modLink.query
                .split("&")
                .map {
                    val (key, value) = it.split("=", limit = 2)
                    key to value
                }.associate { it }

            val fileId = queryMap["file_id"]
            ensureNotNull(fileId) { raise(CannotRecognizeModError("file_id not present").nel()) }
            val modId = Path.of(modLink.path).last()
            ensureNotNull(modId) { raise(CannotRecognizeModError("modId not present").nel()) }

            val result = nexusService.getFiles(ModId(modId.toString().toInt()), FileId(fileId.toInt()), Category.MAIN)
                .toIor()
                .mapLeft { CannotRecognizeModError("mod_link not found").nel() }
                .bind()

            val next = nexusClient.getPreviewFiles(result.contentPreviewLink)
                .toIor()
                .mapLeft { CannotRecognizeModError("mod_link not found").nel() }
                .bind()

            println(modLink)


        }

        TODO()
    }
}

interface RecognizedMod

data class RecognizedModFiles(val files: Set<Path>) : RecognizedMod

data class RecognizedModFeature(
    val modDescriptor: ModDescriptor,
    val modFeatureDescriptor: ModFeatureDescriptor,
    val files: Set<Path>
) : RecognizedMod

data class CannotRecognizeModError(val reason: String)