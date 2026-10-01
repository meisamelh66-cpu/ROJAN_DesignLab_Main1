package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.PublicBannerApi
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.Banner
import ai.rojan.designlab.domain.repository.BannerRepository

class BannerRepositoryImpl(
    private val publicBannerApi: PublicBannerApi,
) : BannerRepository {

    override suspend fun getActiveManagerBanner(): Result<Banner?> =
        safeApiCall { publicBannerApi.getActiveBanners() }
            .map { banners -> banners.firstOrNull()?.let { Banner(imageUrl = it.imageUrl) } }
}
