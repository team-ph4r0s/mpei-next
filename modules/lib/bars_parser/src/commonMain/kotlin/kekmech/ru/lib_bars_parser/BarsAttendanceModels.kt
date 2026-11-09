package kekmech.ru.lib_bars_parser

/** What BARS says about a student at one class. */
enum class BarsAttendanceStatus {
    PRESENT,

    /** Skipped without a good reason: the one that counts against the student. */
    ABSENT,

    /** Skipped with a good reason ("по уважительной причине"). */
    EXCUSED,

    /** The page does not say it in a way that the parser knows; [BarsAttendanceRecord.badges] has the words of BARS. */
    UNKNOWN,
}

/**
 * One line of "Посещение занятий":
 * `28.10.26, 7 пара (20:30-22:00), лекция (Иванов И.И.), Физика (экзамен), ИЭозс-45-25` and the badge behind it.
 */
data class BarsAttendanceRecord(
    /** `data-ls-id` of the row: stays the same when BARS changes the record, so it tells a new record from an old one. */
    val id: String,
    /** "28.10.26" */
    val date: String,
    /** The number of the class of the day ("7 пара"), null if the line does not say. */
    val pair: Int?,
    /** "20:30" */
    val timeStart: String?,
    /** "22:00" */
    val timeEnd: String?,
    /** "лекция", "практическое занятие", "лабораторная работа" as BARS writes it. */
    val kind: String,
    val teacher: String,
    val discipline: String,
    /** "экзамен", "зачёт (без оценки)": the brackets behind the name of the discipline. */
    val assessmentType: String?,
    val group: String,
    val status: BarsAttendanceStatus,
    /** The words of the badges of the row ("пропуск"), as BARS writes them. */
    val badges: List<String>,
    /** The whole line, whitespace collapsed; what the fields were cut from. */
    val raw: String,
) {
    /** `yyyyMMdd` of [date] (the year is "26" or "2026"), 0 if the date is not a date: for sorting. */
    val dateKey: Int get() = attendanceDateKey(date)

    /** The classes of a day go one after another: the key puts the date first, the number of the class second. */
    val orderKey: Long get() = dateKey.toLong() * 100 + (pair ?: 0)

    fun withStatus(status: BarsAttendanceStatus): BarsAttendanceRecord = copy(status = status)
}

/** A page (or a part of it) of "Посещение занятий". */
data class BarsAttendancePage(
    /** The semesters of the filter, "все семестры" is not among them; the selected one is the one of the records. */
    val semesters: List<BarsSemester>,
    val records: List<BarsAttendanceRecord>,
    /** "из 26" of the pager: how many records match the filter in all the pages; null if the page has no pager. */
    val total: Int?,
    /** The number of this page of the pager, 1 if there is no pager. */
    val page: Int,
) {
    val selectedSemester: BarsSemester? get() = semesters.firstOrNull { it.selected }
}

private val DATE_PARTS = Regex("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2}|\\d{4})$")

/** "28.10.26" -> 20261028, "28.10.2026" -> 20261028, anything else -> 0. */
fun attendanceDateKey(date: String): Int {
    val match = DATE_PARTS.find(date.trim()) ?: return 0
    val (day, month, year) = match.destructured
    val fullYear = if (year.length == 2) 2000 + year.toInt() else year.toInt()
    return fullYear * 10_000 + month.toInt() * 100 + day.toInt()
}
