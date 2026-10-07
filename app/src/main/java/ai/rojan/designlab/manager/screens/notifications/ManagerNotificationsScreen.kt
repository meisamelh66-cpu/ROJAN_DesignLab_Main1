package ai.rojan.designlab.manager.screens.notifications

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerEmptyState
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.toPersianDigits
import ai.rojan.designlab.domain.repository.ManagerNotification
import ai.rojan.designlab.domain.repository.NotificationSeverity
import ai.rojan.designlab.manager.presentation.notifications.ManagerNotificationsViewModel
import ai.rojan.designlab.manager.presentation.notifications.ManagerNotificationsViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ManagerNotificationsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onRequireLogin: () -> Unit = {},
    viewModel: ManagerNotificationsViewModel = viewModel(
        factory = ManagerNotificationsViewModelFactory(
            activeSalonContextRepository = BackendApiContainerHolder.get(LocalContext.current).activeSalonContextRepository,
            notificationRepository = BackendApiContainerHolder.get(LocalContext.current).managerNotificationRepository,
        ),
    ),
) {
    LaunchedEffect(viewModel.requiresReauth) {
        if (viewModel.requiresReauth) onRequireLogin()
    }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            when (val currentState = viewModel.state) {
                is UiState.Loading -> {
                    ManagerLoadingState(
                        modifier = Modifier.padding(top = RojanDimens.SpaceMD),
                        message = "در حال بارگذاری اعلان‌ها...",
                    )
                }

                is UiState.Error -> {
                    ManagerErrorState(
                        modifier = Modifier.padding(top = RojanDimens.SpaceMD),
                        description = currentState.message,
                        actionLabel = "تلاش مجدد",
                        onAction = { viewModel.retry() },
                    )
                }

                is UiState.Empty -> {
                    ManagerEmptyState(
                        modifier = Modifier.padding(top = RojanDimens.SpaceMD),
                        title = "اعلانی وجود ندارد",
                        description = "در حال حاضر هیچ اعلانی برای این سالن ثبت نشده است.",
                    )
                }

                is UiState.Success -> {
                    val data = currentState.data
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            Text(
                                text = "اعلان‌ها",
                                style = RojanTypography.ScreenTitle,
                                color = ManagerColors.TextPrimary,
                            )
                            if (data.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ManagerColors.Turquoise.copy(alpha = 0.2f))
                                        .border(1.dp, ManagerColors.Turquoise.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "${data.unreadCount.toPersianDigits()} خوانده‌نشده",
                                        style = RojanTypography.Caption,
                                        color = ManagerColors.Turquoise,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        if (data.unreadCount > 0) {
                            if (viewModel.isActionInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ManagerColors.Turquoise,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = "خواندن همه",
                                    style = RojanTypography.Caption,
                                    color = ManagerColors.Turquoise,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.rojanPressable(onClick = { viewModel.markAllAsRead() }),
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    ) {
                        items(data.notifications, key = { it.id }) { notification ->
                            NotificationItemCard(
                                notification = notification,
                                onMarkAsRead = { viewModel.markAsRead(notification.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: ManagerNotification,
    onMarkAsRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val severityColor = when (notification.severity) {
        NotificationSeverity.INFO -> ManagerColors.Turquoise
        NotificationSeverity.SUCCESS -> ManagerColors.Gold
        NotificationSeverity.WARNING -> ManagerColors.Gold
        NotificationSeverity.ERROR -> Color(0xFFEF5350)
    }

    ManagerGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
        borderAlpha = if (notification.isRead) 0.20f else 0.45f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ManagerColors.Turquoise),
                        )
                    }
                    Text(
                        text = notification.title,
                        style = RojanTypography.CardTitle,
                        color = if (notification.isRead) ManagerColors.TextSecondary else ManagerColors.TextPrimary,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .border(1.dp, severityColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = when (notification.severity) {
                            NotificationSeverity.INFO -> "اطلاع"
                            NotificationSeverity.SUCCESS -> "موفق"
                            NotificationSeverity.WARNING -> "هشدار"
                            NotificationSeverity.ERROR -> "خطا"
                        },
                        fontSize = 11.sp,
                        color = severityColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Text(
                text = notification.message,
                style = RojanTypography.Body,
                color = if (notification.isRead) ManagerColors.TextSecondary.copy(alpha = 0.8f) else ManagerColors.TextPrimary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (notification.referenceType != null) {
                    Text(
                        text = "مرجع: ${notification.referenceType}",
                        style = RojanTypography.Caption,
                        color = ManagerColors.TextSecondary.copy(alpha = 0.7f),
                    )
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }

                if (!notification.isRead) {
                    Row(
                        modifier = Modifier
                            .rojanPressable(onClick = onMarkAsRead)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "علامت‌گذاری به عنوان خوانده شده",
                            tint = ManagerColors.Turquoise,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "خوانده شد",
                            style = RojanTypography.Caption,
                            color = ManagerColors.Turquoise,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
