package ai.rojan.designlab.domain.repository

/** One real, backend-hosted salon photo — id lets the UI key a grid/pager stably, url is a resolved, ready-to-render CDN address (never a placeholder). */
data class SalonGalleryImage(
    val id: String,
    val url: String,
)

/**
 * Salon Gallery: authenticated view of a salon's `GALLERY`-type media
 * (`ROJAN_Backend/api/.../media/MediaController.kt`, `GET /api/v1/salons/{salonId}/media?mediaType=GALLERY`
 * — the same Media Foundation contract [ai.rojan.designlab.data.remote.ManagerMediaApi]
 * already wraps for Manager, GET is not owner-restricted). Guest equivalent
 * is [PublicSalonRepository.getGallery] (slug-based, `/api/v1/public/salons/{slug}/gallery`).
 */
interface SalonGalleryRepository {
    suspend fun getGallery(salonId: String): Result<List<SalonGalleryImage>>
}
