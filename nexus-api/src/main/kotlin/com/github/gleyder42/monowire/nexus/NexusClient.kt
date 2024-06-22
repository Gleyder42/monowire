package com.github.gleyder42.monowire.nexus

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.gleyder42.monowire.common.listDirectoryEntries
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.create
import java.nio.file.Path
import java.util.concurrent.TimeUnit

// NexusMod specific
@Suppress("UastIncorrectHttpHeaderInspection")
const val API_KEY = "apiKey"

const val USER_AGENT_KEY = "User-Agent"

const val CACHE_CONTROL = "Cache-Control"

class NexusClient {

    companion object {
        private val interceptor = { chain: Interceptor.Chain ->
            with(chain) {
                proceed(request().newBuilder().apply {
                    addHeader(API_KEY, apiKey)
                    addHeader(USER_AGENT_KEY, "Monowire/1.0.0")
                    addHeader(CACHE_CONTROL, CacheControl.Builder().maxAge(30, TimeUnit.SECONDS).build().toString())
                }.build())
            }
        }
    }

    private val nexusModsRetrofit = Retrofit.Builder()
        .baseUrl("https://api.nexusmods.com")
        .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
        .addConverterFactory(JacksonConverterFactory.create(ObjectMapper().registerKotlinModule()))
        .build()

    val service: NexusService = nexusModsRetrofit.create()
}

val apiKey by lazy {
    val path = Path.of("secrets.toml")
    val module = TomlMapper().registerKotlinModule()
    println(Path.of("").listDirectoryEntries())
    module.readValue<SecretConfig>(path.toFile()).apiKey
}

data class SecretConfig(val apiKey: String)