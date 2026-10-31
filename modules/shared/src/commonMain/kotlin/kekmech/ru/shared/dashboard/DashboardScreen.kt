package kekmech.ru.shared.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_notes_shared.Note
import kekmech.ru.feature_notes_shared.NotesController
import kekmech.ru.feature_schedule_shared.DashboardController
import kekmech.ru.feature_schedule_shared.DashboardUiState
import kekmech.ru.feature_schedule_shared.ScheduleType
import kekmech.ru.feature_schedule_shared.SelectedSchedule
import kekmech.ru.feature_schedule_shared.UPCOMING_DAYS
import kekmech.ru.feature_schedule_shared.UpcomingClass
import kekmech.ru.feature_schedule_shared.WeekOfSemester
import kekmech.ru.feature_schedule_shared.firstDayClasses
import kekmech.ru.feature_schedule_shared.formatTimeLeft
import kekmech.ru.feature_schedule_shared.isLunchTime
import kekmech.ru.feature_schedule_shared.minutesUntilStart
import kekmech.ru.feature_schedule_shared.upcomingCaption
import kekmech.ru.shared.ClassCard
import kekmech.ru.shared.NoteCard
import kekmech.ru.shared.schedule.dayTitle
import kekmech.ru.shared.schedule.formatTime
import kotlinx.datetime.LocalDateTime

/**
 * The home screen, the same blocks as on Android: the day and the week of the semester, the search, the chosen schedule,
 * the lunch break, the classes that go now and come next with the time left, the notes of the week and the favorites.
 *
 * @param favorites the schedules that the student has marked, to switch between them with a tap
 */
@Composable
public fun DashboardScreen(
    controller: DashboardController,
    notes: NotesController,
    favorites: List<SelectedSchedule>,
    onChooseSchedule: () -> Unit,
    onSelectFavorite: (SelectedSchedule) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val state by controller.state.collectAsState()
    val noteList by notes.notes.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val current = state) {
            DashboardUiState.NoSchedule -> {
                item { TopBar(title = "Главная", subtitle = null, onOpenSettings = onOpenSettings) }
                item { SearchField(onChooseSchedule) }
                item {
                    Text("Выберите группу или преподавателя, чтобы видеть ближайшие пары.", style = MaterialTheme.typography.bodyLarge)
                }
                item { Button(onClick = onChooseSchedule) { Text("Выбрать расписание") } }
            }
            is DashboardUiState.Loading -> {
                item { TopBar(title = "Главная", subtitle = null, onOpenSettings = onOpenSettings) }
                item { SearchField(onChooseSchedule) }
                item { ScheduleCard(current.selected, onChooseSchedule) }
                item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
            is DashboardUiState.Failed -> {
                item { TopBar(title = "Главная", subtitle = null, onOpenSettings = onOpenSettings) }
                item { SearchField(onChooseSchedule) }
                item { ScheduleCard(current.selected, onChooseSchedule) }
                item { Text(current.message, color = MaterialTheme.colorScheme.error) }
                item { Button(onClick = controller::refresh) { Text("Повторить") } }
            }
            is DashboardUiState.Content -> contentItems(
                state = current,
                notes = noteList,
                favorites = favorites,
                onChooseSchedule = onChooseSchedule,
                onSelectFavorite = onSelectFavorite,
                onOpenSettings = onOpenSettings,
                onOpenNotes = onOpenNotes,
                onOpenMap = onOpenMap,
            )
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.contentItems(
    state: DashboardUiState.Content,
    notes: List<Note>,
    favorites: List<SelectedSchedule>,
    onChooseSchedule: () -> Unit,
    onSelectFavorite: (SelectedSchedule) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenMap: () -> Unit,
) {
    item { TopBar(title = dayTitle(state.today), subtitle = weekText(state.weekOfSemester), onOpenSettings = onOpenSettings) }
    item { SearchField(onChooseSchedule) }
    item { ScheduleCard(state.selected, onChooseSchedule) }
    if (state.offline) {
        item {
            Text(
                "Нет сети, показана сохранённая копия",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    if (isLunchTime(state.now.time)) {
        item { LunchBanner(onOpenMap) }
    }

    item { SectionHeader("Ближайшие события", subtitle = state.upcoming.firstOrNull()?.let { upcomingCaption(state.today, it.date) }) }
    if (state.upcoming.isEmpty()) {
        item { EmptyState("Пока ничего интересного", "На ближайшие $UPCOMING_DAYS дней пар нет") }
    } else {
        val firstDay = firstDayClasses(state.upcoming)
        val first = state.upcoming.first()
        val left = formatTimeLeft(minutesUntilStart(state.now, first))
        if (!first.inProgress && left.isNotEmpty()) {
            item { Text("Пара начнется через $left", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary) }
        }
        items(firstDay) { upcoming ->
            UpcomingView(upcoming, today = upcoming.date == state.today, note = noteOf(upcoming, notes))
        }
    }

    val actual = NotesController.actual(notes, state.today)
    item { SectionHeader("Заметки", action = "ПОКАЗАТЬ ВСЕ", onAction = onOpenNotes) }
    if (actual.isEmpty()) {
        item { EmptyState("Нет ни одной актуальной заметки", "Для создания заметки нажмите на любую пару в разделе расписания") }
    } else {
        items(actual) { note -> NoteCard(note, onClick = onOpenNotes) }
    }

    if (favorites.isNotEmpty()) {
        item {
            SectionHeader(
                "Избранные расписания",
                subtitle = "Нажмите для переключения",
                action = "НАСТРОИТЬ",
                onAction = onChooseSchedule,
            )
        }
        items(favorites) { favorite ->
            FavoriteRow(
                favorite = favorite,
                selected = favorite.name.equals(state.selected.name, ignoreCase = true),
                onClick = { onSelectFavorite(favorite) },
            )
        }
    }
}

private fun weekText(week: WeekOfSemester?): String? = when (week) {
    is WeekOfSemester.Studying -> "Идёт ${week.num}-я учебная неделя"
    WeekOfSemester.NonStudying -> "Неучебная неделя"
    null -> null
}

@Composable
private fun TopBar(title: String, subtitle: String?, onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** The field that looks like the search of Android: a tap opens the search of the schedule. */
@Composable
private fun SearchField(onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            "Искать в МЭИ…",
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The chosen schedule: a tap on it opens the search of another one, as on Android. */
@Composable
private fun ScheduleCard(selected: SelectedSchedule, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                if (selected.type == ScheduleType.GROUP) "Группа ${selected.name}" else selected.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Нажмите, чтобы сменить расписание",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LunchBanner(onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Судя по всему, сейчас обеденный перерыв", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Найти столовые на карте", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (action != null) {
            TextButton(onClick = onAction) { Text(action, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UpcomingView(upcoming: UpcomingClass, today: Boolean, note: Note?) {
    val dayLabel = if (today) "Сегодня" else dayTitle(upcoming.date)
    ClassCard(
        classes = upcoming.classes,
        note = note,
        caption = if (upcoming.inProgress) "$dayLabel · идёт сейчас" else dayLabel,
        highlighted = upcoming.inProgress,
    )
}

@Composable
private fun FavoriteRow(favorite: SelectedSchedule, selected: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                favorite.name,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun noteOf(upcoming: UpcomingClass, notes: List<Note>): Note? {
    val dateTime = LocalDateTime(upcoming.date, upcoming.classes.time.start)
    return notes.firstOrNull { it.classesName == upcoming.classes.name && it.dateTime == dateTime }
}
