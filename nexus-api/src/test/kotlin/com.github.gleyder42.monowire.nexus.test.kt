import io.kotest.core.spec.style.ShouldSpec
import kotlin.io.path.Path
import kotlin.io.path.readLines

class NexusClientTest : ShouldSpec({
    should("extract downloaded mods") {
        val downloadedMods = Path("test.txt").readLines().joinToString(separator = "\n")
        val startDelimiter = "Mod name\n" +
                "\tLast DL\n" +
                "\tUploader\n" +
                "\tCategory\n" +
                "\tUpdated\n" +
                "\tEndorsement\n" +
                "\tLog"
        val endDelimiter = "Previous"

        val output = downloadedMods
            .replaceBefore(startDelimiter, "")
            .replace(startDelimiter, "")
            .replaceAfter(endDelimiter, "")
            .replace(endDelimiter, "")
            .trim()
            .split("View")
            .map { modLine -> modLine.split(Regex("\\n+")).map { it.trim() }.filter { it.isNotBlank() } }
            .filter { it.isNotEmpty() }

        println(output)
    }
})