@file:OptIn(ExperimentalTime::class)

package kekmech.ru.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kekmech.ru.feature_bars_shared.BarsController
import kekmech.ru.feature_map_shared.MapController
import kekmech.ru.feature_notes_shared.NotesController
import kekmech.ru.feature_schedule_shared.DashboardController
import kekmech.ru.feature_schedule_shared.RemindersController
import kekmech.ru.feature_schedule_shared.ScheduleController
import kekmech.ru.feature_schedule_shared.ScheduleUiState
import kekmech.ru.feature_schedule_shared.WeekLoader
import kekmech.ru.lib_settings.ThemeMode
import kekmech.ru.shared.bars.BarsScreen
import kekmech.ru.shared.dashboard.DashboardScreen
import kekmech.ru.ui_map.MapScreen
import kekmech.ru.shared.notes.NotesScreen
import kekmech.ru.shared.settings.SettingsScreen
import kekmech.ru.shared.schedule.ScheduleScreen
import kekmech.ru.ui_map.TileLoader
import kekmech.ru.ui_theme.theme.MpeixTheme
import kotlin.time.ExperimentalTime

/**
 * The root of the shared UI. The sections that were moved from the Android app so far: the home screen and the
 * schedule (milestone 3 of the iOS port, see issue #5), the notes (milestone 4), the map (milestone 5) and BARS
 * (milestone 6); the other sections move here module by module.
 */
@Composable
public fun App(dependencies: AppDependencies) {
    val bars = dependencies.bars
    val scheduleService = dependencies.scheduleService
    val scheduleCache = dependencies.scheduleCache
    val favorites = dependencies.favorites
    val notesStore = dependencies.notesStore
    val map = dependencies.map
    val reminderScheduler = dependencies.reminderScheduler
    var themeMode by remember { mutableStateOf(dependencies.themeSettings.mode) }
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MpeixTheme(darkTheme = darkTheme) {
        // the keyboard lifts the whole screen with the bar: the field that is typed in stays visible
        Surface(Modifier.fillMaxSize().imePadding()) {
            val scope = rememberCoroutineScope()
            val schedule = remember(scheduleService, scheduleCache, favorites) {
                ScheduleController(scheduleService, scheduleCache, favorites, scope)
            }
            val dashboard = remember(scheduleService, scheduleCache) {
                DashboardController(WeekLoader(scheduleService, scheduleCache), scope)
            }
            val barsController = remember(bars) { BarsController(bars, scope) }
            val notes = remember(notesStore) { NotesController(notesStore) }
            val mapController = remember(map) { MapController(map.markers, map.settings, scope) }
            val reminders = remember(scheduleService, scheduleCache, reminderScheduler) {
                val loader = WeekLoader(scheduleService, scheduleCache)
                RemindersController(
                    settings = dependencies.reminderSettings,
                    changeSettings = dependencies.changeSettings,
                    scheduler = reminderScheduler,
                    loadWeek = { schedule, offset -> loader.load(schedule, offset).schedule },
                    scope = scope,
                )
            }
            var showSettings by remember { mutableStateOf(false) }
            val tileLoader = remember(map) { TileLoader(scope, map.tiles) }
            val scheduleState by schedule.state.collectAsState()
            // while the search of another schedule is open, the chosen one is still the chosen one
            val selected = when (val state = scheduleState) {
                is ScheduleUiState.Week -> state.selected
                is ScheduleUiState.Search -> if (state.canCancel) scheduleCache.selected() else null
            }
            var tab by remember { mutableIntStateOf(TAB_HOME) }
            // the favorites change on the schedule screen, so the list is read again when the home screen appears
            val favoriteList = remember(tab, selected) { favorites.list() }
            LaunchedEffect(notes, selected?.name) { notes.setSchedule(selected?.name) }
            // the home screen is loaded again every time it appears, so "now" is not stale
            LaunchedEffect(dashboard, selected, tab) { if (tab == TAB_HOME) dashboard.show(selected) }
            // when the app is opened and when the chosen schedule changes: the changes in the schedule are looked for and the
            // reminders are planned again by the fresh schedule; if nothing is chosen, the reminders of the old one go away
            LaunchedEffect(reminders, selected) {
                reminders.attach(selected)
                if (selected == null) reminders.sync(null) else dependencies.backgroundSync.run()
            }
            LaunchedEffect(schedule) { schedule.start() }
            LaunchedEffect(barsController) { barsController.start() }

            if (showSettings) {
                SettingsScreen(
                    reminders,
                    hasSchedule = selected != null,
                    themeMode = themeMode,
                    onThemeChange = {
                        themeMode = it
                        dependencies.themeSettings.mode = it
                    },
                    onBack = { showSettings = false },
                )
                return@Surface
            }
            Scaffold(
                // the insets are applied once, below: the bar takes the bottom one itself
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = { AppNavigationBar(selectedTab = tab, onSelect = { tab = it }) },
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        // the status bar (the Dynamic Island) and the sides; not safeContent: it has the zones of the gestures
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                ) {
                    when (tab) {
                        TAB_HOME -> DashboardScreen(
                            dashboard,
                            notes,
                            favorites = favoriteList,
                            // the search of the schedule opens at once; the group that was chosen stays until another one is picked
                            onChooseSchedule = {
                                schedule.startSearch()
                                tab = TAB_SCHEDULE
                            },
                            onSelectFavorite = schedule::switchTo,
                            onOpenSettings = { showSettings = true },
                            onOpenNotes = { tab = TAB_NOTES },
                            onOpenMap = { tab = TAB_MAP },
                        )
                        TAB_SCHEDULE -> ScheduleScreen(schedule, notes)
                        TAB_NOTES -> NotesScreen(notes, selected?.name)
                        TAB_MAP -> MapScreen(mapController, tileLoader)
                        else -> BarsScreen(barsController)
                    }
                }
            }
        }
    }
}
