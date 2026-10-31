package kekmech.ru.lib_settings

/** How the app picks its colors: the same three choices as the theme setting of the Android app. */
public enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The chosen [ThemeMode], kept in [store]; [SYSTEM] until the student picks another one. */
public class ThemeSettings(private val store: KeyValueStore) {

    public var mode: ThemeMode
        get() = store.getString(KEY_THEME_MODE)
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM
        set(value) = store.putString(KEY_THEME_MODE, value.name)

    private companion object {
        const val KEY_THEME_MODE = "mpeix_theme_mode"
    }
}
