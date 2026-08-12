package ai.rojan.designlab.manager.domain.auth

import ai.rojan.designlab.domain.repository.ActiveSalonContext
import ai.rojan.designlab.domain.repository.AvailableSalon

/**
 * Active Salon Context & Selection Flow - the Manager App's resolution
 * state for "which salon is this session operating on," computed by
 * [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel] every
 * time its identity context refreshes (fresh login or cold-start restore).
 * Deliberately separate from [ManagerAuthState] - that gate only cares
 * whether a session exists; this cares which salon it applies to, a
 * question with no answer at all until a real [ActiveSalonContext] is
 * resolved here.
 */
sealed interface ActiveSalonUiState {

    /** Resolution in progress - the identity context hasn't been fetched (or re-fetched) yet. */
    data object Loading : ActiveSalonUiState

    /** More than one salon is available and none was already validly selected - the picker screen must decide, never this state itself. */
    data class SelectionRequired(val options: List<AvailableSalon>) : ActiveSalonUiState

    /** A real salon is resolved - either the only one available, or explicitly chosen. */
    data class Active(val context: ActiveSalonContext) : ActiveSalonUiState

    /** No available salon at all for this account - a real, honest outcome (e.g. a newly-approved manager account), not a bug. */
    data class Error(val message: String) : ActiveSalonUiState
}
