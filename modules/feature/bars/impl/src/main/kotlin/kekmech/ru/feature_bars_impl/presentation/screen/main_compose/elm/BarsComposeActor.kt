package kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm

import kekmech.ru.feature_app_settings_api.AppSettingsFeatureLauncher
import kekmech.ru.feature_bars_impl.data.mapper.BarsDisciplineMapper
import kekmech.ru.feature_bars_impl.data.repository.BarsRepository
import kekmech.ru.feature_schedule_api.data.repository.ScheduleRepository
import kekmech.ru.feature_schedule_api.domain.model.WeekOfSemester
import kekmech.ru.feature_bars_impl.domain.ExceptionWithId
import kekmech.ru.feature_bars_impl.presentation.screen.details.BarsDetailsFragment
import kekmech.ru.feature_bars_impl.presentation.screen.login.BarsLoginFragment
import kekmech.ru.feature_bars_impl.presentation.screen.login.elm.Account
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeEvent.Internal.CheckAuthStatusFailure
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeEvent.Internal.GetCurrentWeekSuccess
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeEvent.Internal.GetMarksFailure
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeEvent.Internal.GetMarksSuccess
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeEvent.Internal.GetProfileSuccess
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeEvent.Internal.SubscribeAuthStatusSuccess
import kekmech.ru.lib_bars_client.BarsSessionExpiredException
import kekmech.ru.lib_elm.actorFlow
import kekmech.ru.lib_navigation.AddScreenForward
import kekmech.ru.lib_navigation.Router
import kekmech.ru.lib_navigation.ShowDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import money.vivid.elmslie.core.store.Actor
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeCommand as Command
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.elm.BarsComposeEvent as Event

internal class BarsComposeActor(
    private val appSettingsFeatureLauncher: AppSettingsFeatureLauncher,
    private val router: Router,
    private val barsRepository: BarsRepository,
    private val scheduleRepository: ScheduleRepository,
) : Actor<Command, Event>() {

    override fun execute(command: Command): Flow<Event> =
        when (command) {
            is Command.SubscribeAuthStatus -> barsRepository.loginStateTrigger
                .map {
                    // The session is checked by the first page that is asked for: BARS answers with the sign-in page if it is dead
                    if (barsRepository.api.restoreSession() && barsRepository.getCurrentAccount() != null) {
                        AuthStatus.LoggedIn
                    } else {
                        AuthStatus.LoginRequired
                    }
                }.mapEvents(
                    eventMapper = ::SubscribeAuthStatusSuccess,
                    errorMapper = { e ->
                        val e = ExceptionWithId(e)
                        timber.log.Timber.e(e.source, "BARS main auth.init failed, id=${e.uuid}")
                        CheckAuthStatusFailure(e)
                    },
                )

            is Command.GetProfile -> actorFlow {
                val acc = requireNotNull(barsRepository.getCurrentAccount()) {
                    "Unexpected state: selected account is null"
                }
                acc.toBarsProfile()
            }.mapEvents(
                eventMapper = ::GetProfileSuccess,
                errorMapper = { e ->
                    val e = ExceptionWithId(e)
                    timber.log.Timber.e(e.source, "BARS main getProfile failed, id=${e.uuid}")
                    CheckAuthStatusFailure(e)
                },
            )

            is Command.GetMarks -> flow {
                val cached = barsRepository.getSavedDisciplines()
                if (cached != null) {
                    emit(GetMarksSuccess(cached, true))
                }

                val studentId = barsRepository.getCurrentAccount()?.id
                val page = try {
                    barsRepository.api.loadMarksPage(studentId)
                } catch (expired: BarsSessionExpiredException) {
                    // BARS has closed the session: back to the sign-in prompt
                    barsRepository.onSessionExpired()
                    return@flow
                }
                val actual = BarsDisciplineMapper.map(page.disciplines)
                barsRepository.saveDisciplines(actual)
                emit(GetMarksSuccess(actual, false))
            }.mapEvents(
                eventMapper = { it },
                errorMapper = { e ->
                    val e = ExceptionWithId(e)
                    timber.log.Timber.e(e.source, "BARS main getMarks failed, id=${e.uuid}")
                    GetMarksFailure(e)
                }
            )

            // The current week is a nice-to-have for the summary: any failure just means "unknown"
            is Command.GetCurrentWeek -> actorFlow {
                getCurrentWeekOfSemester()
            }.mapEvents(
                eventMapper = ::GetCurrentWeekSuccess,
                errorMapper = { GetCurrentWeekSuccess(null) },
            )

            is Command.OpenAppSettings -> actorFlow {
                withContext(Dispatchers.Main) {
                    appSettingsFeatureLauncher.launch()
                }
            }.mapEvents()

            is Command.OpenLogin -> actorFlow {
                withContext(Dispatchers.Main) {
                    router.executeCommand(AddScreenForward { BarsLoginFragment() })
                }
            }.mapEvents()

            is Command.OpenDetails -> actorFlow {
                withContext(Dispatchers.Main) {
                    router.executeCommand(ShowDialog {
                        BarsDetailsFragment.newInstance(command.discipline)
                    })
                }
            }.mapEvents()
        }

    private suspend fun getCurrentWeekOfSemester(): Int? {
        val selected = runCatching { scheduleRepository.getSelectedSchedule() }.getOrNull() ?: return null
        val schedule = scheduleRepository
            .getSchedule(type = selected.type, name = selected.name, weekOffset = 0)
            .getOrNull()
            ?: return null
        return (schedule.weeks.firstOrNull()?.weekOfSemester as? WeekOfSemester.Studying)?.num
    }

    private fun Account.toBarsProfile(): BarsProfile =
        BarsProfile(
            name = name.split("\\s+".toRegex()).take(2).joinToString(" "),
            group = group,
        )
}