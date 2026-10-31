package kekmech.ru.feature_contributors_impl.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kekmech.ru.feature_contributors_impl.data.model.GitHubContributorDto
import kekmech.ru.feature_contributors_impl.data.model.GitHubUserDto
import kekmech.ru.lib_network.BackendServiceUrl
import kekmech.ru.lib_network.ServiceUrlResolver

internal class GitHubService(
    private val client: HttpClient,
) {

    suspend fun getContributors(
        user: String = "tonykolomeytsev",
        repository: String = "mpeiapp",
    ): List<GitHubContributorDto> =
        client.get("${baseUrl()}repos/$user/$repository/stats/contributors").body()

    suspend fun getUser(login: String): GitHubUserDto =
        client.get("${baseUrl()}users/$login").body()

    private fun baseUrl() = ServiceUrlResolver.resolve(BackendServiceUrl.GITHUB)
}
