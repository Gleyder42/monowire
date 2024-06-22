package com.github.gleyder42.monowire.nexus

const val CYBERPUNK = "cyberpunk2077"

enum class Category(val string: String) {
    MAIN("main"),
    UPDATE("update"),
    OPTIONAL("optional"),
    OLD_VERSION("old_version"),
    MISCELLANEOUS("miscellaneous");

    override fun toString() = string
}