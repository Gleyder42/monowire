package com.github.gleyder42.monowire.common

import arrow.core.Either
import arrow.core.Ior
import arrow.core.getOrElse
import org.assertj.core.api.Assertions
import org.jetbrains.annotations.TestOnly
import kotlin.contracts.contract

@TestOnly
fun <A, B> Either<A, B>.getOrThrow(): B = getOrElse { throw IllegalArgumentException(it.toString()) }

fun <A, B> assertLeft(actual: Either<A, B>) {
    contract {
        returns() implies (actual is Either.Left)
    }

    Assertions.assertThat(actual)
        .`as`("check is Either.Left")
        .isInstanceOf(Either.Left::class.java)
}

fun <A, B> assertRight(actual: Either<A, B>) {
    contract {
        returns() implies (actual is Either.Right)
    }

    Assertions.assertThat(actual)
        .`as`("check is Either.Right")
        .isInstanceOf(Either.Right::class.java)
}


fun <A, B> assertLeft(actual: Ior<A, B>) {
    contract {
        returns() implies (actual is Ior.Left)
    }

    Assertions.assertThat(actual)
        .`as`("check is Ior.Left")
        .isInstanceOf(Ior.Left::class.java)
}

fun <A, B> assertRight(actual: Ior<A, B>) {
    contract {
        returns() implies (actual is Ior.Right)
    }

    Assertions.assertThat(actual)
        .`as`("check is Ior.Right")
        .isInstanceOf(Ior.Right::class.java)
}

fun <A, B> assertBoth(actual: Ior<A, B>) {
    contract {
        returns() implies (actual is Ior.Both)
    }

    Assertions.assertThat(actual)
        .`as`("check is Ior.Both")
        .isInstanceOf(Ior.Both::class.java)
}