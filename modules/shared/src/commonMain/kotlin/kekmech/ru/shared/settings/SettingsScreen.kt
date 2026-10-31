package kekmech.ru.shared.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_schedule_shared.ReminderKind
import kekmech.ru.feature_schedule_shared.RemindersController
import kekmech.ru.lib_settings.ThemeMode
import kekmech.ru.ui_kit_segmented.SegmentedControl
import kekmech.ru.ui_kit_switch.Switch
import kekmech.ru.ui_kit_topappbar.TopAppBar

private const val REPO_URL = "https://github.com/team-ph4r0s/mpei-next"
private const val ORIGINAL_REPO_URL = "https://github.com/tonykolomeytsev/mpeiapp"

/**
 * The settings, in the sections of the Android screen: the theme, the notifications (the changes in the schedule and the
 * reminders about the practices and the laboratory works) and the links of the project.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun SettingsScreen(
    reminders: RemindersController,
    hasSchedule: Boolean,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    val state by reminders.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        // the insets are applied once, by the column
        TopAppBar(title = "Настройки", navigationIcon = { BackIconButton(onBack) }, windowInsets = WindowInsets(0, 0, 0, 0))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Section("Оформление") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Тема оформления", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Светлая, тёмная или как в настройках системы",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SegmentedControl(
                        options = ThemeMode.entries,
                        selected = themeMode,
                        onSelect = onThemeChange,
                        label = { mode -> themeTitle(mode) },
                        modifier = Modifier,
                        role = Role.RadioButton,
                    )
                }
            }
            Section("Уведомления") {
                SwitchRow(
                    title = "Изменения в расписании",
                    description = "Уведомление, когда в ближайших трёх неделях пару заменили, отменили или перенесли. " +
                        "iOS сама выбирает, когда приложению проверять расписание, обычно несколько раз в день.",
                    checked = state.changeAlerts,
                    onChange = reminders::setChangeAlerts,
                )
                HorizontalDivider()
                SwitchRow(
                    title = "Напоминания о занятиях",
                    description = "Практические и лабораторные занятия. Ночью (с 23:00 до 07:00) уведомления не приходят: " +
                        "они переносятся на 07:00.",
                    checked = state.enabled,
                    onChange = reminders::setEnabled,
                )
                if (state.enabled) {
                    HorizontalDivider()
                    SwitchRow(
                        title = "За 24 часа до начала",
                        checked = state.dayBefore,
                        onChange = { reminders.setKindEnabled(ReminderKind.DAY_BEFORE, it) },
                    )
                    HorizontalDivider()
                    SwitchRow(
                        title = "За 5 часов до начала",
                        checked = state.hoursBefore,
                        onChange = { reminders.setKindEnabled(ReminderKind.HOURS_BEFORE, it) },
                    )
                }
            }
            if (state.permissionDenied) {
                Hint(
                    "Уведомления для МЭИ.Хаб отключены, напоминания не будут показываться. " +
                        "Включите их в Настройках iPhone: Уведомления → МЭИ.Хаб.",
                    error = true,
                )
            }
            if (state.enabled) {
                Hint(
                    when {
                        !hasSchedule -> "Выберите группу или преподавателя на вкладке «Расписание»."
                        state.planned == 0 -> "Подходящих занятий в ближайшие три недели нет."
                        else -> "Запланировано напоминаний: ${state.planned}. Они обновляются при каждом открытии приложения."
                    },
                )
            }
            Section("О приложении") {
                LinkRow("Репозиторий MPEI.Hub", "github.com/team-ph4r0s/mpei-next") { uriHandler.openUri(REPO_URL) }
                HorizontalDivider()
                LinkRow("Завести Issue или Pull Request", "Ошибки и пожелания") { uriHandler.openUri("$REPO_URL/issues") }
                HorizontalDivider()
                LinkRow("Оригинальный проект (MpeiX)", "github.com/tonykolomeytsev/mpeiapp") { uriHandler.openUri(ORIGINAL_REPO_URL) }
            }
        }
    }
}

private fun themeTitle(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Системная"
    ThemeMode.LIGHT -> "Светлая"
    ThemeMode.DARK -> "Тёмная"
}

/** A group of rows on one card, with the title over it. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        Modifier.padding(start = 4.dp, top = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(content = content)
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, description: String? = null) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Hint(text: String, error: Boolean = false) {
    Text(
        text,
        Modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
