package com.github.gleyder42.monowire.manager

import arrow.core.Either
import arrow.core.Ior
import arrow.core.NonEmptyList
import arrow.core.raise.either
import com.github.gleyder42.monowire.common.*
import com.github.gleyder42.monowire.common.model.*
import com.github.gleyder42.monowire.persistence.datasource.ModCatalogueDataSource
import com.github.gleyder42.monowire.persistence.sql.ModCatalogueError
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.IOException
import java.nio.file.Path

class ModCatalogue : KoinComponent {

    companion object {
        const val MOD_CATALOGUE_DIR_KEY = "catalogue"
        const val MOD_IMPORT_DIR_KEY = "import"
    }

    private val dataSource by inject<ModCatalogueDataSource>()
    private val modCatalogueDir by inject<Path>(named(MOD_CATALOGUE_DIR_KEY))
    private val modImportDir by inject<Path>(named(MOD_IMPORT_DIR_KEY))

    fun loadFromImporteDir(): Either<ModImportError, List<PartialMod>> = either {
        val modPaths = modImportDir.listDirectoryEntries()
            .mapLeft { ModImportError.CannotListPaths("Cannot list files of $modImportDir", it) }
            .bind()

        val partialMods = mutableListOf<PartialMod>()
        for (modPath in modPaths) {
            val partialModFeatures = mutableSetOf<PartialModFeature>()
            val modFeaturePaths = modPath.listDirectoryEntries()
                .mapLeft { ModImportError.CannotListPaths("Cannot list files of $modPath", it) }
                .bind()

            for (importFeaturePath in modFeaturePaths) {
                val modFeatureKey = ModFeatureKey(importFeaturePath.last().toString())
                val modFeaturePath = modCatalogueDir `⫽` modFeatureKey.string
                importFeaturePath.moveDirectoryRecursivelyTo(modCatalogueDir)
                    .conservativeToEither()
                    .mapLeft { ModImportError.CannotCopyDirectory(it) }
                    .bind()

                val partialModFeature = PartialModFeature(
                    modFeaturePath,
                    modFeatureKey
                )

                partialModFeatures.add(partialModFeature)
            }

            val partialMod = PartialMod(
                DisplayName(modPath.fileName.toString()),
                partialModFeatures
            )

            partialMods.add(partialMod)

            modPath.deleteExisting().mapLeft { ModImportError.CannotDeleteModDirectory(modImportDir, it) }.bind()
        }

        partialMods
    }

    private fun <A, B> Ior<A, B>.conservativeToEither(): Either<A, B> {
        return when (this) {
            is Ior.Both -> Either.Left(this.leftValue)
            is Ior.Left -> Either.Left(this.value)
            is Ior.Right -> Either.Right(this.value)
        }
    }

    suspend fun addModIfNotExist(mod: Mod) {
        if (!dataSource.exists(mod.descriptor)) {
            dataSource.addMod(mod)
        }
    }

    suspend fun addMod(mod: Mod): Either<ModCatalogueError, Unit> {
        return dataSource.addMod(mod)
    }

    suspend fun getMod(descriptor: ModDescriptor): Mod? {
        return dataSource.getMod(descriptor)
    }

    sealed interface ModImportError {

        data class CannotListPaths(val message: String, val exception: IOException): ModImportError

        data class CannotCopyDirectory(val errors: NonEmptyList<FileError>) : ModImportError

        data class CannotDeleteModDirectory(val modDirectory: Path, val exception: IOException): ModImportError
    }
}