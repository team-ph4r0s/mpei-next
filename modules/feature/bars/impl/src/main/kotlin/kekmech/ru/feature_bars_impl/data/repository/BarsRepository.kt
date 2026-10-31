package kekmech.ru.feature_bars_impl.data.repository

import android.os.Parcelable
import com.tencent.mmkv.MMKV
import kekmech.ru.ext_kotlin.fromBase64
import kekmech.ru.ext_kotlin.toBase64
import kekmech.ru.feature_bars_api.BarsLogoutHandle
import kekmech.ru.feature_bars_impl.data.network.KeystoreBarsSessionStore
import kekmech.ru.feature_bars_impl.domain.AssessedDiscipline
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.Account
import kekmech.ru.lib_bars_client.BarsApi
import kekmech.ru.lib_bars_parser.BarsStudentLink
import kekmech.ru.lib_bars_parser.BarsTwoFactorPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

internal class BarsRepository(
    val api: BarsApi,
    private val sessionStore: KeystoreBarsSessionStore,
) : BarsLogoutHandle {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The page that asks for the code, while the student is entering it. Kept in memory only. */
    @Volatile
    var twoFactorPage: BarsTwoFactorPage? = null

    /** The students of the account that has just signed in, for the choice of the one whose marks are shown. */
    @Volatile
    var students: List<BarsStudentLink> = emptyList()

    private val storage = MMKV.mmkvWithID("bars")

    val loginStateTrigger = MutableSharedFlow<Unit>(replay = 1)

    init {
        loginStateTrigger.tryEmit(Unit)
    }

    fun saveCurrentUserId(id: String) {
        storage.encode("current_user_id", id)
    }

    fun saveAccounts(accounts: List<Account>) {
        storage.encode("all_user_ids", accounts.map { it.id }.toSet())
        for (acc in accounts) {
            val encodedContent = buildString {
                append(acc.id.toBase64())
                append(":")
                append(acc.name.toBase64())
                append(":")
                append(acc.group.toBase64())
                append(":")
                append(acc.status.toBase64())
            }
            storage.encode("user-${acc.id}", encodedContent)
        }
    }

    fun getCurrentAccount(): Account? {
        val currentId = storage.decodeString("current_user_id") ?: return null
        val encodedContent = storage.decodeString("user-${currentId}") ?: return null
        val parts = encodedContent.split(":")
        return Account(
            id = currentId,
            name = parts.getOrNull(1)?.fromBase64().orEmpty(),
            group = parts.getOrNull(2)?.fromBase64().orEmpty(),
            status = parts.getOrNull(3)?.fromBase64().orEmpty(),
        )
    }

    fun saveDisciplines(list: List<AssessedDiscipline>) {
        val currentId = storage.decodeString("current_user_id") ?: return
        val container = Container(list)
        storage.encode(disciplinesKey(currentId), container)
        // The Parcelable layout of the cached marks changed (ControlActivity.rawMark), drop the old cache
        storage.removeValueForKey(legacyDisciplinesKey(currentId))
    }

    fun getSavedDisciplines(): List<AssessedDiscipline>? {
        val currentId = storage.decodeString("current_user_id") ?: return null
        val container =
            storage.decodeParcelable(disciplinesKey(currentId), Container::class.java)
        return container?.disciplines
    }

    /** BARS has closed the session by itself: the saved student is of no use any more. */
    fun onSessionExpired() {
        val currentId = storage.decodeString("current_user_id")
        if (currentId != null) {
            storage.removeValueForKey("current_user_id")
            storage.removeValueForKey("user-$currentId")
            storage.removeValueForKey(disciplinesKey(currentId))
        }
        loginStateTrigger.tryEmit(Unit)
    }

    override fun logout(): Boolean {
        // The session is erased here and now, the portal is told in the background
        sessionStore.clearNow()
        twoFactorPage = null
        students = emptyList()
        scope.launch { runCatching { api.logout() } }
        val currentId = storage.decodeString("current_user_id") ?: return false
        storage.removeValueForKey("current_user_id")
        storage.removeValueForKey("user-$currentId")
        storage.removeValueForKey(disciplinesKey(currentId))
        storage.removeValueForKey(legacyDisciplinesKey(currentId))
        loginStateTrigger.tryEmit(Unit)
        return true
    }

    private fun disciplinesKey(userId: String) = "user-$userId-disciplines-v2"

    private fun legacyDisciplinesKey(userId: String) = "user-$userId-disciplines"

    @Parcelize
    private data class Container(val disciplines: List<AssessedDiscipline>) : Parcelable
}