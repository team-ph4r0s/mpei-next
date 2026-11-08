package kekmech.ru.feature_bars_api

import kotlinx.coroutines.flow.Flow

/** The BARS account the student has signed in to through the app, for the screens outside of the BARS feature. */
public interface BarsSessionHandle {

    /** Emits the current state at once and every change after it: `true` while an account is signed in. */
    public val isSignedIn: Flow<Boolean>

    /** Erases the session and every piece of data of the account from the device; the portal is told in the background. */
    public fun logout()
}
