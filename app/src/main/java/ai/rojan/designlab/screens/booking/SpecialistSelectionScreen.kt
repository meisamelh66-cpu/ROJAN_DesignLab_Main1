package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.specialist.SpecialistSelectionViewModel
import ai.rojan.designlab.presentation.specialist.SpecialistSelectionViewModelFactory
import ai.rojan.designlab.screens.bookingflow.components.BookingCenteredState
import ai.rojan.designlab.screens.bookingflow.components.BookingLoadingRows
import ai.rojan.designlab.screens.bookingflow.components.BookingScaffold
import ai.rojan.designlab.screens.bookingflow.components.BookingScreenMargin
import ai.rojan.designlab.screens.bookingflow.components.RefSelectableCell
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Booking journey, Screen 1 (of the visible steps): Specialist Selection.
 *
 * Quiet Luxury pass (visual only): the floating `GlassBackButton` orb, the
 * bare `HeroTitle`, the staggered `rojanEnterAnimation` entrance, and the
 * `HomeGlassSurface` rows with `accentFor()` colour-tinted avatar circles are
 * replaced with the [BookingScaffold] shell (step 1 of 5) and flat
 * `bookingflow.components` — clean specialist rows (initials avatar, no colour
 * tint), a rose-gold selected state, and calm loading / empty / error states.
 *
 * NOTHING about the data flow changed: [SpecialistSelectionViewModel] still
 * calls `GET /api/v1/salons/{salonId}/specialists`, the factory is
 * byte-identical, and [onSpecialistSelected] still fires `specialist.id`. This
 * screen is only navigated to when the salon has 2+ specialists (the 1-
 * specialist auto-skip stays in Navigation). No ViewModel, booking
 * state/context, navigation, or API is modified here.
 *
 * The selected state is a screen-local `rememberSaveable` — the row the user
 * tapped turns rose-gold and is restored on back-navigation from the date
 * screen. A persistent pre-selection when *editing* the specialist from the
 * Confirmation screen would need the booking state threaded in via Navigation
 * (out of this visual-only pass).
 */
@Composable
fun SpecialistSelectionScreen(
    salonId: String,
    onBackClick: () -> Unit,
    onSpecialistSelected: (String) -> Unit,
    // Guest Booking Flow fix: the salon's public slug, threaded from the
    // shared BookingViewModel (see RojanNavGraph.kt).
    slug: String? = null,
    viewModel: SpecialistSelectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            SpecialistSelectionViewModelFactory(
                salonId = salonId,
                specialistRepository = container.specialistRepository,
                publicSalonRepository = container.publicSalonRepository,
                slug = slug,
                hasSession = { container.tokenRepository.accessToken()?.isNotBlank() == true },
            )
        },
    ),
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    BookingScaffold(
        title = "انتخاب متخصص",
        onBackClick = onBackClick,
        step = 1,
    ) {
        when (val state = viewModel.state) {
            is UiState.Loading -> BookingLoadingRows(
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
                count = 4,
                rowHeight = 72,
            )

            is UiState.Error -> BookingCenteredState(
                icon = Icons.Outlined.CloudOff,
                title = "مشکلی پیش آمد",
                body = state.message,
                actionLabel = "تلاش مجدد",
                onAction = { viewModel.retry() },
            )

            is UiState.Empty -> BookingCenteredState(
                icon = Icons.Outlined.SearchOff,
                title = "متخصصی برای این سالن یافت نشد",
                body = "در حال حاضر متخصصی برای رزرو در دسترس نیست.",
            )

            is UiState.Success -> LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = BookingScreenMargin,
                    vertical = RojanDimens.SpaceLG,
                ),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                items(state.data, key = { it.id }) { specialist ->
                    SpecialistRow(
                        specialist = specialist,
                        selected = specialist.id == selectedId,
                        onClick = {
                            selectedId = specialist.id
                            onSpecialistSelected(specialist.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecialistRow(specialist: Specialist, selected: Boolean, onClick: () -> Unit) {
    RefSelectableCell(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = false,
    ) { contentColor ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (selected) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    specialist.displayName,
                    style = RojanTypography.Body,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                specialist.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                    Text(
                        bio,
                        style = RojanTypography.Caption,
                        color = if (selected) contentColor.copy(alpha = 0.82f) else HomeColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(RojanDimens.SpaceMD))
            SpecialistAvatar(
                name = specialist.displayName,
                photoUrl = specialist.photoUrl,
                selected = selected,
                contentColor = contentColor,
            )
        }
    }
}

@Composable
private fun SpecialistAvatar(
    name: String,
    photoUrl: String?,
    selected: Boolean,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                color = Color.White.copy(alpha = if (selected) 0.22f else 0.05f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        RojanRemoteImage(
            url = photoUrl,
            contentDescription = name,
            shape = CircleShape,
            modifier = Modifier.fillMaxSize(),
            fallback = {
                Text(
                    name.trim().take(1),
                    style = RojanTypography.CardTitle,
                    color = contentColor,
                )
            },
        )
    }
}
