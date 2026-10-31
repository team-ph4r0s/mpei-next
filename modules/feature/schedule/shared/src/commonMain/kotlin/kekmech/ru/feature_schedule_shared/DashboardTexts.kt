package kekmech.ru.feature_schedule_shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil

private const val MINUTES_IN_HOUR = 60L
private const val MINUTES_IN_DAY = 24 * MINUTES_IN_HOUR
private const val HOURS_IN_DAY = 24L
private const val TEEN_FROM = 11L
private const val TEEN_TO = 19L
private const val FEW_FROM = 2L
private const val FEW_TO = 4L

private val LUNCH_START = LocalTime(12, 45)
private val LUNCH_END = LocalTime(13, 45)

/** The word for [n] in Russian: `1 час`, `2 часа`, `5 часов`. */
public fun pluralRu(n: Long, one: String, few: String, many: String): String {
    val form = when {
        n % 100 in TEEN_FROM..TEEN_TO -> many
        n % 10 == 1L -> one
        n % 10 in FEW_FROM..FEW_TO -> few
        else -> many
    }
    return "$n $form"
}

/**
 * The time to the start of a class as the home screen says it: `2 часа 5 минут`, with the days when it is a day or more
 * (`1 день 3 часа`). The parts that are zero are left out; less than a minute is an empty string.
 */
public fun formatTimeLeft(totalMinutes: Long): String {
    if (totalMinutes <= 0) return ""
    val days = totalMinutes / MINUTES_IN_DAY
    val hours = if (days > 0) (totalMinutes / MINUTES_IN_HOUR) % HOURS_IN_DAY else totalMinutes / MINUTES_IN_HOUR
    val minutes = totalMinutes % MINUTES_IN_HOUR
    return listOfNotNull(
        pluralRu(days, "день", "дня", "дней").takeIf { days > 0 },
        pluralRu(hours, "час", "часа", "часов").takeIf { hours > 0 },
        pluralRu(minutes, "минуту", "минуты", "минут").takeIf { minutes > 0 },
    ).joinToString(" ")
}

/** The whole minutes from [now] to the start of [upcoming]; zero when it has started. */
public fun minutesUntilStart(now: LocalDateTime, upcoming: UpcomingClass): Long {
    if (upcoming.inProgress) return 0
    val start = upcoming.classes.time.start
    val days = now.date.daysUntil(upcoming.date).toLong()
    val minutes = (start.hour - now.hour) * MINUTES_IN_HOUR + (start.minute - now.minute)
    return (days * MINUTES_IN_DAY + minutes).coerceAtLeast(0)
}

/** The subtitle of the section of the next classes: when the first of them is. */
public fun upcomingCaption(today: LocalDate, firstDate: LocalDate): String = when (val ahead = today.daysUntil(firstDate)) {
    0 -> "Сегодняшние пары"
    1 -> "Завтрашние пары"
    2 -> "Пары послезавтра"
    else -> "Пары через ${pluralRu(ahead.toLong(), "день", "дня", "дней")}"
}

/** `true` at the lunch break of the university, when the home screen offers to look for a canteen. */
public fun isLunchTime(time: LocalTime): Boolean = time >= LUNCH_START && time <= LUNCH_END

/** The classes of the first day that has any: the home screen shows that day only, as Android does. */
public fun firstDayClasses(upcoming: List<UpcomingClass>): List<UpcomingClass> {
    val firstDate = upcoming.firstOrNull()?.date ?: return emptyList()
    return upcoming.filter { it.date == firstDate }
}
