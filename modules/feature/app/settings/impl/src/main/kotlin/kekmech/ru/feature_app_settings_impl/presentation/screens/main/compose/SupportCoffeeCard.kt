package kekmech.ru.feature_app_settings_impl.presentation.screens.main.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kekmech.ru.res_strings.R.string as Strings
import kekmech.ru.ui_theme.theme.MpeixTheme

/** Where the team is supported: the page of the team on Lava.top. */
internal const val SUPPORT_URL = "https://app.lava.top/pharosteam"

private val GoldLight = Color(0xFFF3D98B)
private val GoldDeep = Color(0xFFC9A227)
private val CardShape = RoundedCornerShape(16.dp)
private val GoldBorderWidth = 1.5.dp

/** "Buy the developers a coffee": a card in a gold frame that opens [SUPPORT_URL]. */
@Composable
internal fun SupportCoffeeCard(
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CardShape,
        color = MpeixTheme.palette.surface,
        border = BorderStroke(GoldBorderWidth, Brush.linearGradient(listOf(GoldLight, GoldDeep, GoldLight))),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(role = Role.Button) { onOpen(SUPPORT_URL) },
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = stringResource(Strings.support_coffee_title),
                style = MpeixTheme.typography.paragraphBig.copy(fontWeight = FontWeight.Bold),
                color = MpeixTheme.palette.content,
            )
            Text(
                text = stringResource(Strings.support_coffee_text),
                style = MpeixTheme.typography.paragraphNormal,
                color = MpeixTheme.palette.contentVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(Strings.support_coffee_action),
                style = MpeixTheme.typography.paragraphBig.copy(fontWeight = FontWeight.Medium),
                color = GoldDeep,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
