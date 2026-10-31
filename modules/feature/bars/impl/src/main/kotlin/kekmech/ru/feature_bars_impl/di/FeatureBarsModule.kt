package kekmech.ru.feature_bars_impl.di

import io.ktor.client.engine.HttpClientEngine
import kekmech.ru.feature_bars_api.BarsFeatureLauncher
import kekmech.ru.feature_bars_api.BarsLogoutHandle
import kekmech.ru.feature_bars_impl.data.network.KeystoreBarsSessionStore
import kekmech.ru.feature_bars_impl.data.repository.BarsRepository
import kekmech.ru.feature_bars_impl.launcher.BarsFeatureLauncherImpl
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginActor
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginStoreFactory
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeActor
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeStoreFactory
import kekmech.ru.lib_bars_client.BarsApi
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

    singleOf(::BarsRepository) binds arrayOf(BarsRepository::class, BarsLogoutHandle::class)

    single { KeystoreBarsSessionStore(androidApplication()) }
    // The client of the portal sends through the engine of the app, so Chucker sees it in the debug builds
    single<BarsApi> { BarsClient(engine = get<HttpClientEngine>(), sessionStore = get<KeystoreBarsSessionStore>()) }
}
