package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGlassWhite
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.RojanVividMagenta

/**
 * Booking Module Design System Migration.
 *
 * Structure preserved exactly (still a real editable [OutlinedTextField],
 * not converted to Customer Home's [ai.rojan.designlab.screens.customer.AISearchBar]
 * pattern — that component is a static display placeholder with different
 * behavior; this one holds real text-input state, a genuine functional
 * difference the migration rules explicitly say to preserve rather than
 * force-generalize).
 *
 * The fill's original two solid pastel stops (`#F3E8FF` lavender-ish,
 * `#E0F7FF` aqua-ish) are close enough in hue to the existing
 * [RojanSoftLavender]/[RojanAquaMint] tokens to reuse directly rather
 * than add near-duplicate new ones.
 */
@Composable
fun BookingSearchBar() {

    var query by remember { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },

        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = RojanDimens.SpaceSM)
            .shadow(
                elevation = RojanShadows.PremiumElevation,
                shape = RojanShapes.GlassCard
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        RojanGlassWhite.copy(alpha = 0.85f),
                        RojanSoftLavender,
                        RojanAquaMint
                    )
                ),
                RojanShapes.GlassCard
            ),

        shape = RojanShapes.GlassCard,
        singleLine = true,

        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = RojanAIGlow
            )
        },

        trailingIcon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI",
                tint = RojanVividMagenta
            )
        },

        placeholder = {
            Text(
                text = "از AI بپرسید...",
                style = RojanTypography.Body,
                color = RojanTextSecondary
            )
        }
    )
}
