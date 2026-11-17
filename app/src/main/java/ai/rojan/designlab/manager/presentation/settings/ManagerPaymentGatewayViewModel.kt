package ai.rojan.designlab.manager.presentation.settings

import ai.rojan.designlab.manager.domain.repository.ManagerPaymentGatewayRepository
import ai.rojan.designlab.manager.domain.repository.PaymentGatewayConfig
import ai.rojan.designlab.manager.domain.repository.PaymentGatewayTestResult
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * P1 Per-Salon Payment Gateway Configuration — one salon's own Zarinpal credential, as the Manager
 * screen edits it. [configured]/[isActive]/[maskedMerchantId] mirror the backend's own response
 * shape exactly (see [PaymentGatewayConfig]'s own doc comment) — [merchantIdInput] is purely local
 * draft-input state for the not-yet-saved textfield, never pre-filled from [maskedMerchantId] (the
 * real plaintext is never sent back by the backend to pre-fill with — see that field's own doc
 * comment on why).
 *
 * A caller without [ai.rojan.backend.domain.salon.Permission.MANAGE_PAYMENT_GATEWAY] at this salon
 * (granted to the owner and to a [ai.rojan.backend.domain.salon.SalonRole.MANAGER] member of *this*
 * salon only — never another salon) gets a 403 from every one of these calls — surfaced through the
 * same generic [UiState.Error]/[userMessageFor] path as any other failure, same as
 * [ManagerWorkingHoursViewModel] already does for its own 403s; no separate "Forbidden" branch is
 * needed since this screen is reached only from Settings, which the owner alone can already open.
 */
data class PaymentGatewaySettingsState(
    val configured: Boolean,
    val isActive: Boolean,
    val maskedMerchantId: String?,
    val merchantIdInput: String = "",
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val isTogglingActive: Boolean = false,
    val testResult: PaymentGatewayTestResult? = null,
    val error: String? = null,
)

/**
 * ViewModel+Factory, same shape [ManagerWorkingHoursViewModel] already establishes for this
 * Settings area. [salonId] is resolved once by the factory from
 * [ai.rojan.designlab.manager.data.ManagerRepositories.salonId], same precondition every sibling
 * Settings ViewModel already relies on for the same field.
 */
class ManagerPaymentGatewayViewModel(
    private val salonId: String?,
    private val repository: ManagerPaymentGatewayRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<PaymentGatewaySettingsState>>(UiState.Loading)
    val state: StateFlow<UiState<PaymentGatewaySettingsState>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val id = salonId
        if (id == null) {
            _state.value = UiState.Error("سالن فعال یافت نشد")
            return
        }
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.getConfig(id)
                .onSuccess { config -> _state.value = UiState.Success(config.toFormState()) }
                .onFailure { _state.value = UiState.Error(userMessageFor(it)) }
        }
    }

    fun onMerchantIdInputChange(value: String) = updateForm { it.copy(merchantIdInput = value, error = null) }

    /** `PUT` — saves (creates or replaces) the credential. Never touches [PaymentGatewaySettingsState.isActive] locally; the response's own, server-authoritative [PaymentGatewayConfig.isActive] is what's actually applied (see that use case's own "replacing a credential never silently (re)activates" guarantee). */
    fun save() {
        val id = salonId ?: return
        val form = currentForm() ?: return
        val merchantId = form.merchantIdInput.trim()
        if (merchantId.isBlank()) return

        updateForm { it.copy(isSaving = true, error = null, testResult = null) }
        viewModelScope.launch {
            repository.save(id, PROVIDER_ZARINPAL, merchantId)
                .onSuccess { config -> _state.value = UiState.Success(config.toFormState()) }
                .onFailure { error -> updateForm { it.copy(isSaving = false, error = userMessageFor(error)) } }
        }
    }

    fun activate() {
        val id = salonId ?: return
        updateForm { it.copy(isTogglingActive = true, error = null) }
        viewModelScope.launch {
            repository.activate(id)
                .onSuccess { config -> _state.value = UiState.Success(config.toFormState()) }
                .onFailure { error -> updateForm { it.copy(isTogglingActive = false, error = userMessageFor(error)) } }
        }
    }

    fun deactivate() {
        val id = salonId ?: return
        updateForm { it.copy(isTogglingActive = true, error = null) }
        viewModelScope.launch {
            repository.deactivate(id)
                .onSuccess { config -> _state.value = UiState.Success(config.toFormState()) }
                .onFailure { error -> updateForm { it.copy(isTogglingActive = false, error = userMessageFor(error)) } }
        }
    }

    fun testConnection() {
        val id = salonId ?: return
        updateForm { it.copy(isTesting = true, error = null, testResult = null) }
        viewModelScope.launch {
            repository.testConnection(id)
                .onSuccess { result -> updateForm { it.copy(isTesting = false, testResult = result) } }
                .onFailure { error -> updateForm { it.copy(isTesting = false, error = userMessageFor(error)) } }
        }
    }

    private fun currentForm(): PaymentGatewaySettingsState? = (_state.value as? UiState.Success)?.data

    private fun updateForm(transform: (PaymentGatewaySettingsState) -> PaymentGatewaySettingsState) {
        val form = currentForm() ?: return
        _state.value = UiState.Success(transform(form))
    }

    /** Preserves whatever draft [PaymentGatewaySettingsState.merchantIdInput] the owner was mid-typing across a refresh — only the server-authoritative fields are replaced. */
    private fun PaymentGatewayConfig.toFormState(): PaymentGatewaySettingsState {
        val previousInput = currentForm()?.merchantIdInput.orEmpty()
        return PaymentGatewaySettingsState(
            configured = configured,
            isActive = isActive,
            maskedMerchantId = maskedMerchantId,
            merchantIdInput = previousInput,
        )
    }

    companion object {
        const val PROVIDER_ZARINPAL = "ZARINPAL"
    }
}
