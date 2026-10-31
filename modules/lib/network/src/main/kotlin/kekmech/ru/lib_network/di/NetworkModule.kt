package kekmech.ru.lib_network.di

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.LogLevel
import kekmech.ru.ext_json.LocalDateSerializer
import kekmech.ru.ext_json.LocalDateTimeSerializer
import kekmech.ru.ext_json.LocalTimeSerializer
import kekmech.ru.ext_koin.bindIntoList
import kekmech.ru.lib_app_info.AppVersionName
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kekmech.ru.lib_network.AppEnvironment
import kekmech.ru.lib_network.NoConnectionInterceptor
import kekmech.ru.lib_network.ServiceUrlResolver
import kekmech.ru.lib_network.device_id.DeviceLocaleProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import okhttp3.Interceptor
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val LOG_TAG = "Ktor"

/**
 * The one network stack of the app: a Ktor client over the OkHttp engine. What the OkHttp interceptors of the old
 * stack did stays: the headers of the app are the [ClientIdentity] of the client, the check of the connection and
 * Chucker (debug and QA builds, see `:lib_chucker`) are interceptors of the engine, the log is the Logging plugin of
 * Ktor and is on outside of the production environment.
 */
public val LibraryNetworkModule: Module = module {
    factoryOf(::DeviceLocaleProvider)
    singleOf(::provideJson)
    factoryOf(::NoConnectionInterceptor) bindIntoList Interceptor::class
    single { provideEngine(getAll()) }
    single { provideHttpClient(get(), get<AppVersionName>().versionName, get<DeviceLocaleProvider>().getLanguage()) }
}

private fun provideJson() = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    serializersModule = SerializersModule {
        contextual(LocalDate::class, LocalDateSerializer)
        contextual(LocalTime::class, LocalTimeSerializer)
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
}

private fun provideEngine(interceptors: List<Interceptor>): HttpClientEngine =
    OkHttp.create {
        config {
            interceptors.forEach(::addInterceptor)
        }
    }

private fun provideHttpClient(engine: HttpClientEngine, appVersion: String, deviceLanguage: String): HttpClient {
    val logged = ServiceUrlResolver.getAppEnvironment() != AppEnvironment.PROD
    return createHttpClient(
        identity = ClientIdentity(
            userAgent = "mpeixAndroidClient/1.0",
            deviceOs = "Android",
            deviceLocale = deviceLanguage,
            appVersion = appVersion,
        ),
        engine = engine,
        logger = if (logged) { message -> Log.d(LOG_TAG, message) } else null,
        logLevel = LogLevel.BODY,
    )
}
