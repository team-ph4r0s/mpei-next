package kekmech.ru.lib_bars_client

import kekmech.ru.lib_bars_parser.BarsMarksPage
import kekmech.ru.lib_bars_parser.BarsMarksPart
import kekmech.ru.lib_bars_parser.BarsRating
import kekmech.ru.lib_bars_parser.BarsSendCodeResult
import kekmech.ru.lib_bars_parser.BarsSummaryPage
import kekmech.ru.lib_bars_parser.BarsTwoFactorProvider

/**
 * What the screens need from the portal. [BarsClient] is the implementation; the screens and their tests work with
 * this interface, so they do not need a network.
 */
interface BarsApi {

    /** The student whose pages are asked for, known after the sign-in or the first page. */
    val studentId: String?

    suspend fun login(
        login: String,
        password: String,
        rememberMe: Boolean = false,
        sendCode: Boolean = true,
    ): BarsLoginResult

    suspend fun sendCode(provider: BarsTwoFactorProvider? = null): BarsSendCodeResult

    suspend fun submitCode(code: String): BarsLoginResult

    suspend fun restoreSession(): Boolean

    suspend fun loadMarksPage(studentId: String? = null): BarsMarksPage

    suspend fun loadSummary(semesterId: String? = null, studentId: String? = null): BarsSummaryPage

    suspend fun loadSemesterMarks(semesterId: String, studentId: String? = null): BarsMarksPart

    suspend fun loadRating(semesterId: String, studentId: String? = null): BarsRating?

    /**
     * "Посещение занятий" of a semester ([semesterId] is a value of the semester filter, the current one if null).
     *
     * BARS lists the classes with a filter "skipped / visited" and a filter of the reason of a skip, so the status of a
     * record does not come from how its badge looks but from the filter that returned it: the skips with any reason, the
     * skips with a good reason (they are [kekmech.ru.lib_bars_parser.BarsAttendanceStatus.EXCUSED], the other skips are
     * [kekmech.ru.lib_bars_parser.BarsAttendanceStatus.ABSENT]) and the visits.
     * @throws BarsSessionExpiredException if BARS wants the sign-in again
     */
    suspend fun loadAttendance(semesterId: String? = null, studentId: String? = null): BarsAttendance

    suspend fun logout()
}
