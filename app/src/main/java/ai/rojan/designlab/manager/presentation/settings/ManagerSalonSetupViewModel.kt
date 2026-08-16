package ai.rojan.designlab.manager.presentation.settings

import ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary
import ai.rojan.designlab.manager.domain.repository.ManagerSalonRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The backend-ready Salon Identity fields. `city`/logo/cover stay
 * excluded — `city` has no backend field at all, and logo/cover have a
 * backend field but no upload endpoint to produce a real URL yet.
 * [latitude]/[longitude] (Phase A Correction) are real, writable fields
 * kept as raw text here (not `Double`) since they're user-edited input
 * that may be blank or momentarily invalid while typing — [save] parses
 * and range-validates them before submission.
 */
data class SalonSetupFormState(
    val name: String = "",
    val description: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val latitude: String = "",
    val longitude: String = "",
)

/**
 * Owns loading + form + submission state for
 * [ai.rojan.designlab.manager.screens.settings.ManagerSalonSetupScreen] —
 * proper ViewModel+Factory, deliberately not read from the
 * [ai.rojan.designlab.manager.data.ManagerRepositories] global singleton
 * the way 13 other Manager screens currently do (`ROJAN_QA_Remediation_Plan_v1.md`
 * §1.1, `ROJAN_First_Salon_Implementation_Roadmap_v1.md`'s own risk
 * section) — this screen is new, so it follows Customer/Reception's
 * established pattern instead of extending that debt.
 *
 * [loadState] uses the shared [UiState]: [UiState.Empty] means the owner
 * has no salon yet (create mode), [UiState.Success] means one was loaded
 * (edit mode, form pre-filled) — decided by a real `GET /salons/mine`
 * call, never guessed or defaulted.
 */
class ManagerSalonSetupViewModel(
    private val salonRepository: ManagerSalonRepository,
) : ViewModel() {

    private val _loadState = MutableStateFlow<UiState<ManagerSalonSummary?>>(UiState.Loading)
    val loadState: StateFlow<UiState<ManagerSalonSummary?>> = _loadState.asStateFlow()

    private val _formState = MutableStateFlow(SalonSetupFormState())
    val formState: StateFlow<SalonSetupFormState> = _formState.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError.asStateFlow()

    /** `null` until a real salon is loaded or created — the mode sentinel [save] switches on. */
    private var existingSalonId: String? = null

    init {
        load()
    }

    fun load() {
        _loadState.value = UiState.Loading
        viewModelScope.launch {
            salonRepository.getMySalon()
                .onSuccess { salon ->
                    if (salon == null) {
                        existingSalonId = null
                        _formState.value = SalonSetupFormState()
                        _loadState.value = UiState.Empty
                    } else {
                        existingSalonId = salon.id
                        _formState.value = SalonSetupFormState(
                            name = salon.name,
                            description = salon.description.orEmpty(),
                            phone = salon.phone,
                            email = salon.email.orEmpty(),
                            address = salon.address,
                            latitude = salon.latitude?.toString().orEmpty(),
                            longitude = salon.longitude?.toString().orEmpty(),
                        )
                        _loadState.value = UiState.Success(salon)
                    }
                }
                .onFailure { _loadState.value = UiState.Error(userMessageFor(it)) }
        }
    }

    fun onNameChange(value: String) {
        _formState.value = _formState.value.copy(name = value)
    }

    fun onDescriptionChange(value: String) {
        _formState.value = _formState.value.copy(description = value)
    }

    fun onPhoneChange(value: String) {
        _formState.value = _formState.value.copy(phone = value)
    }

    fun onEmailChange(value: String) {
        _formState.value = _formState.value.copy(email = value)
    }

    fun onAddressChange(value: String) {
        _formState.value = _formState.value.copy(address = value)
    }

    fun onLatitudeChange(value: String) {
        _formState.value = _formState.value.copy(latitude = value)
    }

    fun onLongitudeChange(value: String) {
        _formState.value = _formState.value.copy(longitude = value)
    }

    /**
     * Creates the owner's salon if none was loaded, otherwise updates the
     * existing one — [existingSalonId] is the real mode sentinel, resolved
     * from an actual backend read in [load], never assumed.
     *
     * [latitude]/[longitude] only ever reach [ManagerSalonRepository.updateSalon] -
     * the backend's `CreateSalonRequest` has no such fields, so a
     * brand-new salon's coordinates can only be set on a later edit, once
     * [existingSalonId] is real. A non-blank value that doesn't parse as a
     * valid coordinate blocks the whole submission (same fail-closed shape
     * as the existing blank-required-field guard below) rather than being
     * silently dropped.
     */
    fun save(onSaved: () -> Unit) {
        val form = _formState.value
        if (form.name.isBlank() || form.phone.isBlank() || form.address.isBlank()) return

        val latitude = form.latitude.trim().ifBlank { null }?.toDoubleOrNull()
        if (form.latitude.isNotBlank() && (latitude == null || latitude !in -90.0..90.0)) return
        val longitude = form.longitude.trim().ifBlank { null }?.toDoubleOrNull()
        if (form.longitude.isNotBlank() && (longitude == null || longitude !in -180.0..180.0)) return

        _submitError.value = null
        _isSubmitting.value = true
        viewModelScope.launch {
            val description = form.description.trim().ifBlank { null }
            val email = form.email.trim().ifBlank { null }

            val result = existingSalonId?.let { salonId ->
                salonRepository.updateSalon(
                    salonId = salonId,
                    name = form.name.trim(),
                    description = description,
                    phone = form.phone.trim(),
                    email = email,
                    address = form.address.trim(),
                    latitude = latitude,
                    longitude = longitude,
                )
            } ?: salonRepository.createSalon(
                name = form.name.trim(),
                description = description,
                phone = form.phone.trim(),
                email = email,
                address = form.address.trim(),
            )

            _isSubmitting.value = false
            result
                .onSuccess { salon ->
                    existingSalonId = salon.id
                    _loadState.value = UiState.Success(salon)
                    onSaved()
                }
                .onFailure { _submitError.value = userMessageFor(it) }
        }
    }
}
