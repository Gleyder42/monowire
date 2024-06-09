package com.github.gleyder42.monowire.manager

import arrow.core.*
import arrow.core.raise.either
import com.github.gleyder42.monowire.common.*
import com.github.gleyder42.monowire.common.model.ModFeature
import com.github.gleyder42.monowire.common.model.ModFeatureDescriptor
import com.github.gleyder42.monowire.common.model.ModFiles
import com.github.gleyder42.monowire.common.model.ModInstance
import com.github.gleyder42.monowire.persistence.datasource.ModInstanceLibraryDataSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.*
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.moveTo

class ModInstanceLibrary : KoinComponent {

    private val dataSource by inject<ModInstanceLibraryDataSource>()
    private val gameDirectory by inject<Path>(named("gameDirectory"))
    private val temporary by inject<Path>(named("temporary"))

    suspend fun install(feature: ModFeature): Ior<ModInstallError, ModFiles> {
        // Copy the files into the game directory
        val copyResult = when (val modFiles = feature.modPath.copyDirectorySiblingsRecursivelyTo(gameDirectory)) {
            is Ior.Left -> return ModInstallError.CannotCopyFiles(modFiles.value).leftIor()
            is Ior.Both -> {
                val wereCopied = modFiles.rightValue.toDest.toMutableSet()

                val cannotBeDeleted = mutableSetOf<FileError>()
                for (path in modFiles.rightValue.toDest) {
                    when (val result = path.safeDelete()) {
                        is Either.Left -> cannotBeDeleted.add(result.value)
                        is Either.Right -> wereCopied.remove(result.value)
                    }
                }

                val nelCannotBeDeleted = cannotBeDeleted.toNonEmptyListOrNull()
                if (nelCannotBeDeleted != null) {
                    val error = ModInstallError.CannotCleanUpFiles(nelCannotBeDeleted)
                    return (error to wereCopied).bothIor()
                }

                return ModInstallError.CannotCopyFiles(modFiles.leftValue).leftIor()
            }
            is Ior.Right -> modFiles.value
        }

        // Update the changes
        val modInstance = ModInstance(
            copyResult.toDest,
            feature.version,
            feature.descriptor
        )

        // Persist the changes
        dataSource.addModInstance(modInstance)
        return copyResult.toDest.rightIor()
    }

    suspend fun uninstall(descriptor: ModFeatureDescriptor): Ior<ModUninstallError, ModFiles> {
        val modInstance = dataSource.getModInstance(descriptor)
            ?: return ModUninstallError.ModNotInstalled(descriptor).leftIor()

        if (modInstance.installedFiles.isEmpty()) {
            // Uninstalling a mod with no files is not an error, but also does not have a purpose
            return (ModUninstallError.ModHasNoFiles(descriptor) to emptySet<Path>()).bothIor()
        }

        val temporaryDirectory = temporary resolve UUID.randomUUID().toString()

        try {
            temporaryDirectory.createDirectories()
        } catch (exception: IOException) {
            // If we cannot create a temporary directory, we cannot safely delete the mod
            return ModUninstallError.CannotCreateTemporaryDirectory(temporaryDirectory, exception).leftIor()
        }

        val failedFiles = mutableSetOf<FileError>()
        val movedFiles = mutableListOf<FileMoveResult>()
        for (installedFile in modInstance.installedFiles) {
            try {
                val newPath = temporaryDirectory.resolve(gameDirectory.relativize(installedFile))
                newPath.createParentDirectories()
                installedFile.moveTo(newPath)
                movedFiles.add(FileMoveResult(installedFile, newPath))
            } catch (exception: IOException) {
                failedFiles.add(FileError(installedFile, exception))
            }
        }

        val nelFailedFiles = failedFiles.toNonEmptySetOrNull()
        when {
            // All files could be moved (aka no failed files)
            nelFailedFiles == null -> {
                when (val result = temporaryDirectory.safeDeleteRecursively(deleteSource = true)) {
                    is Ior.Both -> {
                        val error = ModUninstallError.CannotDeleteTemporaryDirectory(result.leftValue)
                        return (error to result.rightValue).bothIor()
                    }
                    is Ior.Left -> return ModUninstallError.CannotDeleteTemporaryDirectory(result.value).leftIor()
                    is Ior.Right -> return movedFiles.map { it.src }.toSet().rightIor()
                }
            }
            // Some files could not be moved
            else -> {
                val failedRecoveryFiles = mutableListOf<FileError>()
                for (movedFile in movedFiles) {
                    try {
                        movedFile.src.createParentDirectories()
                        movedFile.dest.copyTo(movedFile.src)
                    } catch (exception: NoSuchFileException) {
                        failedRecoveryFiles.add(FileError(movedFile.dest, exception))
                    } catch (exception: FileAlreadyExistsException) {
                        failedRecoveryFiles.add(FileError(movedFile.src, exception))
                    } catch (exception: IOException) {
                        failedRecoveryFiles.add(FileError(movedFile.src, exception))
                    }
                }

                return when {
                    failedRecoveryFiles.isEmpty() -> {
                        val error = ModUninstallError.CannotMoveFiles(nelFailedFiles)
                        (error to emptySet<Path>()).bothIor()
                    }
                    else -> {
                        ModUninstallError.CannotRecoverFiles(failedRecoveryFiles.toNonEmptySetOrNull()!!).leftIor()
                    }
                }
            }
        }
    }
}

data class FileMoveResult(val src: Path, val dest: Path)

sealed interface ModInstallError {

    data class CannotCopyFiles(val error: NonEmptyList<FileError>) : ModInstallError

    data class CannotCleanUpFiles(val error: NonEmptyList<FileError>) : ModInstallError
}

sealed interface ModUninstallError {

    data class ModNotInstalled(val descriptor: ModFeatureDescriptor) : ModUninstallError

    data class CannotCreateTemporaryDirectory(val dir: Path, val exception: IOException) : ModUninstallError

    data class CannotMoveFiles(val files: NonEmptySet<FileError>) : ModUninstallError {

        override fun toString() = "CannotMoveFiles(files=${files.joinToString("\n")})"
    }

    data class CannotRecoverFiles(val unrecoverableFiles: NonEmptySet<FileError>) : ModUninstallError

    data class CannotDeleteTemporaryDirectory(val cannotDelete: Nel<FileError>) : ModUninstallError

    data class ModHasNoFiles(val descriptor: ModFeatureDescriptor) : ModUninstallError
}
