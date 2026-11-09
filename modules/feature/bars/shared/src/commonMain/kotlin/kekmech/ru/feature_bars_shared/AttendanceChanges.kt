package kekmech.ru.feature_bars_shared

import kekmech.ru.lib_bars_parser.BarsAttendanceRecord
import kekmech.ru.lib_bars_parser.BarsAttendanceStatus

/**
 * What was known about the attendance at the last check: the status of every record and the disciplines that were
 * critical. It is what the next check is compared with, to find what is new.
 */
class AttendanceSnapshot(
    /** The semester the records are of: another semester is another start (there is nothing to compare with). */
    val semesterId: String,
    val statuses: Map<String, BarsAttendanceStatus>,
    val critical: Set<String>,
) {

    /** Text for the storage of the app: a line for a record (`S`, id, status) and a line for a critical discipline (`C`, name). */
    fun encode(): String = buildString {
        append("V1").append(SEPARATOR).append(clean(semesterId))
        for ((id, status) in statuses) append(LINE).append("S").append(SEPARATOR).append(clean(id)).append(SEPARATOR).append(status.name)
        for (name in critical) append(LINE).append("C").append(SEPARATOR).append(clean(name))
    }

    companion object {
        private const val LINE = '\n'
        private const val SEPARATOR = '\t'

        private fun clean(text: String) = text.replace(LINE, ' ').replace(SEPARATOR, ' ')

        /** The snapshot that [encode] wrote; null for a text that is not one (the next check then starts anew). */
        fun decode(text: String?): AttendanceSnapshot? {
            if (text.isNullOrEmpty()) return null
            val lines = text.split(LINE)
            val head = lines.first().split(SEPARATOR)
            if (head.size != 2 || head[0] != "V1") return null
            val statuses = LinkedHashMap<String, BarsAttendanceStatus>()
            val critical = LinkedHashSet<String>()
            for (line in lines.drop(1)) {
                val parts = line.split(SEPARATOR)
                when {
                    parts.size == 3 && parts[0] == "S" -> {
                        val status = BarsAttendanceStatus.entries.firstOrNull { it.name == parts[2] } ?: return null
                        statuses[parts[1]] = status
                    }
                    parts.size == 2 && parts[0] == "C" -> critical += parts[1]
                    else -> return null
                }
            }
            return AttendanceSnapshot(head[1], statuses, critical)
        }
    }
}

/** What is new since the last check. */
data class AttendanceChanges(
    /** Skips without a good reason that were not known as skips. */
    val newSkips: List<BarsAttendanceRecord>,
    /** The disciplines that became critical. */
    val newCritical: List<DisciplineAttendance>,
) {
    val isEmpty: Boolean get() = newSkips.isEmpty() && newCritical.isEmpty()
}

fun AttendanceSummary.toSnapshot(semesterId: String): AttendanceSnapshot =
    AttendanceSnapshot(
        semesterId = semesterId,
        statuses = disciplines.flatMap { it.records }.associate { it.id to it.status },
        critical = disciplines.filter { it.risk == AttendanceRisk.CRITICAL }.mapTo(LinkedHashSet()) { it.name },
    )

/**
 * Compares the attendance with the [previous] check. There is nothing new at the very first check (and at the first one
 * in a new semester): everything that is already in BARS is the start, not news, or the student would get a notification
 * for every old skip.
 */
fun detectAttendanceChanges(
    previous: AttendanceSnapshot?,
    semesterId: String,
    summary: AttendanceSummary,
): AttendanceChanges {
    if (previous == null || previous.semesterId != semesterId) return AttendanceChanges(emptyList(), emptyList())
    val newSkips = summary.disciplines
        .flatMap { it.records }
        .filter { it.status == BarsAttendanceStatus.ABSENT && previous.statuses[it.id] != BarsAttendanceStatus.ABSENT }
        .sortedBy { it.orderKey }
    val newCritical = summary.disciplines
        .filter { it.risk == AttendanceRisk.CRITICAL && it.name !in previous.critical }
    return AttendanceChanges(newSkips = newSkips, newCritical = newCritical)
}
