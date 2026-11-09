package kekmech.ru.feature_bars_impl.di

import io.ktor.client.engine.HttpClientEngine
import kekmech.ru.ext_koin.bindIntoList
import kekmech.ru.feature_bars_api.BarsFeatureLauncher
import kekmech.ru.feature_bars_api.BarsNotificationSettings
import kekmech.ru.feature_bars_api.BarsSessionHandle
import kekmech.ru.feature_bars_impl.data.attendance.AttendanceController
import kekmech.ru.feature_bars_impl.data.attendance.AttendanceLifecycleObserver
import kekmech.ru.feature_bars_impl.data.attendance.AttendanceNotificationSettingsImpl
import kekmech.ru.feature_bars_impl.data.attendance.AttendanceNotifier
import kekmech.ru.feature_bars_impl.data.attendance.AttendancePrefs
import kekmech.ru.feature_bars_impl.data.attendance.AttendanceTracker
import kekmech.ru.feature_bars_impl.data.network.KeystoreBarsSessionStore
import kekmech.ru.feature_bars_impl.data.repository.BarsRepository
import kekmech.ru.feature_bars_impl.launcher.BarsFeatureLauncherImpl
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginActor
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginStoreFactory
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeActor
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeStoreFactory
import kekmech.ru.lib_bars_client.BarsApi
import kekmech.ru.lib_app_lifecycle.AppLifecycleObserver
import kekmech.ru.lib_bars_client.BarsClient
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val FeatureBarsModule = module {
    factoryOf(::BarsComposeActor)
    factoryOf(::BarsLoginActor)
    singleOf(::BarsComposeStoreFactory)
    singleOf(::BarsLoginStoreFactory)
    factoryOf(::BarsFeatureLauncherImpl) bind BarsFeatureLauncher::class

    singleOf(::BarsRepository) binds arrayOf(BarsRepository::class, BarsSessionHandle::class)

    single { KeystoreBarsSessionStore(androidApplication()) }
    // The client of the portal sends through the engine of the app, so Chucker sees it in the debug builds
    single<BarsApi> { BarsClient(engine = get<HttpClientEngine>(), sessionStore = get<KeystoreBarsSessionStore>()) }

    // The attendance: the notifications about a new skip and about the risk of being refused
    single { AttendancePrefs(androidApplication()) }
    single { AttendanceNotifier(androidApplication()) }
    single { AttendanceTracker(get(), get(), get()) }
    single { AttendanceController(androidApplication(), get()) }
    factoryOf(::AttendanceLifecycleObserver) bindIntoList AppLifecycleObserver::class
    single<BarsNotificationSettings> { AttendanceNotificationSettingsImpl(get(), get()) }
}
