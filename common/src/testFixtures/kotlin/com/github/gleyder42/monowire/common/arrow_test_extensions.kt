package com.github.gleyder42.monowire.common

import arrow.core.Either
import arrow.core.Ior
import org.junit.jupiter.api.fail

infix fun <A, B> Either<A, B>.extractLeft(assert: (left: A)->Unit) {
    when (this) {
        is Either.Left -> assert(this.value)
        is Either.Right -> fail("Expected left, but got right ${this.value}")
    }
}

infix fun <A, B> Either<A, B>.extractRight(assert: (right: B)->Unit) {
    when (this) {
        is Either.Left -> fail("Expected right, but got left ${this.value}")
        is Either.Right -> assert(this.value)
    }
}

infix fun <A, B> Ior<A, B>.extractLeft(assert: (left: A)->Unit) {
    when (this) {
        is Ior.Left -> assert(this.value)
        is Ior.Both -> fail("Expected left, but got both ${this.leftValue} and ${this.rightValue}")
        is Ior.Right -> fail("Expected left, but got right ${this.value}")
    }
}

infix fun <A, B> Ior<A, B>.extractBoth(assert: (both: Pair<A, B>)->Unit) {
    when (this) {
        is Ior.Left -> fail("Expected both, but got left ${this.value}")
        is Ior.Both -> assert(this.leftValue to this.rightValue)
        is Ior.Right -> fail("Expected both, but got right ${this.value}")
    }
}

infix fun <A, B> Ior<A, B>.extractRight(assert: (right: B)->Unit) {
    when (this) {
        is Ior.Left -> fail("Expected right, but got left ${this.value}")
        is Ior.Both -> fail("Expected right, both ${this.leftValue} and ${this.rightValue}")
        is Ior.Right -> assert(this.value)
    }
}