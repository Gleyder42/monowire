import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.gleyder42.monowire.nexus.RootPath
import io.kotest.core.spec.style.ShouldSpec
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import kotlin.io.path.Path
import kotlin.io.path.readLines

class NexusClientTest : ShouldSpec({
    should("call api") {
        val mapper = JsonMapper.builder()
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build()
            .registerKotlinModule()

        val link = "https://file-metadata.nexusmods.com/file/nexus-files-s3-meta/3333/14896/Hyst_Angel_BodyMod-14896-1-0-1-1717880722.zip.json"
        val request = Request(Method.GET, link)

        val client = JavaHttpClient()

        val content = client(request).bodyString()
        println(content)
        val value = mapper.readValue<RootPath>(content)
        println(value)
    }

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