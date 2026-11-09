package kekmech.ru.feature_bars_impl.data.attendance

import kekmech.ru.feature_bars_api.BarsNotificationSettings
import kekmech.ru.feature_bars_api.BarsNotificationSettings.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AttendanceNotificationSettingsImpl(
    private val prefs: AttendancePrefs,
    private val controller: AttendanceController,
) : BarsNotificationSettings {

    override fun current(): State = State(newSkip = prefs.newSkipEnabled, criticalThreshold = prefs.criticalEnabled)

    override fun observe(): Flow<State> = prefs.observeSwitches().map { (newSkip, critical) -> State(newSkip, critical) }

    override fun setNewSkipEnabled(enabled: Boolean) {
        prefs.newSkipEnabled = enabled
        changed(enabled)
    }

    override fun setCriticalThresholdEnabled(enabled: Boolean) {
        prefs.criticalEnabled = enabled
        changed(enabled)
    }

    private fun changed(turnedOn: Boolean) {
        controller.ensureScheduled()
        if (turnedOn) controller.checkNow()
    }
}
