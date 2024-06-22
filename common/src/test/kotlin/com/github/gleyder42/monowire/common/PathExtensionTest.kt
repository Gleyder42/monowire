package com.github.gleyder42.monowire.common

import arrow.core.Ior
import arrow.core.getOrElse
import com.github.gleyder42.monowire.common.TestData.TEST_DATA_METHOD_SOURCE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.koin.core.component.inject
import org.koin.test.KoinTest
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.createDirectories

@ExtendWith(SoftAssertionsExtension::class)
class PathExtensionTest {

    private val pathHelper = PathHelper()

    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldListFilesRecursively(root: Path, dir: Path, paths: List<Path>) {
        // Act
        val files = dir.listFilesRecursively().getOrElse { emptyList() }.map { dir.relativize(it) }

        // Assert
        assertThat(files).containsExactlyInAnyOrderElementsOf(paths)
    }

    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldCopyDirectorySiblingsRecursively(
        namespace: Path,
        srcDir: Path,
        paths: List<Path>,
        softly: SoftAssertions
    ) {
        // Arrange
        val dest = namespace `⫽` "dest"
        dest.createDirectories()

        // Act
        val copyResult = with(pathHelper) {
            srcDir.copyDirectorySiblingsRecursivelyTo(dest).getOrElse { fail(it.toString()) }
        }

        // Assert
        val expectedSrc = paths.map { srcDir `⫽` it }
        val expectedDest = paths.map { dest `⫽` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir.listFilesRecursively().getOrElse { emptyList() })
            .containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() })
            .containsExactlyInAnyOrderElementsOf(expectedDest)
    }

    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldCopyDirectoryRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Arrange
        val dest = namespace `⫽` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.copyDirectoryRecursivelyTo(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `⫽` it }
        val expectedDest = paths.map { dest `⫽` TestData.SRC_NAME `⫽` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir.listFilesRecursively().getOrElse { emptyList() })
            .containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() })
            .containsExactlyInAnyOrderElementsOf(expectedDest)
    }

    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldMoveDirectorySiblingsRecursively(
        namespace: Path,
        srcDir: Path,
        paths: List<Path>,
        softly: SoftAssertions
    ) {
        // Arrange
        val dest = namespace `⫽` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.moveDirectorySiblingsRecursivelyTo(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `⫽` it }
        val expectedDest = paths.map { dest `⫽` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir).isEmptyDirectory()
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() })
            .containsExactlyInAnyOrderElementsOf(expectedDest)
    }

    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldMoveDirectoryRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Arrange
        val dest = namespace `⫽` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.moveDirectoryRecursivelyTo(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `⫽` it }
        val expectedDest = paths.map { dest `⫽` TestData.SRC_NAME `⫽` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir).doesNotExist()
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() })
            .containsExactlyInAnyOrderElementsOf(expectedDest)
    }

    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun shouldSafeDeleteDirectoryRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Act
        val deletedPaths =
            PathHelper().safeDeleteRecursively(srcDir, deleteSource = true).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `⫽` it }
        softly.assertThat(deletedPaths).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(srcDir).doesNotExist()
    }

    @Test
    fun shouldThrowExceptionWhenListingNotExistentFile(@TempDir namespace: Path) {
        // Arrange
        val path = namespace `⫽` "nonExistentFile.txt"

        // Act
        val result = path.listFilesRecursively()

        // Assert
        assertLeft(result)

        assertThat(result.value).isInstanceOf(NoSuchFileException::class.java)
    }

    @Test
    fun shouldFileWhenMovingLockedFile(@TempDir namespace: Path) {
        // Arrange
        val name = "openFile.txt"
        val (path, builder) = scopedDir(namespace `⫽` "src") {
            file(name, lockFile = true)
        }

        val dest = dir(namespace `⫽` "dest")

        builder.use {
            // Act
            val result = path.moveDirectorySiblingsRecursivelyTo(dest)

            // Assert
            assertLeft(result)

            assertThat(result.value).allSatisfy { it.failedFile.endsWith(name) }
        }
    }

    @Test
    fun shouldFileNotFileWhenCopyingLockedFile(@TempDir namespace: Path, softly: SoftAssertions) {
        // Arrange
        val name = "openFile.txt"
        val (path, builder) = scopedDir(namespace `⫽` "src") {
            file(name, lockFile = true)
        }

        builder.use {
            val dest = dir(namespace `⫽` "dest")

            // Act
            val result = with(pathHelper) { path.copyDirectorySiblingsRecursivelyTo(dest) }

            // Assert
            assertRight(result)

            softly.assertThat((result as Ior.Right).value.fromSrc).allSatisfy { it.endsWith(name) }
            softly.assertThat(result.value.toDest).allSatisfy { it.endsWith(name) }
        }
    }

    @Test
    fun shouldPartiallyFailWhenMovingLockedFiles(@TempDir namespace: Path, softly: SoftAssertions) {
        // Arrange
        val lockedFileName = "openFile.txt"
        val notLockedFile = "notLockedFile.txt"
        val (path, builder) = scopedDir(namespace `⫽` "src") {
            file(lockedFileName, lockFile = true)
            file(notLockedFile, lockFile = false)
        }

        builder.use {
            val dest = dir(namespace `⫽` "dest")

            // Act
            val result = path.moveDirectorySiblingsRecursivelyTo(dest)

            // Assert
            assertBoth(result)

            softly.assertThat(result.leftValue).allSatisfy { it.failedFile.endsWith(lockedFileName) }
            softly.assertThat(result.rightValue.fromSrc).allSatisfy { it.endsWith(notLockedFile) }
            softly.assertThat(result.rightValue.toDest).allSatisfy { it.endsWith(notLockedFile) }
        }
    }
}