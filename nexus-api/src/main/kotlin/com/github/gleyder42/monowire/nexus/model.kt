package com.github.gleyder42.monowire.nexus

import arrow.core.Either
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.github.gleyder42.monowire.common.model.ModVersion
import java.net.URL
import java.time.ZonedDateTime

const val CYBERPUNK = "cyberpunk2077"

enum class Category(private val string: String) {
    MAIN("main"),
    UPDATE("update"),
    OPTIONAL("optional"),
    OLD_VERSION("old_version"),
    MISCELLANEOUS("miscellaneous");

    override fun toString() = string
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(value = [
    "id", // array containing the game id and file id
    "size" // duplicate of sizeKb
])
data class NexusModFile(
    val uid: Uid,
    val fileId: FileId,
    val name: String,
    val version: ModVersion,
    val categoryId: CategoryId,
    val categoryName: CategoryName,
    val isPrimary: Boolean,
    val size: Long,
    val fileName: String,
    val uploadedTimestamp: Long,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    val uploadedTime: ZonedDateTime,
    val modVersion: ModVersion,
    val externalVirusScanUrl: URL?,
    val description: String?,
    val sizeKb: Long,
    val sizeInBytes: Long,
    val changelogHtml: String?,
    val contentPreviewLink: ContentPreviewLink
)

@JvmInline
value class ContentPreviewLink(val url: URL)

suspend fun NexusClient.getPreviewFiles(preview: ContentPreviewLink): Either<RequestError, RootPath> = call<RootPath>(preview.url)

@JvmInline
value class Uid(val long: Long)

@JvmInline
value class CategoryId(val int: Int)

@JvmInline
value class CategoryName(val string: String)

@JvmInline
value class FileId(val int: Int) {
    override fun toString(): String = int.toString()
}

data class RootPath(val children: List<PreviewPath>)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(DirectoryPreviewPath::class, name = "directory"),
    JsonSubTypes.Type(FilePreviewPath::class, name = "file"),
)
sealed class PreviewPath(
    val path: String,
    val name: String,
)

class DirectoryPreviewPath(path: String, name: String, val children: List<PreviewPath>) : PreviewPath(path, name)

class FilePreviewPath(path: String, name: String, val size: String) : PreviewPath(path, name)