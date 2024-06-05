package com.github.gleyder42.monowire.manager

import arrow.core.Either
import arrow.core.Ior
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.github.gleyder42.monowire.common.copyDirectorySiblingsRecursivelyTo
import com.github.gleyder42.monowire.common.model.ModFeature
import com.github.gleyder42.monowire.common.model.ModFeatureDescriptor
import com.github.gleyder42.monowire.common.model.ModFiles
import com.github.gleyder42.monowire.common.model.ModInstance
import com.github.gleyder42.monowire.persistence.datasource.ModInstanceLibraryDataSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.nio.file.Path
import kotlin.io.path.deleteExisting

class ModInstanceLibrary : KoinComponent {

    private val dataSource by inject<ModInstanceLibraryDataSource>()
    private val gameDirectory by inject<Path>(named("gameDirectory"))

    suspend fun install(feature: ModFeature): Either<ModUninstallError, Unit> = either {
        // Copy the files into the game directory
        val copyResult = when (val modFiles = feature.modPath.copyDirectorySiblingsRecursivelyTo(gameDirectory)) {
            is Ior.Left -> raise(ModUninstallError.CannotDeleteFiles(modFiles.value.map { it.failedFile }.toList()))
            is Ior.Both -> raise(ModUninstallError.CannotDeleteFiles(modFiles.leftValue.map { it.failedFile }.toList()))
            is Ior.Right -> { modFiles.value }
        }

        // Update the changes
        val modInstance = ModInstance(
            copyResult.toDest,
            feature.version,
            feature.descriptor
        )

        // Persist the changes
        dataSource.addModInstance(modInstance)
    }

    suspend fun uninstall(descriptor: ModFeatureDescriptor): Either<ModUninstallError, ModFiles> = either {
        val installed = dataSource.getModInstance(descriptor)
        ensureNotNull(installed) { ModUninstallError.ModDoesNotExist(descriptor) }

        // Try to delete files
        // TODO Make a safe delete here
        installed.installedFiles.forEach { it.deleteExisting() }

        // Only remove the mod instance if the mod files were completely removed
        dataSource.removeModInstance(descriptor)

        // TODO Add option to just move files out instead of deleting them
        installed.installedFiles
    }
}

sealed interface ModUninstallError {

    data class ModDoesNotExist(val descriptor: ModFeatureDescriptor) : ModUninstallError

    @JvmInline
    value class CannotDeleteFiles(val paths: List<Path>) : ModUninstallError
}
