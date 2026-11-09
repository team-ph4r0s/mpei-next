package kekmech.ru.feature_bars_impl.data.attendance

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kekmech.ru.feature_bars_shared.AttendanceSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The switches of the attendance notifications and what was known at the last check (the base of "what is new").
 * Both switches are off at first: nothing is sent until the student asks for it.
 */
internal class AttendancePrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var newSkipEnabled: Boolean
        get() = prefs.getBoolean(KEY_NEW_SKIP, false)
        set(value) = prefs.edit { putBoolean(KEY_NEW_SKIP, value) }

    var criticalEnabled: Boolean
        get() = prefs.getBoolean(KEY_CRITICAL, false)
        set(value) = prefs.edit { putBoolean(KEY_CRITICAL, value) }

    val anyEnabled: Boolean get() = newSkipEnabled || criticalEnabled

    fun observeSwitches(): Flow<Pair<Boolean, Boolean>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(newSkipEnabled to criticalEnabled)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(newSkipEnabled to criticalEnabled)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun snapshot(studentId: String): AttendanceSnapshot? =
        AttendanceSnapshot.decode(prefs.getString(snapshotKey(studentId), null))

    fun saveSnapshot(studentId: String, snapshot: AttendanceSnapshot) {
        prefs.edit { putString(snapshotKey(studentId), snapshot.encode()) }
    }

    /** The student has signed out: what was known about the attendance of the accounts is of no use. */
    fun clearSnapshots() {
        prefs.edit {
            prefs.all.keys.filter { it.startsWith(SNAPSHOT_PREFIX) }.forEach { remove(it) }
        }
    }

    private fun snapshotKey(studentId: String) = SNAPSHOT_PREFIX + studentId

    private companion object {
        const val PREFS_NAME = "bars_attendance"
        const val KEY_NEW_SKIP = "notify_new_skip"
        const val KEY_CRITICAL = "notify_critical"
        const val SNAPSHOT_PREFIX = "snapshot_"
    }
}
