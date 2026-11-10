package kekmech.ru.shared.bars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_bars_shared.AttendanceRisk
import kekmech.ru.feature_bars_shared.DisciplineAttendance
import kekmech.ru.feature_bars_shared.AttendanceSummary
import kekmech.ru.feature_bars_shared.BarsTab
import kekmech.ru.feature_bars_shared.BarsUiState
import kekmech.ru.feature_bars_shared.controlActivities
import kekmech.ru.feature_bars_shared.currentScore
import kekmech.ru.feature_bars_shared.finalMark
import kekmech.ru.feature_bars_shared.formatMark
import kekmech.ru.feature_bars_shared.summaryActivities
import kekmech.ru.lib_bars_parser.BarsAttendanceRecord
import kekmech.ru.lib_bars_parser.BarsAttendanceStatus
import kekmech.ru.lib_bars_parser.BarsDiscipline
import kekmech.ru.lib_bars_parser.BarsRating
import kekmech.ru.lib_bars_parser.BarsSummaryRow
import kekmech.ru.ui_kit_segmented.SegmentedControl

@Composable
internal fun ContentView(
    state: BarsUiState.Content,
    onSemester: (String) -> Unit,
    onTab: (BarsTab) -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.studentName ?: "БАРС", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                state.group?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            TextButton(onClick = onSignOut) { Text("Выйти") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SemesterPicker(state, onSemester, Modifier.weight(1f))
            TextButton(onClick = onRefresh, enabled = !state.loading) { Text("Обновить") }
        }

        SegmentedControl(
            options = BarsTab.entries,
            selected = state.tab,
            onSelect = onTab,
            label = { tab ->
                when (tab) {
                    BarsTab.MARKS -> "Оценки"
                    BarsTab.SUMMARY -> "Сводка"
                    BarsTab.ATTENDANCE -> "Посещаемость"
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { Box(Modifier.padding(horizontal = 16.dp)) { ErrorText(it) } }

        when (state.tab) {
            BarsTab.MARKS -> MarksList(state)
            BarsTab.SUMMARY -> SummaryList(state)
            BarsTab.ATTENDANCE -> AttendanceList(state)
        }
    }
}

/** A card of the BARS lists: the same look as the cards of the classes. */
@Composable
private fun BarsCard(modifier: Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, content = content)
}

@Composable
private fun SemesterPicker(state: BarsUiState.Content, onSemester: (String) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text(state.semesterName ?: "Семестр")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            state.semesters.forEach { semester ->
                DropdownMenuItem(
                    text = { Text(semester.name) },
                    onClick = {
                        open = false
                        onSemester(semester.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun MarksList(state: BarsUiState.Content) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.rating?.let { rating -> item { RatingCard(rating) } }
        if (state.disciplines.isEmpty() && !state.loading) {
            item { Text("В этом семестре пока нет дисциплин", style = MaterialTheme.typography.bodyLarge) }
        }
        items(state.disciplines) { DisciplineCard(it) }
    }
}

@Composable
private fun RatingCard(rating: BarsRating) {
    BarsCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Рейтинг", style = MaterialTheme.typography.titleMedium)
            rating.items.forEach { Text("${it.label}: ${it.raw}", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun DisciplineCard(discipline: BarsDiscipline) {
    var expanded by remember { mutableStateOf(false) }
    BarsCard(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(discipline.name, style = MaterialTheme.typography.titleMedium)
            Text(
                listOf(discipline.person, discipline.assessmentType, discipline.units)
                    .filter { it.isNotEmpty() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
            discipline.deadline?.let { Text("Сдать до $it", style = MaterialTheme.typography.bodySmall) }
            if (discipline.notes.isNotEmpty()) {
                Text(discipline.notes.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Балл: ${formatMark(discipline.currentScore)}", style = MaterialTheme.typography.bodyMedium)
                Text("Итог: ${formatMark(discipline.finalMark)}", style = MaterialTheme.typography.bodyMedium)
            }
            if (expanded) {
                discipline.controlActivities.forEach { activity ->
                    val mark = activity.mark
                    val retaken = if (mark != null && mark.isRetaken) " (пересдача)" else ""
                    Text(activity.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        listOfNotNull(
                            activity.weight?.let { "вес $it%" },
                            activity.weekNum?.let { "неделя $it" },
                            "оценка ${formatMark(mark)}$retaken",
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                discipline.summaryActivities.forEach { activity ->
                    Text("${activity.name} ${formatMark(activity.mark)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SummaryList(state: BarsUiState.Content) {
    val summary = state.summary
    if (summary == null) {
        if (!state.loading) Box(Modifier.padding(16.dp)) { Text("Сводка не загружена") }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (summary.rows.isEmpty()) {
            item { Text("В этом семестре пока нет оценок", style = MaterialTheme.typography.bodyLarge) }
        }
        items(summary.rows) { SummaryCard(it, summary.currentWeek) }
    }
}

@Composable
private fun SummaryCard(row: BarsSummaryRow, currentWeek: Int?) {
    BarsCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.discipline, style = MaterialTheme.typography.titleMedium)
            row.assessmentType?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            row.weeks.forEach { week ->
                val marks = week.marks.joinToString(" ") { formatMark(it.mark) }
                val now = if (week.week == currentWeek) " · сейчас" else ""
                Text("Неделя ${week.week}$now: $marks", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "ТБ ${formatMark(row.currentScore)} · ПА ${formatMark(row.intermediate)} · И ${formatMark(row.finalMark)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AttendanceList(state: BarsUiState.Content) {
    var expanded by remember { mutableStateOf(emptySet<String>()) }
    val attendance = state.attendance
    if (attendance == null) {
        if (!state.loading) Box(Modifier.padding(16.dp)) { Text("Посещаемость не загружена") }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!attendance.hasRecords) {
            item { Text("Пока нет отметок о посещаемости в этом семестре", style = MaterialTheme.typography.bodyLarge) }
            return@LazyColumn
        }
        item { AttendanceHeader(attendance) }
        item {
            Text(
                "Риск недопуска: пропущена четверть занятий по предмету или 3 занятия подряд. " +
                    "Пропуски по уважительной причине не считаются.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items(attendance.disciplines, key = { it.name }) { discipline ->
            val isExpanded = discipline.name in expanded
            AttendanceCard(
                discipline = discipline,
                isExpanded = isExpanded,
                onClick = { expanded = if (isExpanded) expanded - discipline.name else expanded + discipline.name },
            )
        }
    }
}

@Composable
private fun AttendanceHeader(attendance: AttendanceSummary) {
    BarsCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Посещаемость за семестр", style = MaterialTheme.typography.titleMedium)
            Text("${attendance.percent}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { attendance.percent / 100f }, modifier = Modifier.fillMaxWidth())
            Text(
                "Посещено ${attendance.present + attendance.excused} из ${attendance.total}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Посещено: ${attendance.present} · Уважительные: ${attendance.excused} · Пропуски: ${attendance.absent}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AttendanceCard(discipline: DisciplineAttendance, isExpanded: Boolean, onClick: () -> Unit) {
    BarsCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(discipline.name, style = MaterialTheme.typography.titleMedium)
            discipline.assessmentType?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${discipline.percent}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                // The level is a word, not a color only
                Text(riskLabel(discipline.risk), style = MaterialTheme.typography.bodyMedium, color = riskColor(discipline.risk))
            }
            LinearProgressIndicator(
                progress = { discipline.percent / 100f },
                color = riskColor(discipline.risk),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Посещено ${discipline.present + discipline.excused} из ${discipline.total}" +
                    (if (discipline.absent > 0) " · Пропуски: ${discipline.absent}" else "") +
                    (if (discipline.excused > 0) " · Уважительные: ${discipline.excused}" else ""),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                if (isExpanded) "Скрыть занятия" else "Показать занятия",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (isExpanded) {
                discipline.records.forEach { AttendanceRow(it) }
            }
        }
    }
}

@Composable
private fun AttendanceRow(record: BarsAttendanceRecord) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            listOfNotNull(
                // "28.10": the year is the one of the semester
                record.date.substringBeforeLast('.'),
                record.pair?.let { "$it пара" },
                record.kind.takeIf { it.isNotBlank() },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            statusLabel(record.status),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = statusColor(record.status),
        )
    }
}

private fun riskLabel(risk: AttendanceRisk): String = when (risk) {
    AttendanceRisk.OK -> "Всё хорошо"
    AttendanceRisk.WARNING -> "Есть пропуски"
    AttendanceRisk.CRITICAL -> "Риск недопуска"
}

private fun riskColor(risk: AttendanceRisk): Color = when (risk) {
    AttendanceRisk.OK -> Color(0xFF2E7D32)
    AttendanceRisk.WARNING -> Color(0xFFB26A00)
    AttendanceRisk.CRITICAL -> Color(0xFFC62828)
}

private fun statusLabel(status: BarsAttendanceStatus): String = when (status) {
    BarsAttendanceStatus.PRESENT -> "Был"
    BarsAttendanceStatus.EXCUSED -> "Уважительная причина"
    BarsAttendanceStatus.ABSENT, BarsAttendanceStatus.UNKNOWN -> "Пропуск / Н"
}

private fun statusColor(status: BarsAttendanceStatus): Color = when (status) {
    BarsAttendanceStatus.PRESENT -> riskColor(AttendanceRisk.OK)
    BarsAttendanceStatus.EXCUSED -> riskColor(AttendanceRisk.WARNING)
    BarsAttendanceStatus.ABSENT, BarsAttendanceStatus.UNKNOWN -> riskColor(AttendanceRisk.CRITICAL)
}
