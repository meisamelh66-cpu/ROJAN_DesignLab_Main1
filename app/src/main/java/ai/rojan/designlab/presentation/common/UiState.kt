package ai.rojan.designlab.presentation.common

/**
 * Shared loading/success/empty/error shape for every backend-networked
 * ViewModel introduced by the Android <-> Backend Full Integration
 * milestone. No such convention existed before this — every previous
 * ViewModel in this codebase (`BookingViewModel`, `CustomerEcosystemViewModel`)
 * wraps synchronous, always-available demo data, so there was nothing to
 * represent "still loading" or "failed to load" for. Introduced once here
 * and reused by every new ViewModel rather than each inventing its own.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}
