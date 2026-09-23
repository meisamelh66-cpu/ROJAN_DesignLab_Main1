package ai.rojan.designlab.screens.specialist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonSearch
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.specialist.SpecialistProfileViewModel
import ai.rojan.designlab.presentation.specialist.SpecialistProfileViewModelFactory
import ai.rojan.designlab.screens.customer.components.CustomerHairline
import ai.rojan.designlab.screens.customer.components.CustomerLoadingState
import ai.rojan.designlab.screens.customer.components.CustomerEmptyState
import ai.rojan.designlab.screens.customer.components.CustomerErrorState
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabel
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill
import ai.rojan.designlab.screens.customer.components.RefListRow
import ai.rojan.designlab.screens.customer.components.RefRowDivider
import ai.rojan.designlab.screens.customer.components.RefSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.image.SpecialistAvatar
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 1, Screen 3: Specialist Profile.
 *
 * Backed by [SpecialistProfileViewModel] ->
 * `GET /api/v1/salons/{salonId}/specialists/{specialistId}` plus the same
 * category/service fan-out Salon Detail uses. [salonId] nullable — when
 * absent, a disclosed error rather than a guess. The backend `Specialist`
 * has no experience/rating/reviews concept — those sections are absent, not
 * faked.
 *
 * Quiet Luxury pass (visual only). The `GlassBackButton` orb, the bare
 * `HeroTitle`, the `HomeGlassSurface` bio + service cards, the
 * `RojanSoftLavender`/…-family avatar tint, the `RtlListRow` /
 * `RtlSectionHeader` primitives, the per-item `rojanEnterAnimation` stagger,
 * and the glass `RojanLoadingState` / `RojanErrorState` are replaced with
 * the [CustomerScaffold] shell and the flat foundation primitives:
 * [RefSurface] card, one divided [RefListRow] service list,
 * [CustomerSectionLabel], and [CustomerLoadingState] / [CustomerEmptyState]
 * / [CustomerErrorState].
 *
 * NOTHING about behaviour changed: `retry()` and every `on*` callback
 * ([onBackClick], [onServiceClick]) are called exactly where they were. No
 * ViewModel, repository, API, or navigation route is touched.
 */
@Composable
fun SpecialistProfileScreen(
    specialistId: String,
    salonId: String?,
    onBackClick: () -> Unit,
    onServiceClick: (String) -> Unit,
    // Guest Booking Flow fix: the salon's public slug, threaded from the
    // shared BookingViewModel (see RojanNavGraph.kt).
    slug: String? = null,
    viewModel: SpecialistProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            SpecialistProfileViewModelFactory(
                salonId = salonId,
                specialistId = specialistId,
                specialistRepository = container.specialistRepository,
                serviceCategoryRepository = container.serviceCategoryRepository,
                serviceRepository = container.serviceRepository,
                publicSalonRepository = container.publicSalonRepository,
                slug = slug,
                hasSession = { container.tokenRepository.accessToken()?.isNotBlank() == true },
            )
        },
    ),
) {
    CustomerScaffold(title = "متخصص", onBackClick = onBackClick) {
        when (val loadState = viewModel.state) {
            is UiState.Loading -> CustomerLoadingState(
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
            )

            is UiState.Empty -> CustomerEmptyState(
                title = "متخصص یافت نشد",
                icon = Icons.Outlined.PersonSearch,
            )

            is UiState.Error -> CustomerErrorState(
                message = loadState.message,
                onRetry = viewModel::retry,
            )

            is UiState.Success -> {
                val specialist = loadState.data.specialist
                val services = loadState.data.services

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = RojanDimens.SpaceLG, bottom = RojanDimens.SpaceXL),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = CustomerScreenMargin),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(CustomerSurfaceFill)
                                    .border(1.dp, CustomerHairline, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                SpecialistAvatar(
                                    assetRes = null,
                                    contentDescription = specialist.displayName,
                                    fallbackIconSize = 40.dp,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Spacer(Modifier.height(RojanDimens.SpaceMD))
                            Text(
                                specialist.displayName,
                                style = RojanTypography.Display.copy(fontSize = 26.sp, lineHeight = 34.sp),
                                color = HomeColors.TextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    specialist.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                        item { Spacer(Modifier.height(RojanDimens.SpaceMD)) }
                        item {
                            RefSurface(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
                                Text(
                                    bio,
                                    style = RojanTypography.Body,
                                    color = HomeColors.TextSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(RojanDimens.SpaceMD),
                                )
                            }
                        }
                    }

                    if (services.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(RojanDimens.SpaceXL))
                            CustomerSectionLabel("خدمات قابل رزرو")
                            Spacer(Modifier.height(RojanDimens.SpaceSM))
                        }
                        item {
                            RefSurface(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    services.forEachIndexed { index, service ->
                                        if (index > 0) RefRowDivider()
                                        RefListRow(
                                            title = service.name,
                                            trailingValue = "${service.durationMinutes} دقیقه",
                                            onClick = { onServiceClick(service.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
