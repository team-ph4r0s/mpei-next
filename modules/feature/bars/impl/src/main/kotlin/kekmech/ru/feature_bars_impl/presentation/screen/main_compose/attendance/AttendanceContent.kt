package kekmech.ru.feature_bars_impl.presentation.screen.main_compose.attendance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_bars_impl.domain.AttendanceKind
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.summary.Chip
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.summary.SummaryCardSkeleton
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.summary.SummaryError
import kekmech.ru.feature_bars_impl.presentation.screen.main_compose.summary.cardModifier
import kekmech.ru.feature_bars_shared.AttendanceRisk
import kekmech.ru.feature_bars_shared.AttendanceSummary
import kekmech.ru.feature_bars_shared.DisciplineAttendance
import kekmech.ru.lib_bars_parser.BarsAttendanceRecord
import kekmech.ru.lib_bars_parser.BarsAttendanceStatus
import kekmech.ru.lib_elm.Resource
import kekmech.ru.ui_theme.theme.MpeixTheme
import kekmech.ru.res_strings.R.string as Strings

private const val SKELETON_COUNT = 3

/** The tab "Посещаемость": the summary of the semester, then a card for every discipline. */
internal fun LazyListScope.attendanceItems(
    attendance: Resource<AttendanceSummary>,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when (attendance) {
        is Resource.Loading -> items(SKELETON_COUNT) { SummaryCardSkeleton(Modifier.cardModifier()) }

        is Resource.Error -> item(key = "attendance_error") {
            SummaryError(
                throwable = attendance.error,
                onRetry = onRetry,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
        }

        is Resource.Data -> {
            val summary = attendance.value
            if (!summary.hasRecords) {
                item(key = "attendance_empty") {
                    Text(
                        text = stringResource(Strings.attendance_empty),
                        style = MpeixTheme.typography.paragraphNormal,
                        color = MpeixTheme.palette.contentDisabled,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                    )
                }
            } else {
                item(key = "attendance_header") { AttendanceHeader(summary, Modifier.cardModifier()) }
                item(key = "attendance_hint") {
                    Text(
                        text = stringResource(Strings.attendance_rule_hint),
                        style = MpeixTheme.typography.labelNormal,
                        color = MpeixTheme.palette.contentDisabled,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                // the disciplines are grouped by name, so the name is unique
                items(summary.disciplines, key = { "attendance_${it.name}" }) { discipline ->
                    AttendanceDisciplineCard(
                        discipline = discipline,
                        isExpanded = discipline.name in expanded,
                        onClick = { onToggle(discipline.name) },
                        modifier = Modifier.cardModifier(),
                    )
                }
            }
        }
    }
}

/** The worst level of the disciplines: the ring of the semester has its color. */
internal fun AttendanceSummary.overallRisk(): AttendanceRisk =
    disciplines.maxOfOrNull { it.risk } ?: AttendanceRisk.OK

/** The colors of a level: the text and the background; green, yellow, red. */
@Composable
internal fun AttendanceRisk.colors(): Pair<Color, Color> {
    val palette = MpeixTheme.palette
    return when (this) {
        AttendanceRisk.OK -> palette.greenMarkColor to palette.greenBgMarkColor
        AttendanceRisk.WARNING -> palette.yellowMarkColor to palette.yellowBgMarkColor
        AttendanceRisk.CRITICAL -> palette.redMarkColor to palette.redBgMarkColor
    }
}

@Composable
internal fun AttendanceHeader(summary: AttendanceSummary, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val (accent, _) = summary.overallRisk().colors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MpeixTheme.palette.surface, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AttendanceRing(percent = summary.percent, color = accent)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Strings.attendance_semester_title),
                    style = MpeixTheme.typography.paragraphBigAccent,
                    color = MpeixTheme.palette.content,
                )
                Text(
                    text = stringResource(Strings.attendance_progress, summary.present + summary.excused, summary.total),
                    style = MpeixTheme.typography.paragraphNormal,
                    color = MpeixTheme.palette.contentVariant,
                )
            }
        }
        // three numbers under the ring: they do not fit next to it on a phone
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat(summary.present, Strings.attendance_stat_present, MpeixTheme.palette.greenMarkColor)
            Stat(summary.excused, Strings.attendance_stat_excused, MpeixTheme.palette.yellowMarkColor)
            Stat(summary.absent, Strings.attendance_stat_absent, MpeixTheme.palette.redMarkColor)
        }
    }
}

@Composable
private fun Stat(value: Int, label: Int, color: Color) {
    Column {
        Text(text = value.toString(), style = MpeixTheme.typography.header4, color = color)
        Text(text = stringResource(label), style = MpeixTheme.typography.labelNormal, color = MpeixTheme.palette.contentVariant)
    }
}

/** A ring of the per cent of the attendance; the arc grows to its value when it appears. */
@Composable
private fun AttendanceRing(percent: Int, color: Color, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(targetValue = percent.coerceIn(0, 100) / 100f, label = "attendance_ring")
    val track = MpeixTheme.palette.surfacePlus3
    val description = stringResource(Strings.attendance_ring_description, percent)
    Box(
        modifier = modifier
            .size(RING_SIZE)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = RING_STROKE.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arc,
                style = Stroke(stroke),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arc,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(text = "$percent%", style = MpeixTheme.typography.header4, color = MpeixTheme.palette.content)
    }
}

private val RING_SIZE = 88.dp
private val RING_STROKE = 10.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AttendanceDisciplineCard(
    discipline: DisciplineAttendance,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val chevronRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "attendance_chevron")
    val (accent, accentBackground) = discipline.risk.colors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MpeixTheme.palette.surface, shape)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = discipline.name,
                        style = MpeixTheme.typography.paragraphBig,
                        color = MpeixTheme.palette.content,
                    )
                    Text(
                        text = listOfNotNull(
                            discipline.assessmentType?.replaceFirstChar { it.uppercase() },
                            stringResource(
                                Strings.attendance_progress,
                                discipline.present + discipline.excused,
                                discipline.total,
                            ),
                        ).joinToString(" • "),
                        style = MpeixTheme.typography.paragraphNormal,
                        color = MpeixTheme.palette.contentDisabled,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (isExpanded) Strings.attendance_collapse else Strings.attendance_expand,
                    ),
                    tint = MpeixTheme.palette.contentVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp)
                        .rotate(chevronRotation),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AttendanceBar(
                    fraction = discipline.percent / 100f,
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${discipline.percent}%",
                    style = MpeixTheme.typography.paragraphNormalAccent,
                    color = accent,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                // The level is told with an icon and a word, not with a color only
                Chip(
                    text = stringResource(
                        when (discipline.risk) {
                            AttendanceRisk.OK -> Strings.attendance_risk_ok
                            AttendanceRisk.WARNING -> Strings.attendance_risk_warning
                            AttendanceRisk.CRITICAL -> Strings.attendance_risk_critical
                        },
                    ),
                    contentColor = accent,
                    containerColor = accentBackground,
                    icon = when (discipline.risk) {
                        AttendanceRisk.OK -> Icons.Default.CheckCircle
                        AttendanceRisk.WARNING -> Icons.Default.Info
                        AttendanceRisk.CRITICAL -> Icons.Default.Warning
                    },
                )
                if (discipline.absent > 0) {
                    Text(
                        text = "${stringResource(Strings.attendance_stat_absent)}: ${discipline.absent}",
                        style = MpeixTheme.typography.labelNormal,
                        color = MpeixTheme.palette.contentVariant,
                    )
                }
                if (discipline.excused > 0) {
                    Text(
                        text = "${stringResource(Strings.attendance_stat_excused)}: ${discipline.excused}",
                        style = MpeixTheme.typography.labelNormal,
                        color = MpeixTheme.palette.contentVariant,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .background(MpeixTheme.palette.surfacePlus1)
                    .padding(vertical = 4.dp),
            ) {
                discipline.records.forEach { record -> AttendanceRecordRow(record) }
            }
        }
    }
}

@Composable
private fun AttendanceBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(shape)
            .background(MpeixTheme.palette.surfacePlus3),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(color, shape),
        )
    }
}

/** A class of the timeline: the date, the number of the class and its time, the kind, the badge of the status. */
@Composable
private fun AttendanceRecordRow(record: BarsAttendanceRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(DATE_WIDTH)) {
            Text(
                // "28.10": the year is the one of the semester
                text = record.date.substringBeforeLast('.'),
                style = MpeixTheme.typography.paragraphNormalAccent,
                color = MpeixTheme.palette.content,
            )
            record.pair?.let {
                Text(
                    text = stringResource(Strings.attendance_pair, it),
                    style = MpeixTheme.typography.labelNormal,
                    color = MpeixTheme.palette.contentVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(AttendanceKind.of(record.kind).label),
                style = MpeixTheme.typography.paragraphNormal,
                color = MpeixTheme.palette.content,
            )
            val details = listOfNotNull(
                if (record.timeStart != null && record.timeEnd != null) "${record.timeStart}–${record.timeEnd}" else null,
                record.teacher.takeIf { it.isNotBlank() },
            ).joinToString(" • ")
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MpeixTheme.typography.labelNormal,
                    color = MpeixTheme.palette.contentDisabled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        StatusBadge(record.status)
    }
}

private val DATE_WIDTH = 56.dp

/** The status is an icon and a word; the colors are those of the levels. */
@Composable
internal fun StatusBadge(status: BarsAttendanceStatus) {
    val palette = MpeixTheme.palette
    val (icon, label, colors) = when (status) {
        BarsAttendanceStatus.PRESENT -> Triple<ImageVector, Int, Pair<Color, Color>>(
            Icons.Default.CheckCircle,
            Strings.attendance_status_present,
            palette.greenMarkColor to palette.greenBgMarkColor,
        )

        BarsAttendanceStatus.EXCUSED -> Triple(
            Icons.Default.Info,
            Strings.attendance_status_excused,
            palette.yellowMarkColor to palette.yellowBgMarkColor,
        )

        // a class of an unknown status is shown as a skip only when BARS says so: the analysis does not take it in
        BarsAttendanceStatus.ABSENT, BarsAttendanceStatus.UNKNOWN -> Triple(
            Icons.Default.Warning,
            Strings.attendance_status_absent,
            palette.redMarkColor to palette.redBgMarkColor,
        )
    }
    Chip(
        text = stringResource(label),
        contentColor = colors.first,
        containerColor = colors.second,
        icon = icon,
    )
}
