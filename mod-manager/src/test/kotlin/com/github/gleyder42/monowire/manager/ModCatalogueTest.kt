package com.github.gleyder42.monowire.manager

import arrow.core.Either
import com.github.gleyder42.monowire.common.TestData
import com.github.gleyder42.monowire.common.model.DisplayName
import com.github.gleyder42.monowire.common.model.ModId
import com.github.gleyder42.monowire.common.model.ModVersion
import com.github.gleyder42.monowire.persistence.sql.DatabaseControl
import com.github.gleyder42.monowire.persistence.sql.SqlDataSourceModule
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import org.koin.test.get
import java.nio.file.Path

class ModCatalogueTest : KoinTest {

    @MethodSource(TestData.TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun testAddModIfNotExist(namespace: Path, src: Path, paths: List<Path>) = runTest {
        // Arrange
        startKoin {
            modules(
                TestModule().module,
                SqlDataSourceModule().module,
                SqlDataSourceModule.sqliteModule,
                module { single<String>(SqlDataSourceModule.DB_PATH_KEY) { namespace.resolve("database").toString() } }
            )
        }
        get<DatabaseControl>().createSchema()

        val mod = ModFeatureImporter.importModFeatureAsMod(
            src,
            ModId(10001),
            ModVersion("1.0.0"),
            DisplayName("Test Mod")
        )

        val catalogue = ModCatalogue()

        // Act
        catalogue.addModIfNotExist(mod)

        // Assert
        val actualMod = catalogue.getMod(mod.descriptor)
        assertThat(actualMod).isEqualTo(mod)
    }

    @MethodSource(TestData.TEST_DATA_METHOD_SOURCE)
    @ParameterizedTest
    fun testCannotAddDuplicatedMod(namespace: Path, src: Path, paths: List<Path>) = runTest {
        // Arrange
        startKoin {
            modules(
                TestModule().module,
                SqlDataSourceModule().module,
                SqlDataSourceModule.sqliteModule,
                module { single<String>(SqlDataSourceModule.DB_PATH_KEY) { namespace.resolve("database").toString() } }
            )
        }

        get<DatabaseControl>().createSchema()

        val mod = ModFeatureImporter.importModFeatureAsMod(
            src,
            ModId(10001),
            ModVersion("1.0.0"),
            DisplayName("Test Mod")
        )

        val catalogue = ModCatalogue()
        catalogue.addMod(mod)

        // Assert
        when (val result = catalogue.addMod(mod)) {
            is Either.Right -> {
                // assertions ensure type safety
                assertThat(result).isEqualTo(Unit)
                fail("Expected an error but got none")
            }
            is Either.Left -> { /* Pass */ }
        }
    }

    @AfterEach
    fun teardown() {
        get<DatabaseControl>().close()
        stopKoin()
    }
}