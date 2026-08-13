package ai.rojan.designlab.reception.domain.auth

import ai.rojan.designlab.domain.repository.ActiveSalonContext
import ai.rojan.designlab.domain.repository.AvailableSalon

/**
 * The Reception App's resolution state for "which salon is this session
 * operating on" — same shape and same reasoning as
 * [ai.rojan.designlab.manager.domain.auth.ActiveSalonUiState], kept as its
 * own type in the `reception` package rather than imported from `manager`
 * so the two flavors stay independent (see [ReceptionAuthState]'s own doc
 * comment).
 *
 * See ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 */
sealed interface ActiveSalonUiState {

    /** Resolution in progress. */
    data object Loading : ActiveSalonUiState

    /** More than one salon is available and none was already validly selected. */
    data class SelectionRequired(val options: List<AvailableSalon>) : ActiveSalonUiState

    /** A real salon is resolved. */
    data class Active(val context: ActiveSalonContext) : ActiveSalonUiState

    /** No available salon at all for this account. */
    data class Error(val message: String) : ActiveSalonUiState
}
