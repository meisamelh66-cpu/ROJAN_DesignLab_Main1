package ai.rojan.designlab.domain.repository

/**
 * A platform-managed promotional banner shown on the Manager dashboard —
 * static, admin-uploaded artwork (`ROJAN_Backend`'s `BannerTarget.MANAGER`).
 * Only [imageUrl] is modeled: the Manager Banner is not a navigation
 * mechanism — any text/link the artwork itself carries is part of the
 * image, never rendered or handled separately by this app.
 */
data class Banner(
    val imageUrl: String,
)

/**
 * Talks to the ROJAN backend's unauthenticated public banner surface
 * (`ROJAN_Backend/PublicBannerController`). Data layer only — no screen
 * consumed this before `ManagerBannerSlot`.
 */
interface BannerRepository {
    /** The active Manager banner, if one exists — `null` means genuinely none configured, not an error. */
    suspend fun getActiveManagerBanner(): Result<Banner?>
}
