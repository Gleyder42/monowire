package com.github.gleyder42.monowire.manager

import arrow.core.*
import com.github.gleyder42.monowire.common.*
import com.github.gleyder42.monowire.common.model.*
import com.github.gleyder42.monowire.persistence.sql.DatabaseControl
import com.github.gleyder42.monowire.persistence.sql.SqlDataSourceModule
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import org.koin.test.get
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileSystemException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

private const val GAME_DIRECTORY = "gameDirectory"

private const val TEMPORARY_DIRECTORY = "temporary"

@ExtendWith(RepresentationExtension::class)
@ExtendWith(SoftAssertionsExtension::class)
class ModInstanceLibraryTest : KoinTest {

    @MethodSource(TestData.TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldInstallModFeature(namespace: Path, src: Path, paths: List<Path>, softly: SoftAssertions) = runTest {
        // Arrange
        val (gamePath, _, modLibrary, mod) = modSetup(namespace, src)

        // Act
        val installResult = modLibrary.install(mod.features.first())
        assertRight(installResult)

        // Assert that files are copied to the game directory
        softly.assertThat(gamePath.listFilesRecursivelyOrEmpty().map { gamePath.relativize(it) })
            .containsExactlyInAnyOrderElementsOf(paths)

        // Assert that files are still in the mod catalogue
        softly.assertThat(src.listFilesRecursivelyOrEmpty().map { src.relativize(it) })
            .containsExactlyInAnyOrderElementsOf(paths)
    }

    @Test
    fun shouldReturnErrorWhenTryingToOverwriteSomeFiles(@TempDir namespace: Path, softly: SoftAssertions) = runTest {
        // Arrange
        val gamePath = dir(namespace.resolve("game")) {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }
        }

        val src = dir(namespace resolve "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("mod") {
                file("mod.yaml")
            }
        }

        val (_, _, modLibrary, mod) = modSetup(namespace, src, gamePath = gamePath)

        val result = modLibrary.install(mod.features.first())
        assertLeft(result)

        assertThat(result.value).isInstanceOf(ModInstallError.CannotCopyFiles::class.java)
        val error = result.value as ModInstallError.CannotCopyFiles

        with(namespace.fileSystem) {
            val expected = listOf(
                getPath("archive", "pc", "mod", "mod.archive"),
                getPath("archive", "pc", "mod", "mod.xl")
            )

            val files = error.files
                // The src and dest failed files are returned. We filter here only for the dest (aka game)
                .filter { it.failedFile.contains(getPath("game")) }
                // relativize so it is comparable with expected
                .map { gamePath.relativize(it.failedFile) }
            softly.assertThat(files).containsExactlyInAnyOrderElementsOf(expected)
            // this ensures that the mod was not installed
            softly.assertThat(gamePath.listFilesRecursivelyOrEmpty().map { gamePath.relativize(it) }).doesNotContain(
                getPath("r6", "tweaks", "mod", "mod.yaml")
            )
        }
    }

    @Test
    fun shouldReturnErrorWhenTryingToOverwriteAllFiles(@TempDir namespace: Path) = runTest {
        // Arrange
        val gamePath = dir(namespace.resolve("game")) {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("mod") {
                file("mod.yaml")
            }
        }

        val src = dir(namespace resolve "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("mod") {
                file("mod.yaml")
            }
        }

        val (_, _, modLibrary, mod) = modSetup(namespace, src, gamePath = gamePath)

        val result = modLibrary.install(mod.features.first())
        assertLeft(result)

        assertThat(result.value).isInstanceOf(ModInstallError.CannotCopyFiles::class.java)
        val error = result.value as ModInstallError.CannotCopyFiles

        with(namespace.fileSystem) {
            val expected = listOf(
                getPath("archive", "pc", "mod", "mod.archive"),
                getPath("archive", "pc", "mod", "mod.xl"),
                getPath("r6", "tweaks", "mod", "mod.yaml")
            )

            val files = error.files
                // The src and dest failed files are returned. We filter here only for the dest (aka game)
                .filter { it.failedFile.contains(getPath("game")) }
                // relativize so it is comparable with expected
                .map { gamePath.relativize(it.failedFile) }
            assertThat(files).containsExactlyInAnyOrderElementsOf(expected)
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

        val (_, _, modLibrary, mod) = modSetup(namespace, src, gamePath = gamePath)

        val modFeature = mod.features.first()
        modLibrary.install(modFeature)

        // Act
        val uninstallResult = modLibrary.uninstall(modFeature.descriptor)

        assertRight(uninstallResult)

        // doesNotContainAnyElementsOf does not take empty lists
        if (paths.isNotEmpty()) {
            softly.assertThat(gamePath.listFilesRecursivelyOrEmpty().map { gamePath.relativize(it) })
                .doesNotContainAnyElementsOf(paths)
        }

        // Assert that files are still in the mod catalogue
        softly.assertThat(src.listFilesRecursivelyOrEmpty().map { src.relativize(it) })
            .containsExactlyInAnyOrderElementsOf(paths)

        softly.assertThat((uninstallResult as Ior.Right).value)
            .containsExactlyInAnyOrderElementsOf(paths.map { gamePath resolve it })
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

        val (_, _, _, mod) = modSetup(namespace, src, gamePath = gamePath)

        val library = ModInstanceLibrary()
        val modFeature = mod.features.first()
        library.install(modFeature).getOrElse { fail(it.toString()) }

        // Act
        val uninstallResult = library.uninstall(modFeature.descriptor)

        assertRight(uninstallResult)

        softly.assertThat(gamePath.listFilesRecursivelyOrEmpty())
            .containsExactlyInAnyOrderElementsOf(otherGamePaths.map { gamePath resolve it })

        softly.assertThat((uninstallResult as Ior.Right).value)
            .containsExactlyInAnyOrderElementsOf(paths.map { gamePath resolve it })
    }

    // false positive
    @Suppress("JUnitMalformedDeclaration")
    @MethodSource("uninstallTestData")
    @ParameterizedTest
    fun shouldUninstall(
        namespace: Path,
        modFeaturePath: Path,
        scopedPathBuilder: ScopedPathBuilder,
        lockFiles: () -> List<InputStream>,
        uninstallResult: Ior<ModUninstallError, ModFiles>,
        softly: SoftAssertions
    ) = runTest {
        // Arrange
        val gameDirectory = namespace resolve GAME_DIRECTORY

        val (_, _, modLibrary, mod) = modSetup(namespace, modFeaturePath, gamePath = gameDirectory)

        val modFeature = mod.features.first()

        assertRight(modLibrary.install(modFeature))

        val openedFiles = lockFiles()
        try {
            // Act
            val result = modLibrary.uninstall(modFeature.descriptor)

            // Assert
            assertThat(result.mapLeft { it.relativizeTo(gameDirectory) }.map { it.relativizeTo(gameDirectory) })
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(FileSystemException::class.java)
                .isEqualTo(uninstallResult)
        } finally {
            openedFiles.forEach { it.close() }
        }
    }

    @Test
    fun shouldReturnCannotCreateTemporaryDirectory(@TempDir namespace: Path, softly: SoftAssertions) = runTest {
        // Arrange
        val gameDirectory = namespace resolve GAME_DIRECTORY

        val modFeaturePath = dir(namespace resolve "src") {
            file("modFile.file")
        }

        val (_, _, modLibrary, mod) = modSetup(namespace, modFeaturePath, gamePath = gameDirectory)

        val modFeature = mod.features.first()

        assertRight(modLibrary.install(modFeature))

        // Open a FileInputStream, so the file cannot be copied and throws an IOException
        FileInputStream(namespace.resolve(TEMPORARY_DIRECTORY).createFile().toFile()).use { _ ->
            // Act
            val result = modLibrary.uninstall(modFeature.descriptor)

            assertLeft(result)

            // Assert
            softly.assertThat(result.value).isInstanceOf(ModUninstallError.CannotCreateTemporaryDirectory::class.java)
        }
    }

    @Test
    fun shouldReturnErrorWhenUninstallingModThatDoesNotExist(@TempDir namespace: Path, softly: SoftAssertions) =
        runTest {
            // Arrange
            val gameDirectory = namespace resolve GAME_DIRECTORY
            val modFeaturePath = dir(namespace resolve "src") {
                file("modFile.file")
            }

            val (_, _, modLibrary, mod) = modSetup(namespace, modFeaturePath, gamePath = gameDirectory)

            val modFeature = mod.features.first()
            val uninstall = modLibrary.uninstall(modFeature.descriptor)
            assertLeft(uninstall)

            softly.assertThat(uninstall.value).isInstanceOf(ModUninstallError.ModNotInstalled::class.java)

            val error = uninstall.value as ModUninstallError.ModNotInstalled
            softly.assertThat(error.descriptor).isEqualTo(modFeature.descriptor)
        }

    @Test
    fun shouldReturnErrorWhenUninstallingModWithoutFilesThatDoesNotExist(
        @TempDir namespace: Path, softly: SoftAssertions
    ) = runTest {
        // Arrange
        val gameDirectory = namespace resolve GAME_DIRECTORY
        val modFeaturePath = dir(namespace resolve "src") { }
        val (_, _, modLibrary, mod) = modSetup(namespace, modFeaturePath, gamePath = gameDirectory)

        val modFeature = mod.features.first()
        modLibrary.install(modFeature)

        // Act
        val result = modLibrary.uninstall(modFeature.descriptor)
        assertBoth(result)

        // Assert
        softly.assertThat(result.leftValue).isInstanceOf(ModUninstallError.ModHasNoFiles::class.java)

        val error = result.leftValue as ModUninstallError.ModHasNoFiles
        softly.assertThat(error.descriptor).isEqualTo(modFeature.descriptor)

        softly.assertThat(result.rightValue).isEmpty()
    }

    @Disabled
    @Test
    fun shouldReturnErrorWhenCannotDeleteTemporaryDirectory(
        @TempDir namespace: Path, softly: SoftAssertions
    ) = runTest {
        // Arrange
        val srcDir = dir(namespace resolve "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("mod") {
                file("mod.yaml")
            }
        }

        val (_, _, modLibrary, mod) = modSetup(namespace, srcDir)
        val feature = mod.features.first()
        assertRight(modLibrary.install(feature))

        val resolve = namespace.resolve(TEMPORARY_DIRECTORY)
        resolve.createDirectories()

        // Act
        val result = modLibrary.uninstall(feature.descriptor)

        // Assert
        assertBoth(result)
        assertThat(result.leftValue).isInstanceOf(ModUninstallError.CannotDeleteTemporaryDirectory::class.java)
        val error = result.leftValue as ModUninstallError.CannotDeleteTemporaryDirectory
        val expected = with(namespace.fileSystem) {
            listOf(
                getPath("archive", "pc", "mod", "mod.archive"),
                getPath("archive", "pc", "mod", "mod.xl"),
            )
        }
        assertThat(error.cannotDelete.map { namespace.relativize(it.failedFile) }).containsExactlyInAnyOrderElementsOf(
            expected
        )
    }

    @AfterEach
    fun teardown() {
        get<DatabaseControl>().close()
        stopKoin()
    }

    data class ModSetupResult(
        val gamePath: Path,
        val modCatalogue: ModCatalogue,
        val modInstanceLibrary: ModInstanceLibrary,
        val mod: Mod
    )

    private suspend fun modSetup(namespace: Path, modSrc: Path, gamePath: Path? = null): ModSetupResult {
        @Suppress("NAME_SHADOWING")
        val gamePath = baseSetup(namespace, gamePath)
        val mod = ModFeatureImporter.importModFeatureAsMod(
            modSrc,
            ModId(10001),
            ModVersion("1.0.0"),
            DisplayName("Test Mod")
        )

        val modCatalogue = ModCatalogue()
        val modLibrary = ModInstanceLibrary()

        modCatalogue.addMod(mod)
        return ModSetupResult(gamePath, modCatalogue, modLibrary, mod)
    }

    private suspend fun baseSetup(namespace: Path, gamePath: Path? = null): Path {
        @Suppress("NAME_SHADOWING")
        val gamePath = gamePath ?: namespace.resolve("game")

        startKoin {
            modules(
                SqlDataSourceModule().module,
                SqlDataSourceModule.sqlDriverModule,
                module {
                    single<String>(SqlDataSourceModule.DB_PATH_KEY) { namespace.resolve("database").toString() }
                    single<Path>(named(GAME_DIRECTORY)) { gamePath }
                    single<Path>(named(TEMPORARY_DIRECTORY)) { namespace resolve TEMPORARY_DIRECTORY }
                }
            )
        }

        get<DatabaseControl>().createSchema()

        return gamePath
    }

    private fun Nel<FileError>.relativizeTo(path: Path): Nel<FileError> {
        return this.map { it.copy(failedFile = path.relativize(it.failedFile)) }
    }

    private fun NonEmptySet<FileError>.relativizeTo(path: Path): NonEmptySet<FileError> {
        return this.map { it.copy(failedFile = path.relativize(it.failedFile)) }.toNonEmptySetOrNull()!!
    }

    private fun ModUninstallError.relativizeTo(path: Path): ModUninstallError {
        return when (this) {
            is ModUninstallError.CannotCreateTemporaryDirectory -> copy(dir = dir.relativizeTo(path))
            is ModUninstallError.CannotDeleteTemporaryDirectory -> copy(cannotDelete = cannotDelete.relativizeTo(path))
            is ModUninstallError.CannotMoveFiles -> copy(files = files.relativizeTo(path))
            is ModUninstallError.CannotRecoverFiles -> copy(unrecoverableFiles = unrecoverableFiles.relativizeTo(path))
            is ModUninstallError.ModHasNoFiles -> this
            is ModUninstallError.ModNotInstalled -> this
        }
    }

    companion object {

        @JvmStatic
        fun uninstallTestData(@TempDir workingDir: Path): Array<Arguments> {
            val fs = workingDir.fileSystem

            val arguments = { name: String,
                              dirBuilder: ScopedPathBuilderFunction,
                              lock: Set<Path>,
                              result: Ior<ModUninstallError, ModFiles> ->
                val namespace = workingDir resolve name

                val lockFiles = { lock.map { FileInputStream(namespace.resolve(it).toFile()) } }

                val (path, scopedBuilder) = scopedDir(namespace resolve fs.getPath("src"), dirBuilder)
                Arguments.of(namespace, path, scopedBuilder, lockFiles, result)
            }

            return arrayOf(
                arguments(
                    "validMod",
                    // mod src
                    {
                        dir("archive").dir("pc").dir("mod") {
                            file("mod.archive")
                            file("mod.xl")
                        }

                        dir("r6").dir("tweaks").dir("archive_xl") {
                            file("mod.yaml")
                        }
                    },
                    // locked files
                    emptySet(),
                    // expected result
                    with(fs) {
                        setOf(
                            getPath("archive", "pc", "mod", "mod.archive"),
                            getPath("archive", "pc", "mod", "mod.xl"),
                            getPath("r6", "tweaks", "archive_xl", "mod.yaml"),
                        ).rightIor()
                    }
                ),
                arguments(
                    // mod src
                    "allModFilesCannotBeMoved",
                    {
                        dir("archive").dir("pc").dir("mod") {
                            file("mod.archive")
                            file("mod.xl")
                        }

                        dir("r6").dir("tweaks").dir("archive_xl") {
                            file("mod.yaml")
                        }
                    },
                    // locked files
                    with(fs) {
                        setOf(
                            getPath(GAME_DIRECTORY, "archive", "pc", "mod", "mod.archive"),
                            getPath(GAME_DIRECTORY, "archive", "pc", "mod", "mod.xl"),
                            getPath(GAME_DIRECTORY, "r6", "tweaks", "archive_xl", "mod.yaml"),
                        )
                    },
                    // expected result
                    with(fs) {
                        val error = ModUninstallError.CannotMoveFiles(
                            nonEmptySetOf(
                                FileError(getPath("archive", "pc", "mod", "mod.archive"), IOException()),
                                FileError(getPath("archive", "pc", "mod", "mod.xl"), IOException()),
                                FileError(getPath("r6", "tweaks", "archive_xl", "mod.yaml"), IOException()),
                            )
                        )
                        (error to emptySet<Path>()).bothIor()
                    }
                ),
                arguments(
                    // mod src
                    "someModFilesCannotBeMoved",
                    {
                        dir("archive").dir("pc").dir("mod") {
                            file("mod.archive")
                            file("mod.xl")
                        }

                        dir("r6").dir("tweaks").dir("archive_xl") {
                            file("mod.yaml")
                        }
                    },
                    // locked files
                    with(fs) {
                        setOf(
                            getPath(GAME_DIRECTORY, "archive", "pc", "mod", "mod.archive"),
                            getPath(GAME_DIRECTORY, "archive", "pc", "mod", "mod.xl"),
                        )
                    },
                    // expected result
                    with(fs) {
                        val error = ModUninstallError.CannotMoveFiles(
                            nonEmptySetOf(
                                FileError(getPath("archive", "pc", "mod", "mod.archive"), IOException()),
                                FileError(getPath("archive", "pc", "mod", "mod.xl"), IOException()),
                            )
                        )
                        (error to emptySet<Path>()).bothIor()
                    }
                ),
            )
        }
    }
}