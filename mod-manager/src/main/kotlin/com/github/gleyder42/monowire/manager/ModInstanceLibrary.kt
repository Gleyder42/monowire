package com.github.gleyder42.monowire.manager

import arrow.core.*
import com.github.gleyder42.monowire.common.*
import com.github.gleyder42.monowire.common.model.*
import com.github.gleyder42.monowire.persistence.datasource.ModInstanceLibraryDataSource
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.*
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.moveTo

@Single
class ModInstanceLibrary : KoinComponent {

    private val dataSource by inject<ModInstanceLibraryDataSource>()
    private val gameDirectory by NamedComponent.gameDirectory()
    private val temporaryDirectory by NamedComponent.temporaryDirectory()
    private val pathHelper by inject<PathHelper>()

    suspend fun install(feature: ModFeature): Ior<ModInstallError, ModFiles> {
        // Copy the files into the game directory

        val copyResult = when (val copyResult = with(pathHelper) { feature.modPath.copyDirectorySiblingsRecursivelyTo(gameDirectory) }) {
            // No files were copied
            is Ior.Left -> return ModInstallError.CannotCopyFiles(copyResult.value).leftIor()
            // Some files were copied
            is Ior.Both -> {
                val wereCopied = copyResult.rightValue.toDest.toMutableSet()

                val cannotBeDeleted = mutableSetOf<FileError>()
                wereCopied.removeIf { path ->
                    when (val result = with(pathHelper) { path.safeDelete() }) {
                        is Either.Left -> {
                            cannotBeDeleted.add(result.value)
                            false
                        }
                        is Either.Right -> true
                    }
                }

                val nelCannotBeDeleted = cannotBeDeleted.toNonEmptyListOrNull()
                if (nelCannotBeDeleted != null) {
                    return ModInstallError.CannotCleanUpFiles(nelCannotBeDeleted).leftIor()
                }

                return ModInstallError.CannotCopyFiles(copyResult.leftValue).leftIor()
            }
            // All files were copied
            is Ior.Right -> copyResult.value
        }

        // Update the changes
        val modInstance = ModInstance(
            ModInstanceId.random(),
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

        val temporaryDirectory = temporaryDirectory `⫽` UUID.randomUUID().toString()

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
                when (val result = pathHelper.safeDeleteRecursively(temporaryDirectory, deleteSource = true)) {
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

    data class CannotCopyFiles(val files: NonEmptyList<FileError>) : ModInstallError

    data class CannotCleanUpFiles(val errors: NonEmptyList<FileError>) : ModInstallError
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
