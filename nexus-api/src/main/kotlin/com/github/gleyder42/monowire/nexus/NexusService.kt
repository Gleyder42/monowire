package com.github.gleyder42.monowire.nexus

import arrow.core.Either
import arrow.retrofit.adapter.either.networkhandling.CallError
import com.github.gleyder42.monowire.common.model.ModId
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NexusService {

    @GET("v1/games/$CYBERPUNK/mods/{modId}/files/{fileId}.json")
    suspend fun getFiles(
        @Path("modId") id: ModId,
        @Path("fileId") fileId: FileId,
        @Query("category") category: Category
    ): Either<CallError, NexusModFile>
}