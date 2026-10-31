package kekmech.ru.feature_contributors_impl.di

import kekmech.ru.feature_contributors_api.data.repository.ContributorsRepository
import kekmech.ru.feature_contributors_impl.data.network.GitHubService
import kekmech.ru.feature_contributors_impl.data.repository.ContributorsRepositoryImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val FeatureContributorsModule = module {
    factoryOf(::GitHubService)
    singleOf(::ContributorsRepositoryImpl) bind ContributorsRepository::class
}
