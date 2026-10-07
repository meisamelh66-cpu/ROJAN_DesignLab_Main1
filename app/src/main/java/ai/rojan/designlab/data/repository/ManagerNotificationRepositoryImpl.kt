package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.ManagerNotificationApi
import ai.rojan.designlab.data.remote.dto.NotificationResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.ManagerNotification
import ai.rojan.designlab.domain.repository.ManagerNotificationRepository
import ai.rojan.designlab.domain.repository.NotificationSeverity
import ai.rojan.designlab.domain.repository.NotificationType

class ManagerNotificationRepositoryImpl(
    private val notificationApi: ManagerNotificationApi,
) : ManagerNotificationRepository {

    override suspend fun getNotifications(
        salonId: String,
        page: Int,
        size: Int,
        unreadOnly: Boolean,
        category: String?,
    ): Result<List<ManagerNotification>> =
        safeApiCall {
            notificationApi.getNotifications(
                salonId = salonId,
                page = page,
                size = size,
                unreadOnly = unreadOnly,
                category = category,
            )
        }.map { paged ->
            paged.content.map { it.toDomain() }
        }

    override suspend fun getUnreadCount(salonId: String): Result<Long> =
        safeApiCall {
            notificationApi.getUnreadCount(salonId = salonId)
        }.map { it.unreadCount }

    override suspend fun markAsRead(
        salonId: String,
        notificationId: String,
    ): Result<ManagerNotification> =
        safeApiCall {
            notificationApi.markAsRead(salonId = salonId, notificationId = notificationId)
        }.map { it.toDomain() }

    override suspend fun markAllAsRead(salonId: String): Result<Unit> =
        safeApiCall {
            notificationApi.markAllAsRead(salonId = salonId)
        }.map { }

    private fun NotificationResponseDto.toDomain() = ManagerNotification(
        id = id,
        salonId = salonId,
        userId = userId,
        type = NotificationType.fromString(type),
        title = title,
        message = message,
        severity = NotificationSeverity.fromString(severity),
        category = category,
        referenceId = referenceId,
        referenceType = referenceType,
        isRead = isRead,
        readAt = readAt,
        createdAt = createdAt,
    )
}
