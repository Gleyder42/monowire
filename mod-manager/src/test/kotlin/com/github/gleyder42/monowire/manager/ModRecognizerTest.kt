package com.github.gleyder42.monowire.manager

import com.github.gleyder42.monowire.common.declare
import com.github.gleyder42.monowire.common.dir
import com.github.gleyder42.monowire.common.model.*
import com.github.gleyder42.monowire.common.tempPath
import com.github.gleyder42.monowire.common.`⫽`
import com.github.gleyder42.monowire.persistence.sql.DelightDataSourceModule
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.spec.style.Test
import io.kotest.datatest.withData
import io.kotest.koin.KoinExtension
import org.koin.core.qualifier.named
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.junit5.KoinTestExtension
import java.net.URI
import java.net.URL
import java.nio.file.Path

class ModRecognizerTest : ShouldSpec(), KoinTest {

    override fun extensions(): List<Extension> =
        listOf(KoinExtension(listOf(ModManagerModule.module, DelightDataSourceModule.module)))

    private val modRecognizer by inject<ModRecognizer>()

    data class TestData(
        val link: URI,
        val gameDirectory: Path,
        val recognizedMod: List<RecognizedMod>
    )

    init {
        xshould("recognize installed mods") {
            // Arrange
            val namespace = tempPath()
            val fs = namespace.fileSystem
            val gameDirectory = dir(namespace `⫽` NamedComponent.Key.GAME_DIRECTORY) {
                dir("archive").dir("pc").dir("mod") {
                    file("author_modName.archive")
                    file("author_modName.xl")
                }

                dir("r6").dir("tweaks").dir("author_modName") {
                    file("authorName.yaml")
                }
            }

            declare {
                single(named(NamedComponent.Key.GAME_DIRECTORY)) { gameDirectory }
            }

            val links = listOf(URL("https://www.nexusmods.com/cyberpunk2077/mods/10001?tab=files&file_id=80008"))

            // Act
            val result = modRecognizer.recognizeMod(links)

            // Assert
            val recognizedMods = listOf(
                RecognizedModFeature(
                    ModDescriptor(ModId(10001), ModVersion("1.0.0")),
                    ModFeatureDescriptor(ModFeatureKey("modNameFeature"), ModId(10001)),
                    setOf(
                        gameDirectory `⫽` "archive" `⫽` "pc" `⫽` "mod" `⫽` "author_modName.archive",
                        gameDirectory `⫽` "archive" `⫽` "pc" `⫽` "mod" `⫽` "author_modName.xl",
                        gameDirectory `⫽` "r6" `⫽` "tweaks" `⫽` "author_modName" `⫽` "authorName.yaml",
                    )
                )
            )

            result shouldBeRight recognizedMods
        }
    }
}