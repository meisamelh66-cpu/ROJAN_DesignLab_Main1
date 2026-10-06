package ai.rojan.designlab.domain.repository

/**
 * A platform-managed promotional banner (`ROJAN_Backend` Banner Management,
 * Super Admin) — real content, never fabricated client-side. Deliberately
 * exposes only [imageUrl]: every consumer of this domain type renders a
 * banner as a static image only. `href`/title/subtitle/displayOrder exist
 * on the real backend DTO but are intentionally never surfaced past
 * [BannerRepository] — see
 * [ai.rojan.designlab.manager.components.ManagerBannerSlot]'s own doc
 * comment for why the Manager app specifically must never make this
 * clickable.
 */
data class Banner(val imageUrl: String)

/**
 * Unauthenticated read access to `GET /api/v1/public/banners?target=`
 * (`ROJAN_Backend` `PublicBannerController`) — the same real, pre-existing
 * public endpoint every other banner target already uses.
 */
interface BannerRepository {
    suspend fun getActiveBanners(target: String): Result<List<Banner>>
}
