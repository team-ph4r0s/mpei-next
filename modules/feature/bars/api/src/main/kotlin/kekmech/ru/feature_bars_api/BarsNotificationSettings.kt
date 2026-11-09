package kekmech.ru.feature_bars_api

import kotlinx.coroutines.flow.Flow

/** On / off switches of the BARS notifications (the attendance), for the main settings screen. */
public interface BarsNotificationSettings {

    public data class State(
        /** A notification at once when BARS has a new skip of a class. */
        val newSkip: Boolean,
        /** A warning when there are so many skips in a subject that the student may be refused. */
        val criticalThreshold: Boolean,
    )

    public fun current(): State

    /** Emits the current state and every change of it. */
    public fun observe(): Flow<State>

    public fun setNewSkipEnabled(enabled: Boolean)

    public fun setCriticalThresholdEnabled(enabled: Boolean)
}
