package kekmech.ru.feature_bars_impl.data.attendance

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kekmech.ru.lib_app_lifecycle.AppLifecycleObserver
import java.util.concurrent.TimeUnit

/** Keeps the periodic check of the attendance alive while a notification is switched on. */
internal class AttendanceController(
    private val context: Context,
    private val prefs: AttendancePrefs,
) {

    private val workManager get() = WorkManager.getInstance(context)

    /** On the start of the app, and after a switch is changed. */
    fun ensureScheduled() {
        if (!prefs.anyEnabled) {
            workManager.cancelUniqueWork(AttendanceSyncWorker.PERIODIC_WORK_NAME)
            workManager.cancelUniqueWork(AttendanceSyncWorker.IMMEDIATE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<AttendanceSyncWorker>(
            repeatInterval = CHECK_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(CONNECTED)
            .build()
        workManager.enqueueUniquePeriodicWork(
            AttendanceSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** A switch has been turned on: the first check is not to wait for the period (it also takes the base of "what is new"). */
    fun checkNow() {
        if (!prefs.anyEnabled) return
        workManager.enqueueUniqueWork(
            AttendanceSyncWorker.IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AttendanceSyncWorker>().setConstraints(CONNECTED).build(),
        )
    }

    private companion object {
        /** A skip is marked by a teacher after the class: some hours later is soon enough, and BARS is slow. */
        const val CHECK_INTERVAL_HOURS = 3L
        val CONNECTED: Constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    }
}

/** Makes sure the check of the attendance is planned on every start of the app. */
internal class AttendanceLifecycleObserver(
    private val controller: AttendanceController,
) : AppLifecycleObserver {

    override fun onCreate(context: Context) {
        controller.ensureScheduled()
    }
}
