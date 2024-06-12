package com.github.gleyder42.monowire.common.model

import java.nio.ByteBuffer
import java.util.UUID

data class ModInstance(
    val id: ModInstanceId,
    val installedFiles: ModFiles,
    val version: ModVersion,
    val modFeatureDescriptor: ModFeatureDescriptor
)

@JvmInline
value class ModInstanceId(private val value: UUID) {

    val string
        get() = value.toString()

    constructor(byteArray: ByteArray) :
            this(ByteBuffer.wrap(byteArray).run { UUID(getLong(0), getLong(1)) })

    companion object {

        fun random() = ModInstanceId(UUID.randomUUID())
    }
}

fun UUID.toByteArray(): ByteArray {
    val buffer = ByteBuffer.allocate(16)
    buffer.putLong(this.mostSignificantBits)
    buffer.putLong(this.leastSignificantBits)
    return buffer.array()
}

