package kekmech.ru.feature_bars_impl.data.attendance

import kekmech.ru.feature_bars_impl.data.repository.BarsRepository
import kekmech.ru.feature_bars_shared.AttendanceChanges
import kekmech.ru.feature_bars_shared.AttendanceSummary
import kekmech.ru.feature_bars_shared.analyzeAttendance
import kekmech.ru.feature_bars_shared.detectAttendanceChanges
import kekmech.ru.feature_bars_shared.toSnapshot
import kekmech.ru.lib_bars_client.BarsAttendance
import kekmech.ru.lib_bars_client.BarsSessionExpiredException

/**
 * Finds what is new in the attendance (BARS is asked, the result is compared with the last check) and tells the student
 * about it with the notifications that are switched on. The base of the comparison is also moved by the screen: what the
 * student has already seen on it is not news.
 */
internal class AttendanceTracker(
    private val repository: BarsRepository,
    private val prefs: AttendancePrefs,
    private val notifier: AttendanceNotifier,
) {

    sealed interface Result {
        /** Nobody is signed in to BARS (or the session is over): there is nothing to check. */
        data object NotSignedIn : Result

        data class Checked(val changes: AttendanceChanges) : Result
    }

    /** One check, for the background work. Network errors are thrown: the caller decides to try again later. */
    suspend fun check(): Result {
        val account = repository.getCurrentAccount()
        if (account == null) {
            prefs.clearSnapshots()
            return Result.NotSignedIn
        }
        val api = repository.api
        // The session of the app may be gone with the process: it is brought back from the store
        if (api.studentId == null && !api.restoreSession()) return Result.NotSignedIn

        val attendance = try {
            api.loadAttendance(studentId = account.id)
        } catch (expired: BarsSessionExpiredException) {
            repository.onSessionExpired()
            return Result.NotSignedIn
        }
        val summary = analyzeAttendance(attendance.records)
        val changes = detectAttendanceChanges(prefs.snapshot(account.id), attendance.semesterId, summary)
        if (prefs.newSkipEnabled) notifier.showSkips(changes.newSkips)
        if (prefs.criticalEnabled) notifier.showCritical(changes.newCritical)
        prefs.saveSnapshot(account.id, summary.toSnapshot(attendance.semesterId))
        return Result.Checked(changes)
    }

    /** The screen has shown this attendance: it is not news any more. */
    fun markSeen(attendance: BarsAttendance, summary: AttendanceSummary) {
        val studentId = repository.getCurrentAccount()?.id ?: return
        prefs.saveSnapshot(studentId, summary.toSnapshot(attendance.semesterId))
    }
}
