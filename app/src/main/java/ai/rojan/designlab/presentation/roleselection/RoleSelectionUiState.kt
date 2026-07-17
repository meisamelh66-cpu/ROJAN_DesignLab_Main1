package ai.rojan.designlab.presentation.roleselection

/** UI state for the act of selecting and persisting a role on the Welcome screen. */
sealed interface RoleSelectionUiState {

    /** Nothing in flight; role cards / the Hero Card CTA are tappable. */
    data object Idle : RoleSelectionUiState

    /** A role is currently being persisted; shown as a transient loading indicator. */
    data object Saving : RoleSelectionUiState

    /** Persisting the last-tapped role failed; shown as a transient, retryable error banner. */
    data object Error : RoleSelectionUiState
}
