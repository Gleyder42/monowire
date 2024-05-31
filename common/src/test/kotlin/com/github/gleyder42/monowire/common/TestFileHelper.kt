package com.github.gleyder42.monowire.common

import arrow.core.getOrElse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.io.path.*

// False positive reported by IntelliJ
// Although directoryStructureTestData() has arguments, the workingDir is injected by JUnit and does not need
// to be supplied by the programmer
@Suppress("JUnitMalformedDeclaration")
@ExtendWith(SoftAssertionsExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestFileHelper {

    private val srcName = "src"

    private fun directoryStructureTestData(@TempDir workingDir: Path): Array<Arguments> {
        val fs = workingDir.fileSystem

        val arguments = { namespace: Path, dirBuilder: PathBuilderFunction, expectedPaths: List<Path> ->
            // We are only interested in the namespace path prepended with the working directory, so we have
            // an absolute path.
            @Suppress("NAME_SHADOWING")
            val namespace = workingDir `⫽` namespace

            Arguments.of(namespace, dir(namespace `⫽` fs.getPath(srcName), dirBuilder), expectedPaths)
        }

        return arrayOf(
            arguments(
                fs.getPath("emptyMod"),
                { },
                listOf()
            ),
            arguments(
                fs.getPath("onlyOneFile"),
                {
                    file("hello.txt")
                },
                with(fs) {
                    listOf(getPath("hello.txt"))
                }
            ),
            arguments(
                fs.getPath("emptySubDirectories"),
                {
                    dir("a")
                    dir("b")
                    dir("c")
                },
                listOf()
            ),
            arguments(
                fs.getPath("simpleMod"),
                {
                    dir("archive").dir("pc").dir("mod") {
                        file("mod.archive")
                        file("mod.xl")
                    }

                    dir("r6").dir("tweaks").dir("archive_xl") {
                        file("mod.yaml")
                    }
                },
                with(fs) {
                    listOf(
                        getPath("archive", "pc", "mod", "mod.archive"),
                        getPath("archive", "pc", "mod", "mod.xl"),
                        getPath("r6", "tweaks", "archive_xl", "mod.yaml"),
                    )
                }
            ),
            arguments(
                fs.getPath("redMod"),
                {
                    dir("red4ext") {
                        dir("logs") {
                            file("mod_settings.1.log")
                            file("mod_settings.2.log")
                            file("mod_settings.3.log")
                        }

                        dir("plugins") {
                            dir("ArchiveXL") {
                                file("ArchiveXL.dll")
                            }
                            dir("Codeware") {
                                file("Codeware.dll")
                            }
                        }

                        file("config.ini")
                        file("LICENSE.txt")
                    }
                },
                with(fs) {
                    listOf(
                        getPath("red4ext", "logs", "mod_settings.1.log"),
                        getPath("red4ext", "logs", "mod_settings.2.log"),
                        getPath("red4ext", "logs", "mod_settings.3.log"),
                        getPath("red4ext", "plugins", "ArchiveXL", "ArchiveXL.dll"),
                        getPath("red4ext", "plugins", "Codeware", "Codeware.dll"),
                        getPath("red4ext", "config.ini"),
                        getPath("red4ext", "LICENSE.txt"),
                    )
                }
            )
        )
    }

    @MethodSource("directoryStructureTestData")
    @ParameterizedTest
    fun testListFilesRecursively(root: Path, dir: Path, paths: List<Path>) {
        // Act
        val files = dir.listFilesRecursively().getOrElse { emptyList() }.map { dir.relativize(it) }

        // Assert
        assertThat(files).containsExactlyInAnyOrderElementsOf(paths)
    }

    @MethodSource("directoryStructureTestData")
    @ParameterizedTest
    fun testCopyDirectorySiblingsRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Arrange
        val dest = namespace `⫽` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.copyDirectorySiblingsRecursively(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `⫽` it }
        val expectedDest = paths.map { dest `⫽` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedDest)
    }

    @MethodSource("directoryStructureTestData")
    @ParameterizedTest
    fun testCopyDirectoryRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Arrange
        val dest = namespace `⫽` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.copyDirectoryRecursively(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `⫽` it }
        val expectedDest = paths.map { dest `⫽` srcName `⫽` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedDest)
    }

    @MethodSource("directoryStructureTestData")
    @ParameterizedTest
    fun testMoveDirectorySiblingsRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Arrange
        val dest = namespace `⫽` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.moveDirectorySiblingsRecursively(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `⫽` it }
        val expectedDest = paths.map { dest `⫽` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir).isEmptyDirectory()
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedDest)
    }

    @MethodSource("directoryStructureTestData")
    @ParameterizedTest
    fun testMoveDirectoryRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Arrange
        val dest = namespace `⫽` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.moveDirectoryRecursively(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `⫽` it }
        val expectedDest = paths.map { dest `⫽` srcName `⫽` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir).doesNotExist()
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedDest)
    }
}