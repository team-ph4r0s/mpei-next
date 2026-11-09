package kekmech.ru.lib_bars_client

import kekmech.ru.lib_bars_parser.BarsSendCodeResult
import kekmech.ru.lib_bars_parser.BarsStudentLink
import kekmech.ru.lib_bars_parser.BarsTwoFactorPage

/** The addresses of the portal. The paths are the ones of statics/bars/config.json and of the pages themselves. */
data class BarsEndpoints(
    val baseUrl: String = "https://bars.mpei.ru",
) {
    val login: String get() = "$baseUrl/BARS_Web"
    val marks: String get() = "$baseUrl/BARS_Web/ST_Study/Main/Main"
    val summary: String get() = "$baseUrl/bars_web/ST_Study/Main/Summary"
    val summaryPart: String get() = "$baseUrl/bars_web/ST_Study/Main/_PartialSummary"

    /** The marks of a semester: what the marks page asks for when the semester is changed. */
    val marksPart: String get() = "$baseUrl/bars_web/ST_Study/Student_SemesterSheet/_PartialListStudent_SemesterSheet__Mark"

    /** The header with the rating of a semester: what the marks page asks for when the semester of the rating is changed. */
    val headerPart: String get() = "$baseUrl/bars_web/ST_Study/Main/_PartialHeader"
    val logout: String get() = "$baseUrl/BARS_Web/Auth/Exit"

    /** "Посещение занятий": the page with the semesters and the first skips. */
    val attendance: String get() = "$baseUrl/bars_web/ST_Study/Lesson__Student/ListStudent__Lesson__Student"

    /** The table of "Посещение занятий" for a filter: what the page asks for when a filter or the page of the pager changes. */
    val attendancePart: String get() = "$baseUrl/bars_web/ST_Study/Lesson__Student/_PartialListStudent__Lesson__Student"
}

/** The attendance of a student in a semester: every class that BARS has a record of, with what BARS says about it. */
data class BarsAttendance(
    /** The semesters of the filter of the page, to choose another one. */
    val semesters: List<kekmech.ru.lib_bars_parser.BarsSemester>,
    /** The semester [records] are of: the one that BARS opens (the current) or the one that was asked for. */
    val semesterId: String,
    /** Skips (with a good reason or without) and visits; the status of each is final, see [BarsApi.loadAttendance]. */
    val records: List<kekmech.ru.lib_bars_parser.BarsAttendanceRecord>,
)

sealed interface BarsLoginResult {

    /** The session is open. [studentId] is null if the account has several students: choose one of [students]. */
    data class LoggedIn(
        val studentId: String?,
        val students: List<BarsStudentLink>,
    ) : BarsLoginResult

    /**
     * The login and the password are right, BARS wants a code. [codeRequest] is the answer to the request for a code
     * that the page itself makes when it is loaded (null if the client was told not to make it).
     */
    data class TwoFactorRequired(
        val page: BarsTwoFactorPage,
        val codeRequest: BarsSendCodeResult?,
    ) : BarsLoginResult

    /**
     * BARS did not let in. With [canRetryCode] a new code can be sent to `submitCode` (the session is still waiting
     * for it), otherwise the login has to start over.
     */
    data class Rejected(
        val message: String,
        val canRetryCode: Boolean,
    ) : BarsLoginResult
}

/** BARS answered with something the client cannot work with: an error status, a page it does not know. */
class BarsNetworkException(message: String) : Exception(message)

/** The page that was asked for is the sign-in page: the session is absent or has expired, sign in again. */
class BarsSessionExpiredException : Exception("The BARS session is absent or has expired")
