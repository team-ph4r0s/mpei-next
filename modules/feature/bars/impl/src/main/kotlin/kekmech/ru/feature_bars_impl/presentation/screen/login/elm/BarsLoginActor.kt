package kekmech.ru.feature_bars_impl.presentation.screen.login.elm

import kekmech.ru.feature_bars_impl.data.mapper.BarsDisciplineMapper
import kekmech.ru.feature_bars_impl.data.repository.BarsRepository
import kekmech.ru.feature_bars_impl.domain.ExceptionWithId
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.CheckAuthStatusFailure
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.CheckAuthStatusSuccess
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.GetAccountsFailure
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.GetAccountsSuccess
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.LoginWithPasswordFailure
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.LoginWithPasswordSuccess
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.RequestTwoFactorCodeFailure
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.RequestTwoFactorCodeSuccess
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.Submit2faCodeFailure
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.Submit2faCodeSuccess
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent.Internal.SubscribeTwoFactorCodeTimerSuccess
import kekmech.ru.lib_bars_client.BarsLoginResult
import kekmech.ru.lib_bars_parser.BarsStudentLink
import kekmech.ru.lib_bars_parser.BarsTwoFactorKind
import kekmech.ru.lib_bars_parser.BarsTwoFactorPage
import kekmech.ru.lib_bars_parser.BarsTwoFactorProvider
import kekmech.ru.lib_elm.actorFlow
import kekmech.ru.lib_navigation.PopBackStack
import kekmech.ru.lib_navigation.Router
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import money.vivid.elmslie.core.store.Actor
import timber.log.Timber
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginCommand as Command
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.BarsLoginEvent as Event

private const val TWO_FACTOR_DEBOUNCE_SEC = 30

internal class BarsLoginActor(
    private val router: Router,
    private val barsRepository: BarsRepository,
) : Actor<Command, Event>() {

    private val api get() = barsRepository.api

    private var activeRequestCodeDeferred: CompletableDeferred<Unit>? = null

    override fun execute(command: Command): Flow<Event> =
        when (command) {
            is Command.CheckAuthStatus -> actorFlow {
                // The saved session is brought back; whether BARS still keeps it is known by the first page
                if (api.restoreSession() && barsRepository.getCurrentAccount() != null) {
                    AuthStatus.LoggedIn
                } else {
                    AuthStatus.LoginRequired
                }
            }.mapEvents(
                eventMapper = ::CheckAuthStatusSuccess,
                errorMapper = { e ->
                    val e = ExceptionWithId(e)
                    Timber.e(e.source, "BARS login restoreSession failed, id=${e.uuid}")
                    CheckAuthStatusFailure(e)
                },
            )

            is Command.LoginWithPassword -> actorFlow {
                barsRepository.twoFactorPage = null
                // The code is requested by the next command, once the screen of the code is shown
                val result = api.login(
                    login = command.login,
                    password = command.password,
                    rememberMe = false,
                    sendCode = false,
                )
                when (result) {
                    is BarsLoginResult.Rejected -> LoginStatus.WrongCredentials
                    is BarsLoginResult.LoggedIn -> {
                        barsRepository.students = result.students
                        LoginStatus.AccountSelectionRequired
                    }
                    is BarsLoginResult.TwoFactorRequired -> {
                        barsRepository.twoFactorPage = result.page
                        LoginStatus.TwoFactorRequired(
                            defaultProvider = result.page.default.kind.toDomain(),
                            providers = result.page.providers.map { it.kind.toDomain() }.distinct(),
                            serverMessage = null,
                        )
                    }
                }
            }.mapEvents(
                eventMapper = ::LoginWithPasswordSuccess,
                errorMapper = { e ->
                    val e = ExceptionWithId(e)
                    Timber.e(e.source, "BARS login failed, id=${e.uuid}")
                    LoginWithPasswordFailure(e)
                },
            )

            is Command.RequestTwoFactorCode -> actorFlow {
                val deferred = CompletableDeferred<Unit>()
                activeRequestCodeDeferred = deferred
                try {
                    val page = requireNotNull(barsRepository.twoFactorPage) { "The sign-in is not waiting for a code" }
                    val result = api.sendCode(page.providerFor(command.provider))
                    if (result.success == false || result.error != null) error("Unable to send code")
                    deferred.complete(Unit)
                    result.message
                } catch (e: Throwable) {
                    deferred.completeExceptionally(e)
                    throw e
                }
            }.mapEvents(
                eventMapper = { serverMsg ->
                    RequestTwoFactorCodeSuccess(
                        provider = command.provider,
                        debounceSec = TWO_FACTOR_DEBOUNCE_SEC,
                        serverMessage = serverMsg,
                    )
                },
                errorMapper = { e ->
                    val e = ExceptionWithId(e)
                    Timber.e(e.source, "BARS 2fa sendCode failed, id=${e.uuid}")
                    RequestTwoFactorCodeFailure(e)
                },
            )

            is Command.SubscribeTwoFactorCodeTimer -> flow {
                for (i in TWO_FACTOR_DEBOUNCE_SEC downTo 0) {
                    emit(i)
                    delay(1000)
                }
            }.mapEvents(::SubscribeTwoFactorCodeTimerSuccess)

            is Command.Submit2faCode -> actorFlow {
                runCatching { activeRequestCodeDeferred?.await() }
                when (val result = api.submitCode(command.code)) {
                    is BarsLoginResult.LoggedIn -> {
                        barsRepository.twoFactorPage = null
                        barsRepository.students = result.students
                        TwoFactorCodeStatus.AccountSelectionRequired
                    }
                    is BarsLoginResult.Rejected ->
                        if (result.canRetryCode) TwoFactorCodeStatus.InvalidCode else error("The sign-in has ended")
                    is BarsLoginResult.TwoFactorRequired -> TwoFactorCodeStatus.InvalidCode
                }
            }.mapEvents(
                eventMapper = ::Submit2faCodeSuccess,
                errorMapper = { e ->
                    val e = ExceptionWithId(e)
                    Timber.e(e.source, "BARS 2fa submitCode failed, id=${e.uuid}")
                    Submit2faCodeFailure(e)
                },
            )

            is Command.GetAccounts -> actorFlow {
                val accounts = barsRepository.students.map { it.toDomain() }
                    .ifEmpty { listOf(loadOnlyAccount()) }
                barsRepository.saveAccounts(accounts)
                accounts
            }.mapEvents(
                eventMapper = ::GetAccountsSuccess,
                errorMapper = { e ->
                    val e = ExceptionWithId(e)
                    Timber.e(e.source, "BARS accounts failed, id=${e.uuid}")
                    GetAccountsFailure(e)
                },
            )

            is Command.SubmitAccountId -> actorFlow {
                // The first page of the student: it tells the group and checks that the id is the one of this account
                val page = api.loadMarksPage(command.id)
                val known = barsRepository.students.firstOrNull { it.id == command.id }
                barsRepository.saveAccounts(
                    listOf(
                        Account(
                            id = command.id,
                            name = page.studentName ?: known?.name.orEmpty(),
                            group = page.studentGroup.orEmpty(),
                            status = "",
                        )
                    )
                )
                barsRepository.saveCurrentUserId(command.id)
                barsRepository.saveDisciplines(BarsDisciplineMapper.map(page.disciplines))
                barsRepository.students = emptyList()
                barsRepository.loginStateTrigger.emit(Unit)
                withContext(Dispatchers.Main) {
                    router.executeCommand(PopBackStack())
                }
            }.mapEvents(
                errorMapper = { e ->
                    val e = ExceptionWithId(e)
                    Timber.e(e.source, "BARS select account failed, id=${e.uuid}")
                    GetAccountsFailure(e)
                },
            )

            is Command.Exit -> actorFlow {
                barsRepository.twoFactorPage = null
                withContext(Dispatchers.Main) {
                    router.executeCommand(PopBackStack())
                }
            }.mapEvents()
        }

    /** An account with one student goes without the choice: his id is the one the portal gave at the sign-in. */
    private suspend fun loadOnlyAccount(): Account {
        val savedId = barsRepository.getCurrentAccount()?.id
        val page = api.loadMarksPage(savedId)
        return Account(
            id = checkNotNull(savedId ?: api.studentId) { "The student is not known" },
            name = page.studentName.orEmpty(),
            group = page.studentGroup.orEmpty(),
            status = "",
        )
    }

    private fun BarsTwoFactorPage.providerFor(provider: CodeProvider): BarsTwoFactorProvider =
        providers.firstOrNull { it.kind.toDomain() == provider } ?: default

    private fun BarsTwoFactorKind.toDomain(): CodeProvider =
        when (this) {
            BarsTwoFactorKind.TOTP -> CodeProvider.TOTP
            BarsTwoFactorKind.EMAIL -> CodeProvider.EMAIL
            BarsTwoFactorKind.MAX -> CodeProvider.MAX
            BarsTwoFactorKind.TELEGRAM -> CodeProvider.TG
            BarsTwoFactorKind.VK -> CodeProvider.VK
        }

    private fun BarsStudentLink.toDomain(): Account =
        Account(
            id = id,
            name = name,
            group = "",
            status = "",
        )
}
