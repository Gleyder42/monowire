package com.github.gleyder42.monowire.manager

import arrow.core.*
import com.github.gleyder42.monowire.common.*
import com.github.gleyder42.monowire.common.model.Mod
import com.github.gleyder42.monowire.common.model.ModFiles
import com.github.gleyder42.monowire.manager.KoinSetup.setupMod
import com.github.gleyder42.monowire.manager.NamedComponent.Key.GAME_DIRECTORY
import com.github.gleyder42.monowire.manager.NamedComponent.Key.TEMPORARY_DIRECTORY
import com.github.gleyder42.monowire.persistence.sql.DatabaseControl
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declareMock
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileSystemException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile


@ExtendWith(RepresentationExtension::class)
@ExtendWith(SoftAssertionsExtension::class)
class ModInstanceLibraryTest : KoinTest {

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz ->
        when  {
            clazz == PathHelper::class -> spyk(PathHelper())
            else -> mockkClass(clazz)
        }
    }

    @MethodSource(TestData.TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldInstallModFeature(namespace: Path, src: Path, paths: List<Path>, softly: SoftAssertions) = runTest {
        // Arrange
        val (gamePath, _, modLibrary, mod) = setupMod(namespace, src)

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

        val src = dir(namespace `⫽` "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("mod") {
                file("mod.yaml")
            }
        }

        val (_, _, modLibrary, mod) = setupMod(namespace, src, gamePath = gamePath)

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

        val src = dir(namespace `⫽` "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("mod") {
                file("mod.yaml")
            }
        }

        val (_, _, modLibrary, mod) = setupMod(namespace, src, gamePath = gamePath)

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
        val gamePath = dir(namespace `⫽` "game") {
            dir("otherMod") {
                file("mod.xl")
                file("mod2.xl")
            }
        }

        val (_, _, modLibrary, mod) = setupMod(namespace, src, gamePath = gamePath)

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
            .containsExactlyInAnyOrderElementsOf(paths.map { gamePath `⫽` it })
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

        val src = dir(namespace `⫽` "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("archive_xl") {
                file("mod.yaml")
            }
        }

        val gamePath = dir(namespace `⫽` "game") {
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

        val (_, _, _, mod) = setupMod(namespace, src, gamePath = gamePath)

        val library = ModInstanceLibrary()
        val modFeature = mod.features.first()
        library.install(modFeature).getOrElse { fail(it.toString()) }

        // Act
        val uninstallResult = library.uninstall(modFeature.descriptor)

        assertRight(uninstallResult)

        softly.assertThat(gamePath.listFilesRecursivelyOrEmpty())
            .containsExactlyInAnyOrderElementsOf(otherGamePaths.map { gamePath `⫽` it })

        softly.assertThat((uninstallResult as Ior.Right).value)
            .containsExactlyInAnyOrderElementsOf(paths.map { gamePath `⫽` it })
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
        val gameDirectory = namespace `⫽` GAME_DIRECTORY

        val (_, _, modLibrary, mod) = setupMod(namespace, modFeaturePath, gamePath = gameDirectory)

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
        val gameDirectory = namespace `⫽` GAME_DIRECTORY

        val modFeaturePath = dir(namespace `⫽` "src") {
            file("modFile.file")
        }

        val (_, _, modLibrary, mod) = setupMod(namespace, modFeaturePath, gamePath = gameDirectory)

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
            val gameDirectory = namespace `⫽` GAME_DIRECTORY
            val modFeaturePath = dir(namespace `⫽` "src") {
                file("modFile.file")
            }

            val (_, _, modLibrary, mod) = setupMod(namespace, modFeaturePath, gamePath = gameDirectory)

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
        val gameDirectory = namespace `⫽` GAME_DIRECTORY
        val modFeaturePath = dir(namespace `⫽` "src") { }
        val (_, _, modLibrary, mod) = setupMod(namespace, modFeaturePath, gamePath = gameDirectory)

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

    @Test
    fun `should return error when some files cannot be deleted from the temporary directory`(
        @TempDir namespace: Path, softly: SoftAssertions
    ) = runTest {
        // Arrange
        val srcDir = dir(namespace `⫽` "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("mod") {
                file("mod.yaml")
            }
        }

        val (_, _, modLibrary, mod) = setupMod(namespace, srcDir)

        declareMock<PathHelper> {
            val fs = namespace.fileSystem
            val both = Ior.Both(
                leftValue = nonEmptyListOf(
                    FileError(
                        fs.getPath("archive", "pc", "mod", "mod.archive"),
                        IOException()
                    ),
                    FileError(
                        fs.getPath("archive", "pc", "mod", "mod.xl"),
                        IOException()
                    )
                ),
                rightValue = setOf(fs.getPath("r6", "tweaks", "mod", "mod.yaml"))
            )

            every {
                safeDeleteRecursively(
                    match { path -> path.any { it == fs.getPath(TEMPORARY_DIRECTORY) } },
                    eq(true)
                )
            } returns both
        }

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

        with(namespace.fileSystem) {
            val expected = listOf(
                getPath("archive", "pc", "mod", "mod.archive"),
                getPath("archive", "pc", "mod", "mod.xl"),
            )

            assertThat(error.cannotDelete.map { it.failedFile })
                .containsExactlyInAnyOrderElementsOf(expected)
            assertThat(result.rightValue)
                .containsExactlyInAnyOrder(
                    getPath("r6", "tweaks", "mod", "mod.yaml")
                )
        }
    }

    @Test
    fun `should return error when all files cannot be deleted from the temporary directory`(
        @TempDir namespace: Path, softly: SoftAssertions
    ) = runTest {
        // Arrange
        val srcDir = dir(namespace `⫽` "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("mod") {
                file("mod.yaml")
            }
        }

        val (_, _, modLibrary, mod) = setupMod(namespace, srcDir)

        declareMock<PathHelper> {
            val fs = namespace.fileSystem
            val left = Ior.Left(
                nonEmptyListOf(
                    FileError(
                        fs.getPath("archive", "pc", "mod", "mod.archive"),
                        IOException()
                    ),
                    FileError(
                        fs.getPath("archive", "pc", "mod", "mod.xl"),
                        IOException()
                    ),
                    FileError(
                        fs.getPath("r6", "tweaks", "mod", "mod.yaml"),
                        IOException()
                    )
                )
            )

            every {
                safeDeleteRecursively(
                    match { path -> path.any { it == fs.getPath(TEMPORARY_DIRECTORY) } },
                    eq(true)
                )
            } returns left
        }

        val feature = mod.features.first()
        assertRight(modLibrary.install(feature))

        val resolve = namespace.resolve(TEMPORARY_DIRECTORY)
        resolve.createDirectories()

        // Act
        val result = modLibrary.uninstall(feature.descriptor)

        // Assert
        assertLeft(result)
        assertThat(result.value).isInstanceOf(ModUninstallError.CannotDeleteTemporaryDirectory::class.java)
        val error = result.value as ModUninstallError.CannotDeleteTemporaryDirectory

        with(namespace.fileSystem) {
            val expected = listOf(
                getPath("archive", "pc", "mod", "mod.archive"),
                getPath("archive", "pc", "mod", "mod.xl"),
                getPath("r6", "tweaks", "mod", "mod.yaml"),
            )

            assertThat(error.cannotDelete.map { it.failedFile })
                .containsExactlyInAnyOrderElementsOf(expected)
        }
    }

    @Test
    fun `should return error when mod files cannot be recovered from temporary directory`(
        @TempDir namespace: Path, softly: SoftAssertions
    ) = runTest {
        // Arrange

        val srcDir = dir(namespace `⫽` "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("mod") {
                file("mod.yaml")
            }
        }

        val (_, _, modLibrary, mod) = setupMod(namespace, srcDir)

        declareMock<PathHelper> {
            val fs = namespace.fileSystem
            val left = Ior.Left(
                nonEmptyListOf(
                    FileError(
                        fs.getPath("archive", "pc", "mod", "mod.archive"),
                        IOException()
                    ),
                    FileError(
                        fs.getPath("archive", "pc", "mod", "mod.xl"),
                        IOException()
                    ),
                    FileError(
                        fs.getPath("r6", "tweaks", "mod", "mod.yaml"),
                        IOException()
                    )
                )
            )

            every {
                safeDeleteRecursively(
                    match { path -> path.any { it == fs.getPath(TEMPORARY_DIRECTORY) } },
                    eq(true)
                )
            } returns left
        }

        val feature = mod.features.first()
        assertRight(modLibrary.install(feature))

        val resolve = namespace.resolve(TEMPORARY_DIRECTORY)
        resolve.createDirectories()

        // Act
        val result = modLibrary.uninstall(feature.descriptor)

        // Assert
        assertLeft(result)
        assertThat(result.value).isInstanceOf(ModUninstallError.CannotDeleteTemporaryDirectory::class.java)
        val error = result.value as ModUninstallError.CannotDeleteTemporaryDirectory

        with(namespace.fileSystem) {
            val expected = listOf(
                getPath("archive", "pc", "mod", "mod.archive"),
                getPath("archive", "pc", "mod", "mod.xl"),
                getPath("r6", "tweaks", "mod", "mod.yaml"),
            )

            assertThat(error.cannotDelete.map { it.failedFile })
                .containsExactlyInAnyOrderElementsOf(expected)
        }
    }

    @Test
    fun `should remove copied files if all files could not be copied`(
        @TempDir namespace: Path, softly: SoftAssertions
    ) = runTest {
        // Arrange
        val srcDir = dir(namespace `⫽` "src") {
            dir("archive").dir("pc").dir("mod") {
                file("mod.archive")
                file("mod.xl")
            }

            dir("r6").dir("tweaks").dir("mod") {
                file("mod.yaml")
            }
        }

        val (_, _, modLibrary, mod) = setupMod(namespace, srcDir)
        val fs = namespace.fileSystem
        val stagedErrors = nonEmptyListOf(
            FileError(
                fs.getPath("archive", "pc", "mod", "mod.archive"),
                IOException()
            ),
            FileError(
                fs.getPath("archive", "pc", "mod", "mod.xl"),
                IOException()
            ),
        )

        declareMock<PathHelper> {
            every {
                srcDir.copyDirectorySiblingsRecursivelyTo(any())
            } returns Ior.Both(
                leftValue = stagedErrors,
                rightValue = CopyResult.both(setOf(fs.getPath("r6", "tweaks", "mod", "mod.yaml")))
            )

            every {
                fs.getPath("r6", "tweaks", "mod", "mod.yaml").safeDelete()
            } returns FileError(
                fs.getPath("r6", "tweaks", "mod", "mod.yaml"),
                IOException()
            ).left()
        }

        val result = modLibrary.install(mod.features.first())

        // Assert
        assertLeft(result)

        softly.assertThat(result.value).isInstanceOf(ModInstallError.CannotCleanUpFiles::class.java)
        val error = result.value as ModInstallError.CannotCleanUpFiles
        softly.assertThat(error.errors.first().failedFile).isEqualTo(fs.getPath("r6", "tweaks", "mod", "mod.yaml"))
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
                val namespace = workingDir `⫽` name

                val lockFiles = { lock.map { FileInputStream(namespace.resolve(it).toFile()) } }

                val (path, scopedBuilder) = scopedDir(namespace `⫽` fs.getPath("src"), dirBuilder)
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