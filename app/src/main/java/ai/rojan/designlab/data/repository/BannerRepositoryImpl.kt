package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.PublicBannerApi
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.Banner
import ai.rojan.designlab.domain.repository.BannerRepository

class BannerRepositoryImpl(
    private val publicBannerApi: PublicBannerApi,
) : BannerRepository {

    override suspend fun getActiveBanners(target: String): Result<List<Banner>> =
        safeApiCall { publicBannerApi.getActiveBanners(target) }
            .map { list -> list.map { Banner(imageUrl = it.imageUrl) } }
}
