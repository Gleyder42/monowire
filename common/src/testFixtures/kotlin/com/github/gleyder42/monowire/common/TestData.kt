package com.github.gleyder42.monowire.common

import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.provider.Arguments
import java.nio.file.Path


object TestData {

    const val SRC_NAME = "src"
    const val TEST_DATA_METHOD_SOURCE = "com.github.gleyder42.monowire.common.TestData#directoryStructureTestData"

    // Needs to be JvmStatic so JUnit finds this function
    @JvmStatic
    // directoryStructureTestData is used, although it is recognized by IntelliJ, because it is accessed reflective
    @Suppress("unused")
    private fun directoryStructureTestData(@TempDir workingDir: Path): Array<Arguments> {
        val fs = workingDir.fileSystem

        val arguments = { namespace: Path, dirBuilder: PathBuilderFunction, expectedPaths: List<Path> ->
            // We are only interested in the namespace path prepended with the working directory, so we have
            // an absolute path.
            @Suppress("NAME_SHADOWING")
            val namespace = workingDir `|` namespace

            Arguments.of(namespace, dir(namespace `|` fs.getPath(SRC_NAME), dirBuilder), expectedPaths)
        }

        return arrayOf(
            arguments(
                fs.getPath("emptyMod"),
                { },
                listOf()
            ),
            arguments(
                fs.getPath("onlyOneFile"),
                {
                    file("hello.txt")
                },
                with(fs) {
                    listOf(getPath("hello.txt"))
                }
            ),
            arguments(
                fs.getPath("emptySubDirectories"),
                {
                    dir("a")
                    dir("b")
                    dir("c")
                },
                listOf()
            ),
            arguments(
                fs.getPath("simpleMod"),
                {
                    dir("archive").dir("pc").dir("mod") {
                        file("mod.archive")
                        file("mod.xl")
                    }

                    dir("r6").dir("tweaks").dir("archive_xl") {
                        file("mod.yaml")
                    }
                },
                with(fs) {
                    listOf(
                        getPath("archive", "pc", "mod", "mod.archive"),
                        getPath("archive", "pc", "mod", "mod.xl"),
                        getPath("r6", "tweaks", "archive_xl", "mod.yaml"),
                    )
                }
            ),
            arguments(
                fs.getPath("redMod"),
                {
                    dir("red4ext") {
                        dir("logs") {
                            file("mod_settings.1.log")
                            file("mod_settings.2.log")
                            file("mod_settings.3.log")
                        }

                        dir("plugins") {
                            dir("ArchiveXL") {
                                file("ArchiveXL.dll")
                            }
                            dir("Codeware") {
                                file("Codeware.dll")
                            }
                        }

                        file("config.ini")
                        file("LICENSE.txt")
                    }
                },
                with(fs) {
                    listOf(
                        getPath("red4ext", "logs", "mod_settings.1.log"),
                        getPath("red4ext", "logs", "mod_settings.2.log"),
                        getPath("red4ext", "logs", "mod_settings.3.log"),
                        getPath("red4ext", "plugins", "ArchiveXL", "ArchiveXL.dll"),
                        getPath("red4ext", "plugins", "Codeware", "Codeware.dll"),
                        getPath("red4ext", "config.ini"),
                        getPath("red4ext", "LICENSE.txt"),
                    )
                }
            )
        )
    }

}