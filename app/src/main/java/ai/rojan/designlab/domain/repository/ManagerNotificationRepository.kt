package ai.rojan.designlab.domain.repository

enum class NotificationType {
    BOOKING_CREATED,
    BOOKING_CANCELLED,
    UNKNOWN;

    companion object {
        fun fromString(value: String): NotificationType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

enum class NotificationSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ERROR;

    companion object {
        fun fromString(value: String): NotificationSeverity =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: INFO
    }
}

data class ManagerNotification(
    val id: String,
    val salonId: String,
    val userId: String?,
    val type: NotificationType,
    val title: String,
    val message: String,
    val severity: NotificationSeverity,
    val category: String,
    val referenceId: String?,
    val referenceType: String?,
    val isRead: Boolean,
    val readAt: String?,
    val createdAt: String,
)

interface ManagerNotificationRepository {

    suspend fun getNotifications(
        salonId: String,
        page: Int = 0,
        size: Int = 20,
        unreadOnly: Boolean = false,
        category: String? = null,
    ): Result<List<ManagerNotification>>

    suspend fun getUnreadCount(salonId: String): Result<Long>

    suspend fun markAsRead(
        salonId: String,
        notificationId: String,
    ): Result<ManagerNotification>

    suspend fun markAllAsRead(salonId: String): Result<Unit>
}
