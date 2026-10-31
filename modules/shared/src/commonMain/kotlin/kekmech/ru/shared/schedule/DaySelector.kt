package kekmech.ru.shared.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_schedule_shared.Week
import kotlinx.datetime.LocalDate

private val DAY_SHORT_NAMES = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
private const val SUNDAY_INDEX = 6

/** The day to show first: today if it is in the week, else the first day with classes. */
internal fun initialDayIndex(week: Week, today: LocalDate): Int =
    week.days.indexOfFirst { it.date == today }.takeIf { it >= 0 }
        ?: week.days.indexOfFirst { it.classes.isNotEmpty() }.takeIf { it >= 0 }
        ?: 0

/**
 * The days of the week in a row, as in the Android app: the short name, the date in a circle (the chosen day is filled, today
 * has a ring) and a dot under the days that have classes. Sunday is shown only when it has classes.
 */
@Composable
internal fun DaySelector(week: Week, today: LocalDate, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        week.days.forEachIndexed { index, day ->
            if (index == SUNDAY_INDEX && day.classes.isEmpty() && selected != index) return@forEachIndexed
            val isSelected = index == selected
            val isToday = day.date == today
            Column(
                Modifier.clip(CircleShape).clickable { onSelect(index) }.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    DAY_SHORT_NAMES[index],
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    Modifier.size(38.dp),
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = if (isToday && !isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    Modifier.size(5.dp),
                    shape = CircleShape,
                    color = if (day.classes.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                ) {}
            }
        }
    }
}
