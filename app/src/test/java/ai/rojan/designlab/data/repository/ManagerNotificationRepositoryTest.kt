package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.ManagerNotificationApi
import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.data.remote.dto.MarkAllReadResponseDto
import ai.rojan.designlab.data.remote.dto.NotificationResponseDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.UnreadCountResponseDto
import ai.rojan.designlab.domain.repository.NotificationSeverity
import ai.rojan.designlab.domain.repository.NotificationType
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ManagerNotificationRepositoryTest {

    private class FakeManagerNotificationApi : ManagerNotificationApi {
        var shouldThrow401 = false
        var shouldThrowNetwork = false

        val notifications = mutableListOf(
            NotificationResponseDto(
                id = "notif-1",
                salonId = "salon-1",
                userId = null,
                type = "BOOKING_CREATED",
                title = "نوبت جدید",
                message = "نوبت جدید ثبت شد",
                severity = "INFO",
                category = "bookings",
                referenceId = "booking-1",
                referenceType = "BOOKING",
                isRead = false,
                readAt = null,
                readBy = null,
                createdAt = "2026-10-07T10:00:00Z",
            ),
            NotificationResponseDto(
                id = "notif-2",
                salonId = "salon-1",
                userId = null,
                type = "BOOKING_CANCELLED",
                title = "لغو نوبت",
                message = "نوبت لغو شد",
                severity = "WARNING",
                category = "bookings",
                referenceId = "booking-2",
                referenceType = "BOOKING",
                isRead = true,
                readAt = "2026-10-07T10:30:00Z",
                readBy = "user-1",
                createdAt = "2026-10-07T09:00:00Z",
            ),
        )

        override suspend fun getNotifications(
            salonId: String,
            page: Int,
            size: Int,
            unreadOnly: Boolean,
            category: String?,
        ): PagedResponseDto<NotificationResponseDto> {
            checkErrors()
            val filtered = if (unreadOnly) notifications.filter { !it.isRead } else notifications
            return PagedResponseDto(
                content = filtered,
                page = page,
                size = size,
                totalElements = filtered.size.toLong(),
                totalPages = 1,
            )
        }

        override suspend fun getUnreadCount(salonId: String): UnreadCountResponseDto {
            checkErrors()
            val count = notifications.count { !it.isRead }.toLong()
            return UnreadCountResponseDto(salonId = salonId, unreadCount = count)
        }

        override suspend fun markAsRead(
            salonId: String,
            notificationId: String,
        ): NotificationResponseDto {
            checkErrors()
            val index = notifications.indexOfFirst { it.id == notificationId }
            val existing = notifications[index]
            val updated = existing.copy(isRead = true, readAt = "2026-10-07T11:00:00Z")
            notifications[index] = updated
            return updated
        }

        override suspend fun markAllAsRead(salonId: String): MarkAllReadResponseDto {
            checkErrors()
            val unreadCount = notifications.count { !it.isRead }
            for (i in notifications.indices) {
                if (!notifications[i].isRead) {
                    notifications[i] = notifications[i].copy(isRead = true, readAt = "2026-10-07T11:00:00Z")
                }
            }
            return MarkAllReadResponseDto(salonId = salonId, markedCount = unreadCount)
        }

        private fun checkErrors() {
            if (shouldThrow401) {
                val errorResponse = Response.error<Any>(
                    401,
                    """{"status":401,"error":"UNAUTHORIZED","message":"Token expired"}""".toResponseBody("application/json".toMediaType()),
                )
                throw HttpException(errorResponse)
            }
            if (shouldThrowNetwork) {
                throw IOException("Network connection lost")
            }
        }
    }

    @Test
    fun getNotifications_mapsDtoToDomainCorrectly() = runBlocking {
        val fakeApi = FakeManagerNotificationApi()
        val repository = ManagerNotificationRepositoryImpl(fakeApi)

        val result = repository.getNotifications("salon-1")
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(2, list.size)

        val first = list[0]
        assertEquals("notif-1", first.id)
        assertEquals("salon-1", first.salonId)
        assertEquals(NotificationType.BOOKING_CREATED, first.type)
        assertEquals(NotificationSeverity.INFO, first.severity)
        assertFalse(first.isRead)

        val second = list[1]
        assertEquals("notif-2", second.id)
        assertEquals(NotificationType.BOOKING_CANCELLED, second.type)
        assertEquals(NotificationSeverity.WARNING, second.severity)
        assertTrue(second.isRead)
    }

    @Test
    fun getUnreadCount_returnsCountFromApi() = runBlocking {
        val fakeApi = FakeManagerNotificationApi()
        val repository = ManagerNotificationRepositoryImpl(fakeApi)

        val result = repository.getUnreadCount("salon-1")
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrThrow())
    }

    @Test
    fun markAsRead_updatesNotificationAndReturnsUpdatedDomain() = runBlocking {
        val fakeApi = FakeManagerNotificationApi()
        val repository = ManagerNotificationRepositoryImpl(fakeApi)

        val result = repository.markAsRead("salon-1", "notif-1")
        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertTrue(updated.isRead)
        assertEquals("notif-1", updated.id)
    }

    @Test
    fun markAllAsRead_marksAllAndReturnsSuccess() = runBlocking {
        val fakeApi = FakeManagerNotificationApi()
        val repository = ManagerNotificationRepositoryImpl(fakeApi)

        val result = repository.markAllAsRead("salon-1")
        assertTrue(result.isSuccess)

        val unreadResult = repository.getUnreadCount("salon-1")
        assertEquals(0L, unreadResult.getOrThrow())
    }

    @Test
    fun http401_mapsToBackendApiException() = runBlocking {
        val fakeApi = FakeManagerNotificationApi().apply { shouldThrow401 = true }
        val repository = ManagerNotificationRepositoryImpl(fakeApi)

        val result = repository.getNotifications("salon-1")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is BackendApiException)
        assertEquals(401, (ex as BackendApiException).statusCode)
    }

    @Test
    fun networkFailure_mapsToNetworkUnavailableException() = runBlocking {
        val fakeApi = FakeManagerNotificationApi().apply { shouldThrowNetwork = true }
        val repository = ManagerNotificationRepositoryImpl(fakeApi)

        val result = repository.getNotifications("salon-1")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is NetworkUnavailableException)
    }
}
