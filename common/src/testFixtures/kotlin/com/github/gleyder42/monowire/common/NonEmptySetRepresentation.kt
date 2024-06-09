package com.github.gleyder42.monowire.common

import arrow.core.Ior
import org.assertj.core.api.Assertions
import org.assertj.core.presentation.StandardRepresentation
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext

class RepresentationExtension : Extension, BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        Assertions.useRepresentation(NonEmptySetRepresentation)
    }
}

object NonEmptySetRepresentation : StandardRepresentation() {

    override fun fallbackToStringOf(`object`: Any?): String {
        return when (`object`) {
            is Ior.Both<*, *> -> {
                "Ior.Both(\n\tleft = ${`object`.leftValue},\n\tright = ${`object`.rightValue})"
            }
            else -> super.fallbackToStringOf(`object`)
        }
    }
}