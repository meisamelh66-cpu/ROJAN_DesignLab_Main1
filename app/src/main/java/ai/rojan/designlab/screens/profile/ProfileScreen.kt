package ai.rojan.designlab.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.profile.ProfileMediaViewModel
import ai.rojan.designlab.presentation.profile.ProfileMediaViewModelFactory
import ai.rojan.designlab.screens.customer.components.CustomerAccent
import ai.rojan.designlab.screens.customer.components.CustomerCardShape
import ai.rojan.designlab.screens.customer.components.CustomerConfirmDialog
import ai.rojan.designlab.screens.customer.components.CustomerHairline
import ai.rojan.designlab.screens.customer.components.CustomerOnAccent
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabel
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill
import ai.rojan.designlab.screens.customer.components.RefListRow
import ai.rojan.designlab.screens.customer.components.RefRowDivider
import ai.rojan.designlab.screens.customer.components.RefSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.media.CustomerImageBounds
import ai.rojan.designlab.ui.media.ImageOnlyPickerRequest
import ai.rojan.designlab.ui.media.decodeResizeAndCompress
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanTypography

private data class ProfileMenuItem(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
)

/**
 * Journey 2, Screen 1: Profile hub — the customer's real identity center.
 *
 * Customer Profile Personalization Phase 5B: the slim name+initial header is
 * now a real editable header — a full-width cover band with a 96dp avatar
 * overlapping its bottom edge, a rose-gold "تایید شده" chip for a verified
 * phone, and camera-badge edit affordances (tap → the shared Photo Picker →
 * the `ui/media/ImageDownscale.kt` pipeline → `POST /api/v1/users/me/media/(avatar|cover)`).
 * Per-slot uploading spinners; a quiet inline error line; a
 * [CustomerConfirmDialog]-gated "remove" link per image. A guest (no
 * [AuthViewModel.currentUser]) sees the placeholder header with no edit
 * affordances. Everything below the header — the "اطلاعات شخصی" card, the
 * three [ProfileMenuItem] groups, and the [CustomerConfirmDialog]-gated
 * logout — is unchanged.
 *
 * NOTHING else about behaviour changed: displayed name still comes from
 * [AuthViewModel.currentDisplayName], contact fields from
 * [AuthViewModel.currentUser], and every navigation callback — including
 * [onLogoutClick] — is called exactly where it was. The media edit is the
 * only new capability; it goes through [ProfileMediaViewModel], whose
 * successes flow back into [AuthViewModel.applyUpdatedUser] (the single
 * source of truth this screen renders from). No navigation route, no
 * session/auth logic, and no other data model is touched.
 */
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onBeautyDnaClick: () -> Unit,
    onAppointmentsClick: () -> Unit,
    onFollowedSalonsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onWalletClick: () -> Unit,
    onCouponsClick: () -> Unit,
    onMembershipClick: () -> Unit,
    onLoyaltyClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onBeautyTimelineClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val displayName = authViewModel.currentDisplayName ?: "کاربر"
    val isGuest = currentUser == null

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showRemoveAvatarConfirm by remember { mutableStateOf(false) }
    var showRemoveCoverConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mediaViewModel: ProfileMediaViewModel = viewModel(
        factory = ProfileMediaViewModelFactory(
            repository = BackendApiContainerHolder.get(context).userProfileRepository,
            onUserUpdated = authViewModel::applyUpdatedUser,
        ),
    )
    val mediaState by mediaViewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { picked ->
            coroutineScope.launch {
                withContext(Dispatchers.Default) {
                    decodeResizeAndCompress(picked, context, CustomerImageBounds.AVATAR_MAX_DIMENSION)
                }?.let { (bytes, name, mime) -> mediaViewModel.uploadAvatar(bytes, name, mime) }
            }
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { picked ->
            coroutineScope.launch {
                withContext(Dispatchers.Default) {
                    decodeResizeAndCompress(picked, context, CustomerImageBounds.COVER_MAX_DIMENSION)
                }?.let { (bytes, name, mime) -> mediaViewModel.uploadCover(bytes, name, mime) }
            }
        }
    }

    val activityItems = listOf(
        ProfileMenuItem(Icons.Outlined.CalendarMonth, "نوبت‌های من", onAppointmentsClick),
        ProfileMenuItem(Icons.Outlined.Spa, "بیوتی DNA من", onBeautyDnaClick),
        ProfileMenuItem(Icons.Outlined.History, "تاریخچه زیبایی", onBeautyTimelineClick),
        ProfileMenuItem(Icons.Outlined.RateReview, "نظرات من", onReviewsClick),
    )
    val salonItems = listOf(
        ProfileMenuItem(Icons.Outlined.FavoriteBorder, "علاقه‌مندی‌ها", onFavoritesClick),
        ProfileMenuItem(Icons.Outlined.NotificationsNone, "سالن‌های دنبال‌شده", onFollowedSalonsClick),
    )
    val accountItems = listOf(
        ProfileMenuItem(Icons.Outlined.AccountBalanceWallet, "کیف پول", onWalletClick),
        ProfileMenuItem(Icons.Outlined.CardGiftcard, "کدهای تخفیف", onCouponsClick),
        ProfileMenuItem(Icons.Outlined.WorkspacePremium, "عضویت", onMembershipClick),
        ProfileMenuItem(Icons.Outlined.Stars, "امتیازات وفاداری", onLoyaltyClick),
    )

    CustomerScaffold(title = "حساب کاربری", onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ProfileHeader(
                name = displayName,
                avatarUrl = currentUser?.avatarUrl,
                coverUrl = currentUser?.coverUrl,
                phoneNumber = currentUser?.phoneNumber,
                editable = !isGuest,
                isUploadingAvatar = mediaState.isUploadingAvatar,
                isUploadingCover = mediaState.isUploadingCover,
                onEditAvatar = { avatarPicker.launch(ImageOnlyPickerRequest) },
                onEditCover = { coverPicker.launch(ImageOnlyPickerRequest) },
                onRemoveAvatar = if (!isGuest && currentUser?.avatarUrl != null) {
                    { showRemoveAvatarConfirm = true }
                } else {
                    null
                },
                onRemoveCover = if (!isGuest && currentUser?.coverUrl != null) {
                    { showRemoveCoverConfirm = true }
                } else {
                    null
                },
            )

            mediaState.errorMessage?.let { message ->
                Spacer(Modifier.height(RojanDimens.SpaceSM))
                Text(
                    text = message,
                    style = RojanTypography.Caption,
                    color = RojanErrorText,
                    modifier = Modifier
                        .padding(horizontal = CustomerScreenMargin)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .rojanPressable(onClick = mediaViewModel::dismissError),
                )
            }

            Spacer(Modifier.height(RojanDimens.SpaceXL))

            CustomerSectionLabel("اطلاعات شخصی")
            Spacer(Modifier.height(RojanDimens.SpaceSM))
            RefSurface(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val phone = currentUser?.phoneNumber
                    val email = currentUser?.email?.takeIf { it.isNotBlank() }
                    when {
                        phone == null && email == null -> {
                            RefListRow(
                                title = "اطلاعاتی ثبت نشده است",
                                showChevron = false,
                                leading = { RowIcon(Icons.Outlined.Person) },
                            )
                        }
                        else -> {
                            phone?.let {
                                RefListRow(
                                    title = it,
                                    trailingValue = "تایید شده",
                                    trailingValueColor = CustomerAccent,
                                    showChevron = false,
                                    leading = { RowIcon(Icons.Outlined.Phone) },
                                )
                            }
                            if (phone != null && email != null) RefRowDivider()
                            email?.let {
                                RefListRow(
                                    title = it,
                                    showChevron = false,
                                    leading = { RowIcon(Icons.Outlined.MailOutline) },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(RojanDimens.SpaceXL))
            MenuGroup("فعالیت من", activityItems)

            Spacer(Modifier.height(RojanDimens.SpaceLG))
            MenuGroup("سالن‌های من", salonItems)

            Spacer(Modifier.height(RojanDimens.SpaceLG))
            MenuGroup("امکانات حساب", accountItems)

            Spacer(Modifier.height(RojanDimens.SpaceXL))
            LogoutRow(onClick = { showLogoutConfirm = true })

            Spacer(Modifier.height(RojanDimens.SpaceLG))
        }
    }

    if (showLogoutConfirm) {
        CustomerConfirmDialog(
            title = "خروج از حساب",
            message = "از حساب کاربری خود خارج می‌شوید؟",
            confirmLabel = "خروج",
            onConfirm = {
                showLogoutConfirm = false
                onLogoutClick()
            },
            onDismiss = { showLogoutConfirm = false },
        )
    }

    if (showRemoveAvatarConfirm) {
        CustomerConfirmDialog(
            title = "حذف عکس نمایه",
            message = "عکس نمایه شما حذف شود؟",
            confirmLabel = "حذف",
            onConfirm = {
                showRemoveAvatarConfirm = false
                mediaViewModel.removeAvatar()
            },
            onDismiss = { showRemoveAvatarConfirm = false },
        )
    }

    if (showRemoveCoverConfirm) {
        CustomerConfirmDialog(
            title = "حذف تصویر کاور",
            message = "تصویر کاور شما حذف شود؟",
            confirmLabel = "حذف",
            onConfirm = {
                showRemoveCoverConfirm = false
                mediaViewModel.removeCover()
            },
            onDismiss = { showRemoveCoverConfirm = false },
        )
    }
}

// --- Editable identity header (Phase 5B) --------------------------------

private val CoverHeight = 168.dp
private val AvatarSize = 96.dp
private val AvatarOverhang = 44.dp

@Composable
private fun ProfileHeader(
    name: String,
    avatarUrl: String?,
    coverUrl: String?,
    phoneNumber: String?,
    editable: Boolean,
    isUploadingAvatar: Boolean,
    isUploadingCover: Boolean,
    onEditAvatar: () -> Unit,
    onEditCover: () -> Unit,
    onRemoveAvatar: (() -> Unit)?,
    onRemoveCover: (() -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CoverHeight + AvatarOverhang),
        ) {
            // Cover band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CoverHeight)
                    .then(if (editable && !isUploadingCover) Modifier.rojanPressable(onClick = onEditCover, role = Role.Button) else Modifier),
            ) {
                RojanRemoteImage(
                    url = coverUrl,
                    contentDescription = "تصویر کاور",
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxSize(),
                    fallback = { Box(Modifier.fillMaxSize().background(CustomerSurfaceFill)) },
                )
                // Calm fade toward the page ground so the avatar and name sit on a settled base.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                0.45f to Color.Transparent,
                                1f to HomeColors.NavyBase.copy(alpha = 0.6f),
                            ),
                        ),
                )
                when {
                    isUploadingCover -> UploadingOverlay(shape = RectangleShape)
                    editable -> EditBadge(
                        contentDescription = "تغییر تصویر کاور",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(RojanDimens.SpaceMD),
                    )
                }
            }

            // Avatar, overlapping the cover's bottom edge (RTL: anchored to the
            // trailing/right edge, matching every other leading-visual position
            // in this screen and the rest of the Customer app)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = CustomerScreenMargin)
                    .size(AvatarSize),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(HomeColors.NavyBase)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(CustomerSurfaceFill)
                        .border(1.dp, CustomerHairline, CircleShape)
                        .then(if (editable && !isUploadingAvatar) Modifier.rojanPressable(onClick = onEditAvatar, role = Role.Button) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    RojanRemoteImage(
                        url = avatarUrl,
                        contentDescription = "عکس نمایه",
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize(),
                        fallback = { AvatarInitial(name) },
                    )
                }
                when {
                    isUploadingAvatar -> UploadingOverlay(shape = CircleShape)
                    editable -> EditBadge(
                        contentDescription = "تغییر عکس نمایه",
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }
            }
        }

        Spacer(Modifier.height(RojanDimens.SpaceMD))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CustomerScreenMargin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                name,
                style = RojanTypography.Display.copy(fontSize = 26.sp, lineHeight = 34.sp),
                color = HomeColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (phoneNumber != null) {
                Spacer(Modifier.width(RojanDimens.SpaceMD))
                VerifiedPhoneChip()
            }
        }

        if (onRemoveAvatar != null || onRemoveCover != null) {
            Spacer(Modifier.height(RojanDimens.SpaceSM))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CustomerScreenMargin),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onRemoveCover?.let { RemoveLink("حذف تصویر کاور", it) }
                if (onRemoveAvatar != null && onRemoveCover != null) Spacer(Modifier.width(RojanDimens.SpaceLG))
                onRemoveAvatar?.let { RemoveLink("حذف عکس نمایه", it) }
            }
        }
    }
}

@Composable
private fun AvatarInitial(name: String) {
    val initial = name.trim().firstOrNull()?.toString()
    if (initial != null) {
        Text(initial, style = RojanTypography.Display.copy(fontSize = 30.sp), color = HomeColors.TextSecondary)
    } else {
        Icon(
            Icons.Outlined.Person,
            contentDescription = null,
            tint = HomeColors.TextSecondary,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun EditBadge(contentDescription: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(CustomerAccent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.PhotoCamera,
            contentDescription = contentDescription,
            tint = CustomerOnAccent,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun UploadingOverlay(shape: androidx.compose.ui.graphics.Shape) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(HomeColors.NavyBase.copy(alpha = 0.55f))
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = CustomerAccent,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun VerifiedPhoneChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, CustomerAccent, RoundedCornerShape(999.dp))
            .padding(horizontal = RojanDimens.SpaceSM, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Verified,
            contentDescription = null,
            tint = CustomerAccent,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("تایید شده", style = RojanTypography.Caption, color = CustomerAccent)
    }
}

@Composable
private fun RemoveLink(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = RojanTypography.Caption,
        color = HomeColors.TextMuted,
        modifier = Modifier
            .rojanPressable(onClick = onClick, role = Role.Button)
            .padding(vertical = 2.dp),
    )
}

// --- Menu group (unchanged) --------------------------------------------

@Composable
private fun MenuGroup(label: String, items: List<ProfileMenuItem>) {
    CustomerSectionLabel(label)
    Spacer(Modifier.height(RojanDimens.SpaceSM))
    RefSurface(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                if (index > 0) RefRowDivider()
                RefListRow(
                    title = item.title,
                    onClick = item.onClick,
                    leading = { RowIcon(item.icon) },
                )
            }
        }
    }
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Icon(
        icon,
        contentDescription = null,
        tint = HomeColors.TextMuted,
        modifier = Modifier.size(22.dp),
    )
}

// --- Logout (unchanged) -----------------------------------------------

@Composable
private fun LogoutRow(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CustomerScreenMargin)
            .clip(CustomerCardShape)
            .background(CustomerSurfaceFill)
            .border(1.dp, CustomerHairline, CustomerCardShape)
            .heightIn(min = RojanDimens.MinTouchTarget)
            .rojanPressable(onClick = onClick, role = Role.Button)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = RojanErrorText,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(RojanDimens.SpaceSM))
            Text("خروج از حساب", style = RojanTypography.Button, color = RojanErrorText)
        }
    }
}
