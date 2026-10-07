package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponseDto(
    val id: String,
    val salonId: String,
    val userId: String? = null,
    val type: String,
    val title: String,
    val message: String,
    val severity: String,
    val category: String,
    val referenceId: String? = null,
    val referenceType: String? = null,
    val isRead: Boolean,
    val readAt: String? = null,
    val readBy: String? = null,
    val createdAt: String,
)

@Serializable
data class UnreadCountResponseDto(
    val salonId: String,
    val unreadCount: Long,
)

@Serializable
data class MarkAllReadResponseDto(
    val salonId: String,
    val markedCount: Int,
)
