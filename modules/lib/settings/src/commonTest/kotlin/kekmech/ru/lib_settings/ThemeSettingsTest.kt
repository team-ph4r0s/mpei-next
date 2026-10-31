package kekmech.ru.lib_settings

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ThemeSettingsTest {

    private val store = InMemoryKeyValueStore()

    @Test
    fun `the theme follows the system until another one is picked`() {
        assertEquals(ThemeMode.SYSTEM, ThemeSettings(store).mode)
    }

    @Test
    fun `the picked theme is kept`() {
        ThemeSettings(store).mode = ThemeMode.DARK

        assertEquals(ThemeMode.DARK, ThemeSettings(store).mode)
    }

    @Test
    fun `an unknown value means the system theme`() {
        store.putString("mpeix_theme_mode", "SEPIA")

        assertEquals(ThemeMode.SYSTEM, ThemeSettings(store).mode)
    }
}
