package com.github.gleyder42.monowire.persistence

import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.SuspendingTransactionWithReturn
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.sqlite.SQLiteException

suspend fun <R> SuspendingTransacter.catchingTransactionWithResult(
    noEnclosing: Boolean = false,
    bodyWithReturn: suspend SuspendingTransactionWithReturn<R>.() -> R,
): Either<SQLiteException, R> {
    return try {
        transactionWithResult(noEnclosing, bodyWithReturn).right()
    } catch (exception: SQLiteException) {
        exception.left()
    }
}