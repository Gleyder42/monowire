package com.github.gleyder42.monowire.manager

import arrow.core.getOrElse
import com.github.gleyder42.monowire.common.*
import com.github.gleyder42.monowire.common.model.DisplayName
import com.github.gleyder42.monowire.common.model.ModId
import com.github.gleyder42.monowire.common.model.ModVersion
import com.github.gleyder42.monowire.persistence.sql.DatabaseControl
import com.github.gleyder42.monowire.persistence.sql.SqlDataSourceModule
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import org.koin.test.get
import java.nio.file.Path

@ExtendWith(SoftAssertionsExtension::class)
class ModInstanceLibraryTest : KoinTest {

    @MethodSource(TestData.TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldInstallModFeature(namespace: Path, src: Path, paths: List<Path>, softly: SoftAssertions) = runTest {
        // Arrange
        val gamePath = namespace.resolve("game")

        startKoin {
            modules(
                TestModule().module,
                SqlDataSourceModule().module,
                SqlDataSourceModule.sqliteModule,
                module {
                    single<String>(SqlDataSourceModule.DB_PATH_KEY) { namespace.resolve("database").toString() }
                    single<Path>(named("gameDirectory")) { gamePath }
                }
            )
        }
        get<DatabaseControl>().createSchema()

        val mod = ModFeatureImporter.importModFeatureAsMod(
            src,
            ModId(10001),
            ModVersion("1.0.0"),
            DisplayName("Test Mod")
        )

        val catalogue = ModCatalogue()
        catalogue.addMod(mod)
        val library = ModInstanceLibrary()

        // Act
        val installResult = library.install(mod.features.first())

        installResult extractRight {
            // Assert that files are copied to the game directory
            softly.assertThat(gamePath.listFilesRecursivelyOrEmpty().map { gamePath.relativize(it) })
                .containsExactlyInAnyOrderElementsOf(paths)

            // Assert that files are still in the mod catalogue
            softly.assertThat(src.listFilesRecursivelyOrEmpty().map { src.relativize(it) })
                .containsExactlyInAnyOrderElementsOf(paths)
        }
    }

    @MethodSource(TestData.TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldUninstallModFeature(namespace: Path, src: Path, paths: List<Path>, softly: SoftAssertions) = runTest {
        // Arrange
        val gamePath = dir(namespace resolve "game") {
            dir("otherMod") {
                file("mod.xl")
                file("mod2.xl")
            }
        }

        startKoin {
            modules(
                TestModule().module,
                SqlDataSourceModule().module,
                SqlDataSourceModule.sqliteModule,
                module {
                    single<String>(SqlDataSourceModule.DB_PATH_KEY) { namespace.resolve("database").toString() }
                    single<Path>(named("gameDirectory")) { gamePath }
                }
            )
        }
        get<DatabaseControl>().createSchema()

        val mod = ModFeatureImporter.importModFeatureAsMod(
            src,
            ModId(10001),
            ModVersion("1.0.0"),
            DisplayName("Test Mod")
        )

        val catalogue = ModCatalogue()
        catalogue.addMod(mod)
        val library = ModInstanceLibrary()
        val modFeature = mod.features.first()
        library.install(modFeature)

        // Act
        val uninstallResult = library.uninstall(modFeature.descriptor)

        uninstallResult extractRight { uninstalled ->
            // doesNotContainAnyElementsOf does not take empty lists
            if (paths.isNotEmpty()) {
                softly.assertThat(gamePath.listFilesRecursivelyOrEmpty().map { gamePath.relativize(it) })
                    .doesNotContainAnyElementsOf(paths)
            }

            // Assert that files are still in the mod catalogue
            softly.assertThat(src.listFilesRecursivelyOrEmpty().map { src.relativize(it) })
                .containsExactlyInAnyOrderElementsOf(paths)

            softly.assertThat(uninstalled).containsExactlyInAnyOrderElementsOf(paths.map { gamePath resolve it })
        }
    }

    @Test
    fun shouldNotThrowWhenClearModFiles(@TempDir namespace: Path, softly: SoftAssertions) = runTest {
        val paths = with(namespace.fileSystem) {
            listOf(
                getPath("archive", "pc", "mod", "mod.archive"),
                getPath("archive", "pc", "mod", "mod.xl"),
                getPath("r6", "tweaks", "archive_xl", "mod.yaml")
            )
        }

        val src = dir(namespace resolve "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("archive_xl") {
                file("mod.yaml")
            }
        }


        val gamePath = dir(namespace resolve "game") {
            dir("archive").dir("pc").dir("mod") {
                file("otherMod.archive")
                file("otherMod.xl")
            }
        }
        val otherGamePaths = with(namespace.fileSystem) {
            listOf(
                getPath("archive", "pc", "mod", "otherMod.archive"),
                getPath("archive", "pc", "mod", "otherMod.xl")
            )
        }

        startKoin {
            modules(
                TestModule().module,
                SqlDataSourceModule().module,
                SqlDataSourceModule.sqliteModule,
                module {
                    single<String>(SqlDataSourceModule.DB_PATH_KEY) { namespace.resolve("database").toString() }
                    single<Path>(named("gameDirectory")) { gamePath }
                }
            )
        }
        get<DatabaseControl>().createSchema()

        val mod = ModFeatureImporter.importModFeatureAsMod(
            src,
            ModId(10001),
            ModVersion("1.0.0"),
            DisplayName("Test Mod")
        )

        val catalogue = ModCatalogue()
        catalogue.addMod(mod)
        val library = ModInstanceLibrary()
        val modFeature = mod.features.first()
        library.install(modFeature).getOrElse { fail(it.toString()) }

        // Act
        val uninstallResult = library.uninstall(modFeature.descriptor)

        uninstallResult extractRight { uninstalled ->
            softly.assertThat(gamePath.listFilesRecursivelyOrEmpty())
                .containsExactlyInAnyOrderElementsOf(otherGamePaths.map { gamePath resolve it })

            softly.assertThat(uninstalled)
                .containsExactlyInAnyOrderElementsOf(paths.map { gamePath resolve it })
        }
    }

    @AfterEach
    fun teardown() {
        get<DatabaseControl>().close()
        stopKoin()
    }
}