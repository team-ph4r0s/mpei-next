package kekmech.ru.feature_schedule_shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DashboardTextsTest {

    private fun upcoming(date: LocalDate, start: LocalTime, inProgress: Boolean = false) = UpcomingClass(
        date = date,
        classes = Classes(
            name = "Физика",
            type = ClassesType.LECTURE,
            rawType = null,
            place = "Е-101",
            groups = "",
            person = "",
            time = Time(start, LocalTime(start.hour + 1, start.minute)),
            number = 2,
        ),
        inProgress = inProgress,
    )

    @Test
    fun `plural forms follow the russian rules`() {
        assertEquals("1 час", pluralRu(1, "час", "часа", "часов"))
        assertEquals("2 часа", pluralRu(2, "час", "часа", "часов"))
        assertEquals("5 часов", pluralRu(5, "час", "часа", "часов"))
        assertEquals("11 часов", pluralRu(11, "час", "часа", "часов"))
        assertEquals("21 час", pluralRu(21, "час", "часа", "часов"))
        assertEquals("112 часов", pluralRu(112, "час", "часа", "часов"))
    }

    @Test
    fun `time left shows hours and minutes`() {
        assertEquals("1 час 5 минут", formatTimeLeft(65))
        assertEquals("2 часа", formatTimeLeft(120))
        assertEquals("1 минуту", formatTimeLeft(1))
        assertEquals("21 минуту", formatTimeLeft(21))
    }

    @Test
    fun `time left shows days when it is a day or more`() {
        assertEquals("1 день 3 часа", formatTimeLeft(24 * 60 + 3 * 60))
        assertEquals("2 дня 1 час 1 минуту", formatTimeLeft(2 * 24 * 60 + 61))
    }

    @Test
    fun `time left is empty when nothing is left`() {
        assertEquals("", formatTimeLeft(0))
        assertEquals("", formatTimeLeft(-5))
    }

    @Test
    fun `minutes until start count across the days`() {
        val now = LocalDateTime(2026, 10, 30, 12, 0)
        assertEquals(105, minutesUntilStart(now, upcoming(LocalDate(2026, 10, 30), LocalTime(13, 45))))
        assertEquals(24 * 60 + 9 * 60 + 20 - 12 * 60, minutesUntilStart(now, upcoming(LocalDate(2026, 10, 31), LocalTime(9, 20))))
    }

    @Test
    fun `minutes until start are zero for a class that goes now`() {
        val now = LocalDateTime(2026, 10, 30, 10, 0)
        assertEquals(0, minutesUntilStart(now, upcoming(LocalDate(2026, 10, 30), LocalTime(9, 20), inProgress = true)))
    }

    @Test
    fun `caption tells when the first class is`() {
        val today = LocalDate(2026, 10, 30)
        assertEquals("Сегодняшние пары", upcomingCaption(today, today))
        assertEquals("Завтрашние пары", upcomingCaption(today, LocalDate(2026, 10, 31)))
        assertEquals("Пары послезавтра", upcomingCaption(today, LocalDate(2026, 11, 1)))
        assertEquals("Пары через 3 дня", upcomingCaption(today, LocalDate(2026, 11, 2)))
        assertEquals("Пары через 5 дней", upcomingCaption(today, LocalDate(2026, 11, 4)))
    }

    @Test
    fun `the home screen shows the first day with classes only`() {
        val monday = LocalDate(2026, 11, 2)
        val tuesday = LocalDate(2026, 11, 3)
        val list = listOf(
            upcoming(monday, LocalTime(9, 20)),
            upcoming(monday, LocalTime(11, 10)),
            upcoming(tuesday, LocalTime(9, 20)),
        )

        assertEquals(list.take(2), firstDayClasses(list))
        assertEquals(emptyList(), firstDayClasses(emptyList()))
    }

    @Test
    fun `lunch time is a break in the middle of the day`() {
        assertTrue(isLunchTime(LocalTime(12, 45)))
        assertTrue(isLunchTime(LocalTime(13, 0)))
        assertTrue(isLunchTime(LocalTime(13, 45)))
        assertFalse(isLunchTime(LocalTime(12, 44)))
        assertFalse(isLunchTime(LocalTime(13, 46)))
    }
}
