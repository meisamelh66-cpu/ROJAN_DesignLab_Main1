package ai.rojan.designlab.manager.presentation.staff

import ai.rojan.designlab.manager.domain.membership.SalonMember
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.membership.StaffCandidate
import ai.rojan.designlab.manager.domain.repository.SalonMembershipRepository
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Staff Access Phase 1 - the real existing-staff list, same Loading/Loaded/Error shape every other Manager list screen in this codebase already uses. */
sealed interface StaffAccessUiState {
    data object Loading : StaffAccessUiState
    data class Loaded(val members: List<SalonMember>) : StaffAccessUiState
    data class Error(val message: String) : StaffAccessUiState
}

/**
 * Staff Access Phase 1 - the "Add Employee" sub-flow: phone entry -> lookup -> confirm identity
 * -> pick a role -> assign (which also activates - see [ai.rojan.backend.domain.salon.SalonMembership]'s
 * backend contract). Deliberately a separate state machine from [StaffAccessUiState] - the member
 * list underneath stays visible/unaffected while this sub-flow is open.
 */
sealed interface AddStaffUiState {
    data object Hidden : AddStaffUiState
    data class EnteringPhone(val phone: String = "", val error: String? = null) : AddStaffUiState
    data object Looking : AddStaffUiState
    data class Confirming(val candidate: StaffCandidate, val selectedRole: SalonMemberRole = SalonMemberRole.RECEPTIONIST) : AddStaffUiState
    data object Assigning : AddStaffUiState
    data class Error(val message: String) : AddStaffUiState
}

/**
 * Owner-only in effect (the backend's own `Permission.MANAGE_MEMBERSHIP` check is the real
 * enforcement); this screen is only ever reached when
 * [ai.rojan.designlab.domain.repository.SalonPermissions.MANAGE_MEMBERSHIP] is present in the
 * active salon's resolved permission set (gated in `ManagerNavGraph`, same convention
 * [ManagerInviteReceptionViewModel] already uses). A caller who somehow reaches this screen
 * without that permission still gets a real 403 from the backend for every action, surfaced
 * through the same [userMessageFor] path as any other failure.
 */
class ManagerStaffAccessViewModel(
    private val salonId: String?,
    private val repository: SalonMembershipRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<StaffAccessUiState>(StaffAccessUiState.Loading)
    val state: StateFlow<StaffAccessUiState> = _state.asStateFlow()

    private val _addState = MutableStateFlow<AddStaffUiState>(AddStaffUiState.Hidden)
    val addState: StateFlow<AddStaffUiState> = _addState.asStateFlow()

    init {
        loadMembers()
    }

    fun loadMembers() {
        val id = salonId
        if (id == null) {
            _state.value = StaffAccessUiState.Error("سالن فعال یافت نشد")
            return
        }
        _state.value = StaffAccessUiState.Loading
        viewModelScope.launch {
            repository.listMembers(id)
                .onSuccess { members -> _state.value = StaffAccessUiState.Loaded(members) }
                .onFailure { error -> _state.value = StaffAccessUiState.Error(userMessageFor(error)) }
        }
    }

    fun startAddStaff() {
        _addState.value = AddStaffUiState.EnteringPhone()
    }

    fun cancelAddStaff() {
        _addState.value = AddStaffUiState.Hidden
    }

    fun onPhoneInputChange(value: String) {
        val current = _addState.value as? AddStaffUiState.EnteringPhone ?: return
        _addState.value = current.copy(phone = value, error = null)
    }

    fun lookupPhone() {
        val id = salonId ?: return
        val current = _addState.value as? AddStaffUiState.EnteringPhone ?: return
        val normalized = normalizeIranianPhone(current.phone)
        if (normalized == null) {
            _addState.value = current.copy(error = "شماره موبایل نامعتبر است")
            return
        }
        _addState.value = AddStaffUiState.Looking
        viewModelScope.launch {
            repository.lookupByPhone(id, normalized)
                .onSuccess { candidate -> _addState.value = AddStaffUiState.Confirming(candidate) }
                .onFailure { error -> _addState.value = AddStaffUiState.Error(userMessageFor(error)) }
        }
    }

    fun onRoleSelected(role: SalonMemberRole) {
        val current = _addState.value as? AddStaffUiState.Confirming ?: return
        _addState.value = current.copy(selectedRole = role)
    }

    fun confirmAssign() {
        val id = salonId ?: return
        val current = _addState.value as? AddStaffUiState.Confirming ?: return
        _addState.value = AddStaffUiState.Assigning
        viewModelScope.launch {
            repository.assignRole(id, current.candidate.userId, current.selectedRole)
                .onSuccess {
                    _addState.value = AddStaffUiState.Hidden
                    loadMembers()
                }
                .onFailure { error -> _addState.value = AddStaffUiState.Error(userMessageFor(error)) }
        }
    }

    fun retryAdd() {
        _addState.value = AddStaffUiState.EnteringPhone()
    }

    fun setMemberActive(userId: String, active: Boolean) {
        val id = salonId ?: return
        viewModelScope.launch {
            repository.setActive(id, userId, active)
                .onSuccess { loadMembers() }
                .onFailure { error -> _state.value = StaffAccessUiState.Error(userMessageFor(error)) }
        }
    }

    fun changeRole(userId: String, role: SalonMemberRole) {
        val id = salonId ?: return
        viewModelScope.launch {
            repository.assignRole(id, userId, role)
                .onSuccess { loadMembers() }
                .onFailure { error -> _state.value = StaffAccessUiState.Error(userMessageFor(error)) }
        }
    }
}

/**
 * Client-side convenience only - the backend's `PhoneNumber` E.164 validation is the real
 * authority (a caller bypassing this still gets a real 400 from the backend). Accepts the common
 * local `09...` shape and normalizes it to `+989...`; anything already E.164 passes through
 * unchanged; anything else is rejected before a network call is even made.
 */
internal fun normalizeIranianPhone(raw: String): String? {
    val trimmed = raw.trim()
    val candidate = when {
        trimmed.startsWith("+") -> trimmed
        trimmed.startsWith("0") -> "+98${trimmed.substring(1)}"
        trimmed.startsWith("98") -> "+$trimmed"
        else -> return null
    }
    return if (Regex("^\\+[1-9]\\d{7,14}$").matches(candidate)) candidate else null
}
