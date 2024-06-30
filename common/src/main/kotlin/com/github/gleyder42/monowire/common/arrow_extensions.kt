package com.github.gleyder42.monowire.common

import arrow.core.*



inline fun <A, B> Pair<Iterable<A>, B>.ior(isEmpty: (B)->Boolean): IorNel<A, B> {
    val nel = first.toNonEmptyListOrNull()
    return when {
        nel == null -> second.rightIor()
        isEmpty(second) -> nel.leftIor()
        else -> (nel to second).bothIor()
    }
}

fun <A, B, I: Collection<B>> Pair<Iterable<A>, I>.ior(): IorNel<A, I> = this.ior { it.isEmpty() }

fun <A, B, T> Iterable<T>.mapOrAccumulate(mapper: (T)->Either<A, B>): IorNel<A, List<B>> {
    val errors = mutableListOf<A>()
    val results = mutableListOf<B>()
    for (t in this) {
        when (val either = mapper(t)) {
            is Either.Left -> errors.add(either.value)
            is Either.Right -> results.add(either.value)
        }
    }
    
    return (errors to results).ior()
}

sealed interface ExclusiveNel<A, B> {

    val first: Collection<A>
    val second: Collection<B>

    data class First<A, B>(override val first: Nel<A>, override val second: List<B>) : ExclusiveNel<A, B>

    data class Second<A, B>(override val first: List<A>, override val second: Nel<B>) : ExclusiveNel<A, B>
}

