package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.data.demo.AppointmentStatus
import ai.rojan.designlab.data.demo.DemoUserReview
import ai.rojan.designlab.domain.customer.ReviewLifecycleStatus
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 2, Appointment History completion (checklist item 8) + Review
 * lifecycle UI (item 4). Reached by tapping any appointment card.
 *
 * Disclosed scope decision: "Invoice"/"Receipt"/"Download" are
 * implemented as a real in-app detail view of the transaction, not
 * actual PDF file generation/export — building a genuine document
 * pipeline is meaningfully larger scope than the rest of this
 * checklist, and I'm not silently treating "shows the same information
 * on screen" as equivalent to "produces a downloadable file."
 */
@Composable
fun AppointmentDetailsScreen(
    appointmentId: String,
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
    onRebookClick: (serviceId: String) -> Unit,
) {
    val appointment = ecosystemViewModel.state.appointments.find { it.id == appointmentId }
    val pendingReview = ecosystemViewModel.state.pendingReviewFor(appointmentId)

    HomeBackgroundTheme {
        if (appointment == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("نوبت یافت نشد", color = HomeColors.TextPrimary, style = RojanTypography.Body)
            }
            return@HomeBackgroundTheme
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("جزئیات نوبت", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }

            item {
                HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                    Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
                        Text(appointment.salonName, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                        Text(
                            "${appointment.serviceName} • ${appointment.specialistName}",
                            style = RojanTypography.Caption,
                            color = HomeColors.TextSecondary,
                        )
                        Text(appointment.dateLabel, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                    }
                }
            }

            item {
                // Receipt / Invoice — real in-app detail view (see disclosed scope decision above)
                HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                    Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Receipt, contentDescription = null, tint = HomeColors.Glow)
                            Text(" رسید و فاکتور", style = RojanTypography.Body, color = HomeColors.TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                        InvoiceRow("خدمت", appointment.serviceName)
                        InvoiceRow("مبلغ", "${appointment.price} تومان")
                        InvoiceRow("تاریخ", appointment.dateLabel)
                        InvoiceRow("شماره پیگیری", appointment.id)
                    }
                }
            }

            item {
                // Photos — placeholder tiles, no new assets, same tinted-square language used everywhere else
                Text("تصاویر", style = RojanTypography.Body, color = HomeColors.TextPrimary)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(HomeColors.Lavender.copy(alpha = 0.4f), RojanShapes.Small),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = HomeColors.TextSecondary)
                        }
                    }
                }
            }

            if (appointment.relatedServiceId != null) {
                item {
                    PremiumButton(
                        text = "رزرو مجدد",
                        onClick = { onRebookClick(appointment.relatedServiceId) },
                    )
                }
            }

            if (appointment.status == AppointmentStatus.COMPLETED && pendingReview != null) {
                item { ReviewSection(appointmentId, pendingReview.status, ecosystemViewModel) }
            }
        }
    }
}

@Composable
private fun InvoiceRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
        Text(value, style = RojanTypography.Caption, color = HomeColors.TextPrimary)
    }
}

@Composable
private fun ReviewSection(
    appointmentId: String,
    status: ReviewLifecycleStatus,
    ecosystemViewModel: CustomerEcosystemViewModel,
) {
    when (status) {
        ReviewLifecycleStatus.REQUESTED -> {
            var text by remember { mutableStateOf("") }
            val salonName = ecosystemViewModel.state.appointments
                .find { it.id == appointmentId }?.salonName ?: ""

            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
                    Text("نظر خود را ثبت کنید", style = RojanTypography.Body, color = HomeColors.TextPrimary)
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HomeColors.Lavender.copy(alpha = 0.3f), RojanShapes.Small)
                            .padding(RojanDimens.SpaceSM),
                        textStyle = RojanTypography.Body.copy(color = HomeColors.TextPrimary).withDirectionFor(text),
                        cursorBrush = SolidColor(HomeColors.Glow),
                    )
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                    PremiumButton(
                        text = "ثبت نظر",
                        onClick = {
                            if (text.isNotBlank()) {
                                // Submit only — publishing is a deliberately
                                // separate step (see CustomerEcosystemEngine's
                                // own doc comment on publishReview), not
                                // auto-chained here. A real moderation/
                                // approval step, if BOOK 3 specifies one,
                                // would call publish independently.
                                ecosystemViewModel.submitReview(
                                    appointmentId,
                                    DemoUserReview(
                                        id = "ureview_$appointmentId",
                                        salonName = salonName,
                                        rating = "5.0",
                                        comment = text,
                                        dateLabel = "امروز",
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
        ReviewLifecycleStatus.SUBMITTED -> {
            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                Text(
                    "نظر شما ثبت شد و در انتظار انتشار است",
                    style = RojanTypography.Body,
                    color = HomeColors.TextPrimary,
                    modifier = Modifier.padding(RojanDimens.SpaceMD),
                )
            }
        }
        ReviewLifecycleStatus.PUBLISHED -> {
            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                Text(
                    "نظر شما منتشر شد",
                    style = RojanTypography.Body,
                    color = HomeColors.TextPrimary,
                    modifier = Modifier.padding(RojanDimens.SpaceMD),
                )
            }
        }
        ReviewLifecycleStatus.PENDING_REQUEST -> Unit
    }
}
