package kekmech.ru.feature_bars_impl.domain

import androidx.annotation.StringRes
import kekmech.ru.res_strings.R.string as Strings

/** The kind of a class as the student knows it: BARS writes "лекция", "практическое занятие", "лабораторная работа". */
internal enum class AttendanceKind(@StringRes val label: Int) {
    LECTURE(Strings.attendance_kind_lecture),
    PRACTICE(Strings.attendance_kind_practice),
    LAB(Strings.attendance_kind_lab),
    OTHER(Strings.attendance_kind_other),
    ;

    companion object {
        fun of(rawKind: String): AttendanceKind {
            val kind = rawKind.lowercase()
            return when {
                "лекц" in kind -> LECTURE
                "практ" in kind || "семинар" in kind -> PRACTICE
                "лаборат" in kind -> LAB
                else -> OTHER
            }
        }
    }
}
