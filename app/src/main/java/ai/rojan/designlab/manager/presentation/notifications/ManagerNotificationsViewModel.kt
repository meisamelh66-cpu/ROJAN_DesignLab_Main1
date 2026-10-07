package ai.rojan.designlab.manager.presentation.notifications

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import ai.rojan.designlab.domain.repository.ManagerNotification
import ai.rojan.designlab.domain.repository.ManagerNotificationRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class ManagerNotificationsData(
    val salonId: String,
    val notifications: List<ManagerNotification>,
    val unreadCount: Long,
)

class ManagerNotificationsViewModel(
    private val activeSalonContextRepository: ActiveSalonContextRepository,
    private val notificationRepository: ManagerNotificationRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<ManagerNotificationsData>>(UiState.Loading)
        private set

    var requiresReauth by mutableStateOf(false)
        private set

    var isActionInProgress by mutableStateOf(false)
        private set

    private var currentSalonId: String? = null

    init {
        viewModelScope.launch {
            activeSalonContextRepository.observeActiveSalonId()
                .collectLatest { salonId ->
                    currentSalonId = salonId
                    if (salonId.isNullOrBlank()) {
                        state = UiState.Empty
                    } else {
                        state = UiState.Loading
                        requiresReauth = false
                        loadForSalon(salonId)
                    }
                }
        }
    }

    fun load() {
        val salonId = currentSalonId
        if (salonId.isNullOrBlank()) {
            viewModelScope.launch {
                val id = activeSalonContextRepository.observeActiveSalonId().firstOrNull()
                currentSalonId = id
                if (id.isNullOrBlank()) {
                    state = UiState.Empty
                } else {
                    state = UiState.Loading
                    requiresReauth = false
                    loadForSalon(id)
                }
            }
        } else {
            state = UiState.Loading
            requiresReauth = false
            viewModelScope.launch {
                loadForSalon(salonId)
            }
        }
    }

    fun retry() = load()

    private suspend fun loadForSalon(salonId: String) {
        val notificationsResult = notificationRepository.getNotifications(salonId = salonId)
        val unreadCountResult = notificationRepository.getUnreadCount(salonId = salonId)

        if (notificationsResult.isFailure) {
            handleFailure(notificationsResult.exceptionOrNull() ?: Exception("Unknown error"))
            return
        }

        val notifications = notificationsResult.getOrThrow()
        val unreadCount = unreadCountResult.getOrDefault(notifications.count { !it.isRead }.toLong())

        state = if (notifications.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Success(
                ManagerNotificationsData(
                    salonId = salonId,
                    notifications = notifications,
                    unreadCount = unreadCount,
                )
            )
        }
    }

    fun markAsRead(notificationId: String) {
        val currentState = state as? UiState.Success ?: return
        val salonId = currentState.data.salonId
        val target = currentState.data.notifications.find { it.id == notificationId } ?: return
        if (target.isRead) return

        viewModelScope.launch {
            val result = notificationRepository.markAsRead(salonId = salonId, notificationId = notificationId)
            if (result.isSuccess) {
                val updatedList = currentState.data.notifications.map { notif ->
                    if (notif.id == notificationId) notif.copy(isRead = true) else notif
                }
                val newUnread = maxOf(0L, currentState.data.unreadCount - 1)
                state = UiState.Success(
                    currentState.data.copy(
                        notifications = updatedList,
                        unreadCount = newUnread,
                    )
                )
            } else {
                val error = result.exceptionOrNull()
                if (error is BackendApiException && error.statusCode == 401) {
                    requiresReauth = true
                }
            }
        }
    }

    fun markAllAsRead() {
        val currentState = state as? UiState.Success ?: return
        val salonId = currentState.data.salonId
        if (currentState.data.unreadCount == 0L && currentState.data.notifications.none { !it.isRead }) return

        viewModelScope.launch {
            isActionInProgress = true
            val result = notificationRepository.markAllAsRead(salonId = salonId)
            isActionInProgress = false
            if (result.isSuccess) {
                val updatedList = currentState.data.notifications.map { it.copy(isRead = true) }
                state = UiState.Success(
                    currentState.data.copy(
                        notifications = updatedList,
                        unreadCount = 0L,
                    )
                )
            } else {
                val error = result.exceptionOrNull()
                if (error is BackendApiException && error.statusCode == 401) {
                    requiresReauth = true
                }
            }
        }
    }

    private fun handleFailure(error: Throwable) {
        if (error is BackendApiException && error.statusCode == 401) {
            requiresReauth = true
        } else {
            state = UiState.Error(userMessageFor(error))
        }
    }
}

class ManagerNotificationsViewModelFactory(
    private val activeSalonContextRepository: ActiveSalonContextRepository,
    private val notificationRepository: ManagerNotificationRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerNotificationsViewModel(
            activeSalonContextRepository = activeSalonContextRepository,
            notificationRepository = notificationRepository,
        ) as T
    }
}
