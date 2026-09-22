package ai.rojan.designlab.screens.profile

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import ai.rojan.designlab.domain.beauty.BeautyProfileRepository
import ai.rojan.designlab.domain.beauty.HairProfile
import ai.rojan.designlab.domain.beauty.NailProfile
import ai.rojan.designlab.domain.beauty.SkinProfile
import ai.rojan.designlab.presentation.beauty.BeautyProfileViewModel
import ai.rojan.designlab.presentation.beauty.BeautyProfileViewModelFactory
import ai.rojan.designlab.screens.customer.components.CustomerAccent
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.RefListRow
import ai.rojan.designlab.screens.customer.components.RefRowDivider
import ai.rojan.designlab.screens.customer.components.RefSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

private val HAIR_TYPES = listOf("صاف", "موج‌دار", "فرفری", "خشک", "چرب")
private val HAIR_COLORS = listOf("مشکی", "قهوه‌ای", "بلوند", "قرمز", "رنگ‌شده")
private val HAIR_TREATMENTS = listOf("رنگ مو", "کراتینه", "بوتاکس مو", "اکستنشن")
private val SKIN_TYPES = listOf("نرمال", "خشک", "چرب", "مختلط", "حساس")
private val SKIN_CONCERNS = listOf("آکنه", "لک", "چروک", "خشکی", "قرمزی")
private val NAIL_STYLES = listOf("کلاسیک", "ژلیش", "کاشت", "طراحی‌شده")

/** The three top-level accordion sections. Only one is ever expanded. */
private enum class DnaSection {
    HAIR,
    SKIN,
    NAILS,
}

/**
 * Beauty DNA — pure preference capture against [BeautyProfileViewModel], no
 * recommendation logic. [BeautyProfileRepository] is in-memory / session-only
 * (no backend endpoint yet) — disclosed to the user in the note under the
 * title rather than implying it is saved to their account.
 *
 * **UX redesign pass (2026-09-10, corrected in the Pre-Release Audit):** the
 * three domains (hair/skin/nails) are [AccordionSection]s — all collapsed by
 * default, opening one closes the others, and each header shows a rose-gold
 * summary of that domain's current selections even while collapsed, so
 * nothing selected is ever hidden. This replaces the earlier "everything
 * expanded, fills the
 * page" layout. Visual language (title "DNA" instead of the transliterated
 * Persian "دی‌ان‌ای", [CustomerScaffold]/[RefSurface] shell, rose-gold
 * selected-check rows, RTL composition) is unchanged from the prior Quiet
 * Luxury pass.
 *
 * NOTHING about behaviour changed beyond the accordion collapse/expand: the
 * same single-select / multi-select toggling still drives
 * [BeautyProfileViewModel.updateHair] / `updateSkin` / `updateNails` with
 * the exact same payloads; [onBackClick] is called unchanged. No ViewModel,
 * repository, API, or navigation route is touched.
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
    // Pre-Release Audit fix: all three sections collapsed by default (none
    // pre-expanded) - the prior default of DnaSection.HAIR contradicted this
    // screen's own doc comment and the required "collapsed by default" UX.
    var expandedSection by remember { mutableStateOf<DnaSection?>(null) }

    CustomerScaffold(title = "DNA", onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = RojanDimens.SpaceLG, bottom = RojanDimens.SpaceXXL),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            item {
                Text(
                    "این اطلاعات فقط تا زمانی که برنامه باز است نگه‌داشته می‌شود و پس از بستن برنامه پاک می‌شود.",
                    style = RojanTypography.Caption,
                    color = HomeColors.TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CustomerScreenMargin),
                )
            }
            item { Spacer(Modifier.height(RojanDimens.SpaceSM)) }

            item {
                AccordionSection(
                    title = "مو",
                    summary = hairSummary(profile.hair),
                    expanded = expandedSection == DnaSection.HAIR,
                    onToggle = {
                        expandedSection = if (expandedSection == DnaSection.HAIR) null else DnaSection.HAIR
                    },
                ) {
                    DnaOptionGroup("نوع مو", isFirst = true) {
                        SingleSelectRows(HAIR_TYPES, profile.hair.hairType) {
                            viewModel.updateHair(profile.hair.copy(hairType = it))
                        }
                    }
                    DnaOptionGroup("رنگ مو") {
                        SingleSelectRows(HAIR_COLORS, profile.hair.hairColor) {
                            viewModel.updateHair(profile.hair.copy(hairColor = it))
                        }
                    }
                    DnaOptionGroup("سابقه خدمات مو") {
                        MultiSelectRows(HAIR_TREATMENTS, profile.hair.treatmentHistory) { option ->
                            viewModel.updateHair(
                                profile.hair.copy(treatmentHistory = profile.hair.treatmentHistory.toggle(option)),
                            )
                        }
                    }
                }
            }

            item {
                AccordionSection(
                    title = "پوست",
                    summary = skinSummary(profile.skin),
                    expanded = expandedSection == DnaSection.SKIN,
                    onToggle = {
                        expandedSection = if (expandedSection == DnaSection.SKIN) null else DnaSection.SKIN
                    },
                ) {
                    DnaOptionGroup("نوع پوست", isFirst = true) {
                        SingleSelectRows(SKIN_TYPES, profile.skin.skinType) {
                            viewModel.updateSkin(profile.skin.copy(skinType = it))
                        }
                    }
                    DnaOptionGroup("نگرانی‌های پوستی") {
                        MultiSelectRows(SKIN_CONCERNS, profile.skin.concerns) { option ->
                            viewModel.updateSkin(profile.skin.copy(concerns = profile.skin.concerns.toggle(option)))
                        }
                    }
                }
            }

            item {
                AccordionSection(
                    title = "ناخن",
                    summary = nailSummary(profile.nails),
                    expanded = expandedSection == DnaSection.NAILS,
                    onToggle = {
                        expandedSection = if (expandedSection == DnaSection.NAILS) null else DnaSection.NAILS
                    },
                ) {
                    DnaOptionGroup("سبک مورد علاقه", isFirst = true) {
                        SingleSelectRows(NAIL_STYLES, profile.nails.stylePreference) {
                            viewModel.updateNails(NailProfile(stylePreference = it))
                        }
                    }
                }
            }
        }
    }
}

private fun List<String>.toggle(value: String): List<String> =
    if (contains(value)) this - value else this + value

private fun hairSummary(hair: HairProfile): String? =
    (listOfNotNull(hair.hairType, hair.hairColor) + hair.treatmentHistory)
        .takeIf { it.isNotEmpty() }
        ?.joinToString("، ")

private fun skinSummary(skin: SkinProfile): String? =
    (listOfNotNull(skin.skinType) + skin.concerns)
        .takeIf { it.isNotEmpty() }
        ?.joinToString("، ")

private fun nailSummary(nails: NailProfile): String? = nails.stylePreference

/**
 * One collapsible domain (مو / پوست / ناخن). Only the section whose
 * [expanded] is true renders [content]; the caller (one shared
 * `expandedSection` in [BeautyDnaScreen]) guarantees at most one section is
 * expanded at a time, so opening a section always closes the others.
 *
 * RTL reading order matches [RefListRow]: the expand/collapse indicator sits
 * on the physical left, the title + selection summary is a right-anchored
 * weighted column. The summary stays visible whether the section is
 * expanded or collapsed, so a user never has to open a section to see what
 * they already picked. `heightIn(min = MinTouchTarget)` keeps the header a
 * full 48dp tap target even when there is no summary line.
 */
@Composable
private fun AccordionSection(
    title: String,
    summary: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
        RefSurface {
            Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .rojanPressable(onClick = onToggle, role = Role.Button)
                        .heightIn(min = RojanDimens.MinTouchTarget)
                        .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowDown else Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = if (expanded) "بستن بخش $title" else "باز کردن بخش $title",
                        tint = HomeColors.TextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(RojanDimens.SpaceSM))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            title,
                            style = RojanTypography.Body.copy(fontWeight = FontWeight.SemiBold),
                            color = HomeColors.TextPrimary,
                        )
                        if (summary != null) {
                            Spacer(Modifier.height(RojanDimens.SpaceXS))
                            Text(
                                summary,
                                style = RojanTypography.Caption,
                                color = CustomerAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (expanded) {
                    RefRowDivider()
                    content()
                }
            }
        }
        Spacer(Modifier.height(RojanDimens.SpaceMD))
    }
}

/** One labelled sub-group of rows inside an expanded [AccordionSection]. */
@Composable
private fun DnaOptionGroup(label: String, isFirst: Boolean = false, rows: @Composable () -> Unit) {
    if (!isFirst) RefRowDivider()
    Text(
        label,
        style = RojanTypography.Caption,
        color = HomeColors.TextMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
    )
    rows()
}

/** [onSelect] receives the option, or `null` when the currently-selected option is tapped again (deselect) — matches the original single-select toggle behaviour. */
@Composable
private fun SingleSelectRows(options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    options.forEachIndexed { index, option ->
        if (index > 0) RefRowDivider()
        OptionRow(
            title = option,
            selected = option == selected,
            onClick = { onSelect(if (option == selected) null else option) },
        )
    }
}

@Composable
private fun MultiSelectRows(options: List<String>, selected: List<String>, onToggle: (String) -> Unit) {
    options.forEachIndexed { index, option ->
        if (index > 0) RefRowDivider()
        OptionRow(
            title = option,
            selected = selected.contains(option),
            onClick = { onToggle(option) },
        )
    }
}

@Composable
private fun OptionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val leadingCheck: (@Composable () -> Unit)? = if (selected) {
        {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "انتخاب‌شده",
                tint = CustomerAccent,
                modifier = Modifier.size(20.dp),
            )
        }
    } else {
        null
    }
    RefListRow(
        title = title,
        showChevron = false,
        onClick = onClick,
        leading = leadingCheck,
    )
}
