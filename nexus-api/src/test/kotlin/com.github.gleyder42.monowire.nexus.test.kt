import arrow.core.getOrElse
import com.github.gleyder42.monowire.common.getOrThrow
import com.github.gleyder42.monowire.common.model.ModId
import com.github.gleyder42.monowire.nexus.Category
import com.github.gleyder42.monowire.nexus.FileId
import com.github.gleyder42.monowire.nexus.NexusClient
import com.github.gleyder42.monowire.nexus.getPreviewFiles
import io.kotest.core.spec.style.ShouldSpec
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class NexusClientTest : ShouldSpec({
    should("parse time") {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        val test = "2024-06-25T03:54:46.000+00:00"
//        val dateTime = LocalDateTime.parse(test, formatter)
        println(ZonedDateTime.now().format(formatter))
        println(test)
    }

    should("extract downloaded mods") {
        val nexusClient = NexusClient()

        val nexusService = nexusClient.service
        val result = nexusService.getFiles(
            ModId(15168),
            FileId(80987),
            Category.MAIN
        ).getOrThrow()
        println(result)

        val orThrow = nexusClient.getPreviewFiles(result.contentPreviewLink).getOrThrow()
    }
})