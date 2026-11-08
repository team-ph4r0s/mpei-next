package kekmech.ru.feature_app_settings_impl.presentation.screens.main.compose

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import kekmech.ru.res_strings.R.string as Strings
import kekmech.ru.ui_kit_dialogs.AlertDialog
import kekmech.ru.ui_kit_dialogs.AlertDialogState
import kekmech.ru.ui_theme.theme.MpeixTheme

/** Asks the student to confirm the sign-out from BARS: it erases the session and the data saved on the device. */
@Composable
internal fun BarsLogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // the screen shows the dialog only while it is needed, so the dialog is visible from its first frame
    val state = remember { AlertDialogState(dialogVisible = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        state = state,
        title = stringResource(Strings.app_settings_bars_logout_dialog_title),
        text = stringResource(Strings.app_settings_bars_logout_dialog_text),
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(Strings.app_settings_bars_logout_confirm),
                    color = MpeixTheme.palette.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Strings.app_settings_bars_logout_cancel))
            }
        },
    )
}
