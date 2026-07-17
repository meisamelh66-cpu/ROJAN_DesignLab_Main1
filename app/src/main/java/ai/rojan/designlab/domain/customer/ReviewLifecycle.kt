package ai.rojan.designlab.domain.customer

/**
 * The 4-stage review lifecycle: Pending Review → Review Request →
 * Submit → Publish. Every transition between these is enforced
 * exclusively inside [CustomerEcosystemEngine] (never in a screen or
 * the ViewModel directly) — per the explicit "every state transition
 * must remain isolated inside Engine classes" requirement.
 */
enum class ReviewLifecycleStatus {
    PENDING_REQUEST,
    REQUESTED,
    SUBMITTED,
    PUBLISHED,
}

data class PendingReview(
    val appointmentId: String,
    val status: ReviewLifecycleStatus,
)
