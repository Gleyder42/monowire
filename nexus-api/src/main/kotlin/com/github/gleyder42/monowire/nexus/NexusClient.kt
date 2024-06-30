package com.github.gleyder42.monowire.nexus

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.retrofit.adapter.either.EitherCallAdapterFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.gleyder42.monowire.common.listDirectoryEntries
import kotlinx.coroutines.coroutineScope
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.create
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.TimeUnit

// NexusMod specific
@Suppress("UastIncorrectHttpHeaderInspection")
const val API_KEY = "apiKey"

const val USER_AGENT_KEY = "User-Agent"

const val CACHE_CONTROL = "Cache-Control"

class NexusClient {

    companion object {

        fun Request.Builder.addDefaultHeader(): Request.Builder {
            addHeader(USER_AGENT_KEY, "Monowire/1.0.0")
            addHeader(CACHE_CONTROL, CacheControl.Builder().maxAge(30, TimeUnit.SECONDS).build().toString())
            return this
        }

        private val interceptor = { chain: Interceptor.Chain ->
            with(chain) {
                proceed(request().newBuilder().apply {
                    addHeader(API_KEY, apiKey)
                    addDefaultHeader()
                }.build())
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()

    private val objectMapper = ObjectMapper()
        .registerModules(JavaTimeModule())
        .registerKotlinModule()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.nexusmods.com")
        .client(okHttpClient)
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .addCallAdapterFactory(EitherCallAdapterFactory.create())
        .build()

    val service: NexusService = retrofit.create()

    internal suspend inline fun <reified T> call(url: URL): Either<RequestError, T> = coroutineScope {
        val request = Request.Builder()
            .url(url)
            .addDefaultHeader()
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body()
            if (!response.isSuccessful) {
                RequestError.UnsuccessfulRequest(response).left()
            } else if (body == null){
                RequestError.EmptyBody(response).left()
            } else {
                val result: T = objectMapper.readValue<T>(body.string())
                result.right()
            }
        } catch (exception: IOException) {
            RequestError.NetworkError(exception).left()
        }
    }
}

sealed interface RequestError {

    data class EmptyBody(val response: Response) : RequestError

    data class NetworkError(val exception: IOException) : RequestError

    data class UnsuccessfulRequest(val response: Response) : RequestError
}

sealed interface URIConversionError {

    val exception: Exception

    data class URLNotAbsolute(override val exception: IllegalArgumentException) : URIConversionError

    data class MalformedURL(override val exception: MalformedURLException) : URIConversionError
}

fun URI.tryIntoUrl(): Either<URIConversionError, URL> {
    return try {
        this.toURL().right()
    } catch (exception: IllegalArgumentException) {
        URIConversionError.URLNotAbsolute(exception).left()
    } catch (exception: MalformedURLException) {
        URIConversionError.MalformedURL(exception).left()
    }
}

val apiKey by lazy {
    val path = Path.of("secrets.toml")
    val module = TomlMapper().registerKotlinModule()
    println(Path.of("").listDirectoryEntries())
    module.readValue<SecretConfig>(path.toFile()).apiKey
}

data class SecretConfig(val apiKey: String)