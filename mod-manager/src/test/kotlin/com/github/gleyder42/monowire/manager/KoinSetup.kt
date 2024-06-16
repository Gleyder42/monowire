package com.github.gleyder42.monowire.manager

import com.github.gleyder42.monowire.common.CommonModule
import com.github.gleyder42.monowire.common.model.DisplayName
import com.github.gleyder42.monowire.common.model.ModId
import com.github.gleyder42.monowire.common.model.ModVersion
import com.github.gleyder42.monowire.common.resolve
import com.github.gleyder42.monowire.manager.ModInstanceLibraryTest.ModSetupResult
import com.github.gleyder42.monowire.persistence.sql.DatabaseControl
import com.github.gleyder42.monowire.persistence.sql.DelightDataSourceModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ksp.generated.module
import java.nio.file.Path

const val GAME_DIRECTORY = "gameDirectory"
const val TEMPORARY_DIRECTORY = "temporary"

object KoinSetup {

    suspend fun setupBase(namespace: Path, gamePath: Path? = null): Path {
        @Suppress("NAME_SHADOWING")
        val gamePath = gamePath ?: namespace.resolve("game")

        startKoin {
            modules(
                DelightDataSourceModule.module,
                DelightDataSourceModule.sqlDriverModule,
                CommonModule.module,
                module {
                    single<String>(DelightDataSourceModule.DB_PATH_KEY) { namespace.resolve("database").toString() }
                    single<Path>(named(GAME_DIRECTORY)) { gamePath }
                    single<Path>(named(TEMPORARY_DIRECTORY)) { namespace resolve TEMPORARY_DIRECTORY }
                }
            )
        }

        GlobalContext.get().get<DatabaseControl>().createSchema()

        return gamePath
    }

    suspend fun setupDependencies(namespace: Path, gamePath: Path? = null): Triple<Path, ModCatalogue, ModInstanceLibrary> {
        @Suppress("NAME_SHADOWING")
        val gamePath = setupBase(namespace, gamePath)

        val modCatalogue = ModCatalogue()
        val modLibrary = ModInstanceLibrary()

        return Triple(gamePath, modCatalogue, modLibrary)
    }

    suspend fun setupMod(namespace: Path, modSrc: Path, gamePath: Path? = null): ModSetupResult {
        @Suppress("NAME_SHADOWING")
        val gamePath = setupBase(namespace, gamePath)
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
}