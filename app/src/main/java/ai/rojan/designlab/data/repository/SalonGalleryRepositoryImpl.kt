package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.ManagerMediaApi
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.SalonGalleryImage
import ai.rojan.designlab.domain.repository.SalonGalleryRepository

class SalonGalleryRepositoryImpl(
    private val mediaApi: ManagerMediaApi,
) : SalonGalleryRepository {

    override suspend fun getGallery(salonId: String): Result<List<SalonGalleryImage>> =
        safeApiCall { mediaApi.list(salonId, mediaType = "GALLERY") }
            .map { list -> list.map { SalonGalleryImage(id = it.id, url = it.url) } }
}
