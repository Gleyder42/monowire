package com.github.gleyder42.monowire.persistence.sql

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.gleyder42.monowire.persistence.Database
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.*

@ComponentScan
@Module
class SqlDataSourceModule {

    companion object {

        val DB_PATH_KEY = named("dbPath")

        val sqlDriverModule = module {
            // We need to JdbcSqliteDriver here, so we have access closeConnection() and closeConnection() methods.
            // The close method from the Closable interface is empty (likely a bug).
            // Therefore, the specialized JdbcSqliteDriver interface is required instead of the more general SqlDriver.
            single<JdbcSqliteDriver> {
                JdbcSqliteDriver(
                    url = "jdbc:sqlite:${get<String>(DB_PATH_KEY)}",
                    properties = Properties().apply { put("foreign_keys", "true") }
                )
            }
            // Other components use SqlDriver, so we can also access the driver through the general SqlDriver interface.
            // Cannot use constructor DSL, because Database() is not a constructor,
            // but an overloaded invoke method on Database companion object.
            single<SqlDriver> { get<JdbcSqliteDriver>() }
            single { Database(get()) }
            singleOf(::DatabaseControl)
        }
    }
}

@Single
class DatabaseControl : KoinComponent {

    private val sqlDriver by inject<JdbcSqliteDriver>()

    suspend fun createSchema() {
        // Await is important here. Without it the statement would do nothing.
        Database.Schema.create(sqlDriver).await()
    }

    fun close() {
        // Cannot use sqlDriver.close() here, because it's implementation is empty (likely a bug).
        // Therefore, the specialized JdbcSqliteDriver class is required instead of the more general SqlDriver
        sqlDriver.closeConnection(sqlDriver.getConnection())
    }
}

