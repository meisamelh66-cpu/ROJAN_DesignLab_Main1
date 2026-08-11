package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import ai.rojan.designlab.domain.beauty.BeautyProfileRepository
import ai.rojan.designlab.domain.beauty.HairProfile
import ai.rojan.designlab.domain.beauty.NailProfile
import ai.rojan.designlab.domain.beauty.SkinProfile
import ai.rojan.designlab.presentation.beauty.BeautyProfileViewModel
import ai.rojan.designlab.presentation.beauty.BeautyProfileViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.rtl.RtlListRow
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

private val HAIR_TYPES = listOf("صاف", "موج‌دار", "فرفری", "خشک", "چرب")
private val HAIR_COLORS = listOf("مشکی", "قهوه‌ای", "بلوند", "قرمز", "رنگ‌شده")
private val HAIR_TREATMENTS = listOf("رنگ مو", "کراتینه", "بوتاکس مو", "اکستنشن")
private val SKIN_TYPES = listOf("نرمال", "خشک", "چرب", "مختلط", "حساس")
private val SKIN_CONCERNS = listOf("آکنه", "لک", "چروک", "خشکی", "قرمزی")
private val NAIL_STYLES = listOf("کلاسیک", "ژلیش", "کاشت", "طراحی‌شده")

/**
 * Beauty DNA foundation screen — Phase 2 scope ("create the domain model
 * only... do not implement AI recommendations yet"). Pure preference
 * capture against [BeautyProfileViewModel], no recommendation logic
 * anywhere. [BeautyProfileRepository] is in-memory/session-only (no
 * backend endpoint exists for this yet) — disclosed to the user via the
 * note under the title rather than silently implying it's saved to their
 * account, same "don't fake persistence" reasoning as [FavoritesScreen]/
 * [ai.rojan.designlab.screens.customer.FollowedSalons].
 */
@Composable
fun BeautyDnaScreen(
    customerId: String,
    beautyProfileRepository: BeautyProfileRepository,
    onBackClick: () -> Unit,
    viewModel: BeautyProfileViewModel = viewModel(
        factory = BeautyProfileViewModelFactory(customerId, beautyProfileRepository),
    ),
) {
    val profile = viewModel.profile

    HomeBackgroundTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }

            item {
                Text("بیوتی دی‌ان‌ای", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
            }
            item {
                Text(
                    "این اطلاعات فقط روی همین دستگاه ذخیره می‌شود.",
                    style = RojanTypography.Caption,
                    color = HomeColors.TextSecondary,
                )
            }

            item { RtlSectionHeader("مو", color = HomeColors.TextPrimary, horizontalPadding = 0.dp) }
            item {
                SingleSelectGroup(
                    label = "نوع مو",
                    options = HAIR_TYPES,
                    selected = profile.hair.hairType,
                    onSelect = { viewModel.updateHair(profile.hair.copy(hairType = it)) },
                )
            }
            item {
                SingleSelectGroup(
                    label = "رنگ مو",
                    options = HAIR_COLORS,
                    selected = profile.hair.hairColor,
                    onSelect = { viewModel.updateHair(profile.hair.copy(hairColor = it)) },
                )
            }
            item {
                MultiSelectGroup(
                    label = "سابقه خدمات مو",
                    options = HAIR_TREATMENTS,
                    selected = profile.hair.treatmentHistory,
                    onToggle = { option ->
                        val updated = profile.hair.treatmentHistory.toggle(option)
                        viewModel.updateHair(profile.hair.copy(treatmentHistory = updated))
                    },
                )
            }

            item { RtlSectionHeader("پوست", color = HomeColors.TextPrimary, horizontalPadding = 0.dp) }
            item {
                SingleSelectGroup(
                    label = "نوع پوست",
                    options = SKIN_TYPES,
                    selected = profile.skin.skinType,
                    onSelect = { viewModel.updateSkin(profile.skin.copy(skinType = it)) },
                )
            }
            item {
                MultiSelectGroup(
                    label = "نگرانی‌های پوستی",
                    options = SKIN_CONCERNS,
                    selected = profile.skin.concerns,
                    onToggle = { option ->
                        val updated = profile.skin.concerns.toggle(option)
                        viewModel.updateSkin(profile.skin.copy(concerns = updated))
                    },
                )
            }

            item { RtlSectionHeader("ناخن", color = HomeColors.TextPrimary, horizontalPadding = 0.dp) }
            item {
                SingleSelectGroup(
                    label = "سبک مورد علاقه",
                    options = NAIL_STYLES,
                    selected = profile.nails.stylePreference,
                    onSelect = { viewModel.updateNails(NailProfile(stylePreference = it)) },
                )
            }
        }
    }
}

private fun List<String>.toggle(value: String): List<String> =
    if (contains(value)) this - value else this + value

@Composable
private fun SingleSelectGroup(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    OptionGroup(label = label) {
        options.forEach { option ->
            OptionRow(
                title = option,
                isSelected = option == selected,
                onClick = { onSelect(if (option == selected) null else option) },
            )
        }
    }
}

@Composable
private fun MultiSelectGroup(
    label: String,
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    OptionGroup(label = label) {
        options.forEach { option ->
            OptionRow(
                title = option,
                isSelected = selected.contains(option),
                onClick = { onToggle(option) },
            )
        }
    }
}

@Composable
private fun OptionGroup(label: String, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
    ) {
        Text(label, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
        Column(
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
        ) {
            content()
        }
    }
}

@Composable
private fun OptionRow(title: String, isSelected: Boolean, onClick: () -> Unit) {
    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        RtlListRow(
            title = title,
            titleColor = HomeColors.TextPrimary,
            trailingIcon = if (isSelected) Icons.Filled.Check else null,
            modifier = Modifier.padding(RojanDimens.SpaceMD),
        )
    }
}
