package com.github.gleyder42.monowire.manager

import com.github.gleyder42.monowire.common.*
import com.github.gleyder42.monowire.common.model.*
import com.github.gleyder42.monowire.manager.KoinSetup.setupBase
import com.github.gleyder42.monowire.manager.KoinSetup.setupDependencies
import com.github.gleyder42.monowire.persistence.sql.DatabaseControl
import com.github.gleyder42.monowire.persistence.sql.ModCatalogueError
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import java.nio.file.Path

const val MOD_CATALOGUE_DIR = "catalogueDir"

@ExtendWith(SoftAssertionsExtension::class)
class ModCatalogueTest : KoinTest {

    @MethodSource(TestData.TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldAddModIfNotExist(namespace: Path, src: Path, paths: List<Path>) = runTest {
        // Arrange
        setupBase(namespace)

        val mod = ModFeatureImporter.importModFeatureAsMod(
            src,
            ModId(10001),
            ModVersion("1.0.0"),
            DisplayName(namespace.fileName.toString())
        )

        val catalogue = ModCatalogue()

        // Act
        catalogue.addModIfNotExist(mod)

        // Assert
        val actualMod = catalogue.getMod(mod.descriptor)
        assertThat(actualMod).isEqualTo(mod)
    }

    @MethodSource(TestData.TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldCannotAddDuplicatedMod(namespace: Path, src: Path, paths: List<Path>) = runTest {
        // Arrange
        setupBase(namespace)

        val mod = ModFeatureImporter.importModFeatureAsMod(
            src,
            ModId(10001),
            ModVersion("1.0.0"),
            DisplayName("Test Mod")
        )

        val catalogue = ModCatalogue()
        catalogue.addMod(mod)

        // Assert
        val result = catalogue.addMod(mod)

        assertLeft(result)

        assertThat(result.value).isEqualTo(ModCatalogueError.DuplicateModError(mod.descriptor))
    }

    @Suppress("JUnitMalformedDeclaration")
    @MethodSource("testData")
    @ParameterizedTest
    fun test(
        namespace: Path,
        import: Path,
        expected: Mod,
        softly: SoftAssertions,
    ) = runTest {
        // Setup
        val (_, modCatalogue, _) = setupDependencies(namespace)

        val modCatalogueDir = dir(namespace resolve MOD_CATALOGUE_DIR)

        loadKoinModules(module {
            single(named(ModCatalogue.MOD_IMPORT_DIR_KEY)) { import }
            single(named(ModCatalogue.MOD_CATALOGUE_DIR_KEY)) { modCatalogueDir }
        })

        // Act
        val result = modCatalogue.loadFromImporteDir()
            .map { partialMods -> partialMods.map { it.toMod(expected.descriptor) } }

        // Assert
        assertRight(result)

        val mods: List<Mod> = result.value as List<Mod>

        softly.assertThat(mods.first().features).isEqualTo(expected.features)
        softly.assertThat(import).isEmptyDirectory()
    }

    @AfterEach
    fun teardown() {
        get<DatabaseControl>().close()
        stopKoin()
    }

    companion object {

        @JvmStatic
        private fun testData(@TempDir namespace: Path): Array<Arguments> {
            val import = "import"

            val argument = { modSrcBuilder: PathBuilderFunction, expected: Mod ->
                dir(namespace resolve import, modSrcBuilder)

                Arguments.of(namespace, namespace resolve import, expected)
            }

            return arrayOf(
                argument(
                    {
                        dir("modName") {
                            dir("modFeatureOne") {
                                file("1-feature.archive")
                                file("1-feature.xl")
                            }
                            dir("modFeatureTwo") {
                                file("2-feature.archive")
                                file("2-feature.xl")
                            }
                            dir("modFeatureThree") {
                                file("3-feature.archive")
                                file("3-feature.xl")
                            }
                        }
                    },
                    Mod(
                        ModId(10002),
                        ModVersion("1.0.0"),
                        DisplayName("modName"),
                        features = setOf(
                            ModFeature(
                                namespace resolve MOD_CATALOGUE_DIR resolve "modFeatureOne",
                                ModId(10002),
                                ModVersion("1.0.0"),
                                ModFeatureKey("modFeatureOne")
                            ),
                            ModFeature(
                                namespace resolve MOD_CATALOGUE_DIR resolve "modFeatureTwo",
                                ModId(10002),
                                ModVersion("1.0.0"),
                                ModFeatureKey("modFeatureTwo")
                            ),
                            ModFeature(
                                namespace resolve MOD_CATALOGUE_DIR resolve "modFeatureThree",
                                ModId(10002),
                                ModVersion("1.0.0"),
                                ModFeatureKey("modFeatureThree")
                            )
                        )
                    ),
                )
            )
        }

    }
}