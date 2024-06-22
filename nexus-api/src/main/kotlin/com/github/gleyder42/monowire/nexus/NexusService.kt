package com.github.gleyder42.monowire.nexus

import com.fasterxml.jackson.annotation.*
import com.github.gleyder42.monowire.common.model.ModId
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NexusService {

    @GET("v1/games/$CYBERPUNK/mods/{modId}/files.json")
    suspend fun getFiles(@Path("modId") id: ModId, @Query("category") category: Category): Response<PreviewPath>
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

class DirectoryPreviewPath(path: String, name: String, val children: List<PreviewPath>?) : PreviewPath(path, name)

class FilePreviewPath(path: String, name: String, val size: String) : PreviewPath(path, name)