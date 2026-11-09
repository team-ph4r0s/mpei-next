package kekmech.ru.feature_bars_impl.data.attendance

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.IOException

/** The periodic check of the attendance: new skips and the risk of being refused. */
internal class AttendanceSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val tracker: AttendanceTracker by inject()
    private val prefs: AttendancePrefs by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // The switches may have been turned off after the work was queued
        if (!prefs.anyEnabled) return@withContext Result.success()
        try {
            tracker.check()
            Result.success()
        } catch (e: IOException) {
            // no network, a slow BARS: the next try is later
            Timber.w(e, "AttendanceSyncWorker: network error")
            Result.retry()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "AttendanceSyncWorker: unexpected error")
            Result.failure()
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "bars_attendance_sync"
        const val IMMEDIATE_WORK_NAME = "bars_attendance_sync_now"
    }
}
