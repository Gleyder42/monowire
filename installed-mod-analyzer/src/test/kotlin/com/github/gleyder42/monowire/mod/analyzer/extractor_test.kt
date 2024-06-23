package com.github.gleyder42.monowire.mod.analyzer

import io.kotest.core.spec.style.ShouldSpec
import java.time.LocalDateTime
import java.time.Month
import kotlin.io.path.Path
import kotlin.io.path.readLines

class ModHistoryExtractorTest : ShouldSpec({
    should("extract downloaded mod from clipboard when copying whole mod site") {
        val input = Path("test.txt").readLines().joinToString(separator = "\n")
        val extractor = ModHistoryExtractor()

        val time = LocalDateTime.of(
            2023,
            Month.OCTOBER,
            2,
            12 + 4,
            59
        )

        println(extractor.extractDownloadedModHistory(input).joinToString("\n"))
    }
})