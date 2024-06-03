package com.github.gleyder42.monowire.common

import arrow.core.getOrElse
import com.github.gleyder42.monowire.common.TestData.TEST_DATA_METHOD_SOURCE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.io.path.*

// False positive reported by IntelliJ
// Although directoryStructureTestData() has arguments, the workingDir is injected by JUnit and does not need
// to be supplied by the programmer
@ExtendWith(SoftAssertionsExtension::class)
class TestFileHelper {

    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun testListFilesRecursively(root: Path, dir: Path, paths: List<Path>) {
        // Act
        val files = dir.listFilesRecursively().getOrElse { emptyList() }.map { dir.relativize(it) }

        // Assert
        assertThat(files).containsExactlyInAnyOrderElementsOf(paths)
    }

    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun testCopyDirectorySiblingsRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Arrange
        val dest = namespace `|` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.copyDirectorySiblingsRecursively(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `|` it }
        val expectedDest = paths.map { dest `|` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedDest)
    }

    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun testCopyDirectoryRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Arrange
        val dest = namespace `|` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.copyDirectoryRecursivelyTo(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `|` it }
        val expectedDest = paths.map { dest `|` TestData.SRC_NAME `|` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedDest)
    }

    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun testMoveDirectorySiblingsRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Arrange
        val dest = namespace `|` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.moveDirectorySiblingsRecursively(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `|` it }
        val expectedDest = paths.map { dest `|` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir).isEmptyDirectory()
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedDest)
    }


    @MethodSource(TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun testMoveDirectoryRecursively(namespace: Path, srcDir: Path, paths: List<Path>, softly: SoftAssertions) {
        // Arrange
        val dest = namespace `|` "dest"
        dest.createDirectories()

        // Act
        val copyResult = srcDir.moveDirectoryRecursively(dest).getOrElse { fail(it.toString()) }

        // Assert
        val expectedSrc = paths.map { srcDir `|` it }
        val expectedDest = paths.map { dest `|` TestData.SRC_NAME `|` it }
        softly.assertThat(copyResult.fromSrc).containsExactlyInAnyOrderElementsOf(expectedSrc)
        softly.assertThat(copyResult.toDest).containsExactlyInAnyOrderElementsOf(expectedDest)
        softly.assertThat(srcDir).doesNotExist()
        softly.assertThat(dest.listFilesRecursively().getOrElse { emptyList() }).containsExactlyInAnyOrderElementsOf(expectedDest)
    }
}