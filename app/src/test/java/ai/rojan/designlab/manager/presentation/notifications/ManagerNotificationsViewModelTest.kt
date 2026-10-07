package ai.rojan.designlab.manager.presentation.notifications

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import ai.rojan.designlab.domain.repository.ManagerNotification
import ai.rojan.designlab.domain.repository.ManagerNotificationRepository
import ai.rojan.designlab.domain.repository.NotificationSeverity
import ai.rojan.designlab.domain.repository.NotificationType
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ManagerNotificationsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeActiveSalonContextRepository(
        initialSalonId: String? = "salon-1",
    ) : ActiveSalonContextRepository {
        val flow = MutableStateFlow(initialSalonId)

        override suspend fun saveActiveSalonId(salonId: String) {
            flow.value = salonId
        }

        override suspend fun clearActiveSalonId() {
            flow.value = null
        }

        override fun observeActiveSalonId(): Flow<String?> = flow
    }

    private class FakeManagerNotificationRepository : ManagerNotificationRepository {
        var shouldThrow401 = false
        var shouldThrowGenericError = false

        val notificationsBySalon = mutableMapOf<String, MutableList<ManagerNotification>>()

        fun setNotifications(salonId: String, list: List<ManagerNotification>) {
            notificationsBySalon[salonId] = list.toMutableList()
        }

        override suspend fun getNotifications(
            salonId: String,
            page: Int,
            size: Int,
            unreadOnly: Boolean,
            category: String?,
        ): Result<List<ManagerNotification>> {
            if (shouldThrow401) {
                return Result.failure(BackendApiException(401, null))
            }
            if (shouldThrowGenericError) {
                return Result.failure(IOException("Server error"))
            }
            val list = notificationsBySalon[salonId] ?: mutableListOf()
            val filtered = if (unreadOnly) list.filter { !it.isRead } else list
            return Result.success(filtered.toList())
        }

        override suspend fun getUnreadCount(salonId: String): Result<Long> {
            if (shouldThrow401) {
                return Result.failure(BackendApiException(401, null))
            }
            if (shouldThrowGenericError) {
                return Result.failure(IOException("Server error"))
            }
            val list = notificationsBySalon[salonId] ?: mutableListOf()
            return Result.success(list.count { !it.isRead }.toLong())
        }

        override suspend fun markAsRead(
            salonId: String,
            notificationId: String,
        ): Result<ManagerNotification> {
            if (shouldThrow401) {
                return Result.failure(BackendApiException(401, null))
            }
            val list = notificationsBySalon[salonId] ?: return Result.failure(NoSuchElementException())
            val idx = list.indexOfFirst { it.id == notificationId }
            if (idx == -1) return Result.failure(NoSuchElementException())
            val updated = list[idx].copy(isRead = true)
            list[idx] = updated
            return Result.success(updated)
        }

        override suspend fun markAllAsRead(salonId: String): Result<Unit> {
            if (shouldThrow401) {
                return Result.failure(BackendApiException(401, null))
            }
            val list = notificationsBySalon[salonId] ?: return Result.success(Unit)
            for (i in list.indices) {
                list[i] = list[i].copy(isRead = true)
            }
            return Result.success(Unit)
        }
    }

    private fun sampleNotification(
        id: String,
        salonId: String = "salon-1",
        isRead: Boolean = false,
    ) = ManagerNotification(
        id = id,
        salonId = salonId,
        userId = null,
        type = NotificationType.BOOKING_CREATED,
        title = "نوبت جدید $id",
        message = "پیام $id",
        severity = NotificationSeverity.INFO,
        category = "bookings",
        referenceId = "ref-$id",
        referenceType = "BOOKING",
        isRead = isRead,
        readAt = if (isRead) "2026-10-07T10:00:00Z" else null,
        createdAt = "2026-10-07T09:00:00Z",
    )

    @Test
    fun loadingToSuccess_loadsNotificationsForActiveSalon() {
        val salonRepo = FakeActiveSalonContextRepository("salon-1")
        val notifRepo = FakeManagerNotificationRepository().apply {
            setNotifications(
                "salon-1",
                listOf(
                    sampleNotification("notif-1", "salon-1", isRead = false),
                    sampleNotification("notif-2", "salon-1", isRead = true),
                ),
            )
        }

        val viewModel = ManagerNotificationsViewModel(salonRepo, notifRepo)

        assertTrue(viewModel.state is UiState.Success)
        val data = (viewModel.state as UiState.Success).data
        assertEquals("salon-1", data.salonId)
        assertEquals(2, data.notifications.size)
        assertEquals(1L, data.unreadCount)
    }

    @Test
    fun emptyState_whenActiveSalonHasNoNotifications() {
        val salonRepo = FakeActiveSalonContextRepository("salon-1")
        val notifRepo = FakeManagerNotificationRepository().apply {
            setNotifications("salon-1", emptyList())
        }

        val viewModel = ManagerNotificationsViewModel(salonRepo, notifRepo)

        assertTrue(viewModel.state is UiState.Empty)
    }

    @Test
    fun emptyState_whenNoActiveSalonSelected() {
        val salonRepo = FakeActiveSalonContextRepository(null)
        val notifRepo = FakeManagerNotificationRepository()

        val viewModel = ManagerNotificationsViewModel(salonRepo, notifRepo)

        assertTrue(viewModel.state is UiState.Empty)
    }

    @Test
    fun activeSalonSwitching_reloadsNotificationsForNewSalon() {
        val salonRepo = FakeActiveSalonContextRepository("salon-1")
        val notifRepo = FakeManagerNotificationRepository().apply {
            setNotifications("salon-1", listOf(sampleNotification("notif-1", "salon-1")))
            setNotifications("salon-2", listOf(sampleNotification("notif-2", "salon-2")))
        }

        val viewModel = ManagerNotificationsViewModel(salonRepo, notifRepo)
        assertEquals("salon-1", (viewModel.state as UiState.Success).data.salonId)

        // Switch to salon-2
        salonRepo.flow.value = "salon-2"
        assertEquals("salon-2", (viewModel.state as UiState.Success).data.salonId)
        assertEquals("notif-2", (viewModel.state as UiState.Success).data.notifications[0].id)
    }

    @Test
    fun markAsRead_updatesItemAndDecrementsUnreadCountSafely() {
        val salonRepo = FakeActiveSalonContextRepository("salon-1")
        val notifRepo = FakeManagerNotificationRepository().apply {
            setNotifications(
                "salon-1",
                listOf(
                    sampleNotification("notif-1", "salon-1", isRead = false),
                    sampleNotification("notif-2", "salon-1", isRead = false),
                ),
            )
        }

        val viewModel = ManagerNotificationsViewModel(salonRepo, notifRepo)
        val initialData = (viewModel.state as UiState.Success).data
        assertEquals(2L, initialData.unreadCount)

        viewModel.markAsRead("notif-1")

        val updatedData = (viewModel.state as UiState.Success).data
        assertTrue(updatedData.notifications.find { it.id == "notif-1" }!!.isRead)
        assertFalse(updatedData.notifications.find { it.id == "notif-2" }!!.isRead)
        assertEquals(1L, updatedData.unreadCount)
    }

    @Test
    fun markAllAsRead_marksAllItemsReadAndClearsUnreadCount() {
        val salonRepo = FakeActiveSalonContextRepository("salon-1")
        val notifRepo = FakeManagerNotificationRepository().apply {
            setNotifications(
                "salon-1",
                listOf(
                    sampleNotification("notif-1", "salon-1", isRead = false),
                    sampleNotification("notif-2", "salon-1", isRead = false),
                ),
            )
        }

        val viewModel = ManagerNotificationsViewModel(salonRepo, notifRepo)
        assertEquals(2L, (viewModel.state as UiState.Success).data.unreadCount)

        viewModel.markAllAsRead()

        val updatedData = (viewModel.state as UiState.Success).data
        assertEquals(0L, updatedData.unreadCount)
        assertTrue(updatedData.notifications.all { it.isRead })
    }

    @Test
    fun http401_triggersRequiresReauth() {
        val salonRepo = FakeActiveSalonContextRepository("salon-1")
        val notifRepo = FakeManagerNotificationRepository().apply {
            shouldThrow401 = true
        }

        val viewModel = ManagerNotificationsViewModel(salonRepo, notifRepo)

        assertTrue(viewModel.requiresReauth)
    }

    @Test
    fun genericBackendError_setsErrorState() {
        val salonRepo = FakeActiveSalonContextRepository("salon-1")
        val notifRepo = FakeManagerNotificationRepository().apply {
            shouldThrowGenericError = true
        }

        val viewModel = ManagerNotificationsViewModel(salonRepo, notifRepo)

        assertTrue(viewModel.state is UiState.Error)
        assertFalse(viewModel.requiresReauth)
    }
}
