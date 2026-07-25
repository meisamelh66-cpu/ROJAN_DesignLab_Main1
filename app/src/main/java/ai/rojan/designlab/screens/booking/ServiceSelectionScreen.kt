package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.data.demo.DemoService
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Booking Experience Refactor, spec section 8 — Service Selection.
 * "User can select multiple services. Tap: Selected. Tap again:
 * Deselected. Bottom button: Continue Booking." Multi-select state is
 * plain local UI state ([remember] + [mutableStateOf]) since it's
 * transient (only meaningful for this one screen visit) — not something
 * that belongs in a ViewModel's persisted state.
 */
@Composable
fun ServiceSelectionScreen(
    categoryLabel: String,
    onBackClick: () -> Unit,
    onContinueBooking: (List<String>) -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    val services = remember(categoryLabel) { catalogEngine.servicesForCategory(categoryLabel) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)

            Text(
                text = categoryLabel,
                style = RojanTypography.HeroTitle,
                color = RojanTextOnGlass,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                items(services) { service ->
                    val isSelected = service.id in selectedIds
                    ServiceSelectionRow(
                        service = service,
                        isSelected = isSelected,
                        onToggle = {
                            selectedIds = if (isSelected) selectedIds - service.id else selectedIds + service.id
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            PremiumButton(
                text = "ادامه رزرو",
                onClick = { onContinueBooking(selectedIds.toList()) },
            )
        }
    }
}

@Composable
private fun ServiceSelectionRow(
    service: DemoService,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onToggle,
            ),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    service.name,
                    style = RojanTypography.Body.rojanPressedShadow(interactionSource),
                    color = RojanTextPrimary,
                )
                Text(
                    "${service.durationMinutes} دقیقه",
                    style = RojanTypography.Caption,
                    color = RojanTextSecondary,
                )
                Text(
                    text = "${service.discountPrice ?: service.price} تومان",
                    style = RojanTypography.Caption,
                    color = RojanAIGlow,
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (isSelected) "انتخاب شده" else "انتخاب نشده",
                tint = if (isSelected) RojanAIGlow else RojanTextSecondary,
            )
        }
    }
}
