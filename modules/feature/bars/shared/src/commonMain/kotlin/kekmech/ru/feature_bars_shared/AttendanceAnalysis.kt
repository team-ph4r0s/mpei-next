package kekmech.ru.feature_bars_shared

import kekmech.ru.lib_bars_parser.BarsAttendanceRecord
import kekmech.ru.lib_bars_parser.BarsAttendanceStatus

/** How bad the skips of a discipline are: green, yellow, red. */
enum class AttendanceRisk {
    /** No skips without a good reason. */
    OK,

    /** There are skips without a good reason. */
    WARNING,

    /** The student is close to being refused the exam: too many skips, or too many in a row. */
    CRITICAL,
}

/** The thresholds of [AttendanceRisk.CRITICAL]; the one place to change them. */
object AttendanceRules {
    /** Skips without a good reason are this share of the recorded classes of the discipline or more. */
    const val CRITICAL_SKIP_SHARE = 0.25

    /** The share counts only when the discipline has this many recorded classes: one skip of two is not a verdict. */
    const val MIN_CLASSES_FOR_SHARE = 4

    /** This many skips without a good reason in a row (in the order of the classes of the discipline). */
    const val CRITICAL_STREAK = 3
}

/** The attendance of one discipline: its classes that BARS has a record of, newest first. */
data class DisciplineAttendance(
    val name: String,
    /** "экзамен", "зачёт" ... as BARS writes it behind the name of the discipline. */
    val assessmentType: String?,
    val records: List<BarsAttendanceRecord>,
    val present: Int,
    /** Skipped with a good reason: they do not count against the student. */
    val excused: Int,
    /** Skipped without a good reason. */
    val absent: Int,
    /** The longest row of skips without a good reason over the semester. */
    val longestSkipStreak: Int,
    /** The row of skips without a good reason that ends with the latest class. */
    val currentSkipStreak: Int,
    val risk: AttendanceRisk,
) {
    val total: Int get() = present + excused + absent

    /** Per cent of the classes that the student was at or missed for a good reason. */
    val percent: Int get() = percentOf(present + excused, total)

    /** The share of the skips without a good reason in the recorded classes, 0..1. */
    val skipShare: Double get() = if (total == 0) 0.0 else absent.toDouble() / total
}

/** The attendance of a semester. */
data class AttendanceSummary(
    val present: Int,
    val excused: Int,
    val absent: Int,
    /** The worst first: the critical ones, then by the number of skips. */
    val disciplines: List<DisciplineAttendance>,
) {
    val total: Int get() = present + excused + absent
    val percent: Int get() = percentOf(present + excused, total)
    val hasRecords: Boolean get() = total > 0
}

private fun percentOf(part: Int, whole: Int): Int = if (whole == 0) 100 else (part * 100 + whole / 2) / whole

/** Groups the records by discipline and counts them. Records of an unknown status are left out: it is not known what they are. */
fun analyzeAttendance(records: List<BarsAttendanceRecord>): AttendanceSummary {
    val known = records.filter { it.status != BarsAttendanceStatus.UNKNOWN }
    val disciplines = known.groupBy { it.discipline }
        .map { (name, items) -> disciplineAttendance(name, items) }
        .sortedWith(
            compareByDescending<DisciplineAttendance> { it.risk }
                .thenByDescending { it.absent }
                .thenBy { it.name },
        )
    return AttendanceSummary(
        present = disciplines.sumOf { it.present },
        excused = disciplines.sumOf { it.excused },
        absent = disciplines.sumOf { it.absent },
        disciplines = disciplines,
    )
}

private fun disciplineAttendance(name: String, items: List<BarsAttendanceRecord>): DisciplineAttendance {
    val oldestFirst = items.sortedBy { it.orderKey }
    val absent = oldestFirst.count { it.status == BarsAttendanceStatus.ABSENT }
    val excused = oldestFirst.count { it.status == BarsAttendanceStatus.EXCUSED }
    val present = oldestFirst.count { it.status == BarsAttendanceStatus.PRESENT }
    val total = present + excused + absent

    var longest = 0
    var run = 0
    for (record in oldestFirst) {
        run = if (record.status == BarsAttendanceStatus.ABSENT) run + 1 else 0
        if (run > longest) longest = run
    }

    val risk = when {
        absent == 0 -> AttendanceRisk.OK
        longest >= AttendanceRules.CRITICAL_STREAK -> AttendanceRisk.CRITICAL
        total >= AttendanceRules.MIN_CLASSES_FOR_SHARE &&
            absent.toDouble() / total >= AttendanceRules.CRITICAL_SKIP_SHARE -> AttendanceRisk.CRITICAL
        else -> AttendanceRisk.WARNING
    }
    return DisciplineAttendance(
        name = name,
        assessmentType = items.firstNotNullOfOrNull { it.assessmentType },
        records = oldestFirst.asReversed(),
        present = present,
        excused = excused,
        absent = absent,
        longestSkipStreak = longest,
        currentSkipStreak = run,
        risk = risk,
    )
}
