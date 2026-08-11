package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.SalonRelationshipApi
import ai.rojan.designlab.data.remote.dto.SalonFavoriteDto
import ai.rojan.designlab.data.remote.dto.SalonFollowDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.CustomerFavoriteSalon
import ai.rojan.designlab.domain.repository.CustomerFollowedSalon
import ai.rojan.designlab.domain.repository.CustomerRelationshipRepository
import ai.rojan.designlab.domain.repository.SalonFollowStatus

class CustomerRelationshipRepositoryImpl(
    private val api: SalonRelationshipApi,
) : CustomerRelationshipRepository {

    override suspend fun followSalon(salonId: String): Result<CustomerFollowedSalon> =
        safeApiCall { api.follow(salonId) }.map { it.toDomain() }

    override suspend fun unfollowSalon(salonId: String): Result<Unit> =
        safeApiCall { api.unfollow(salonId) }

    override suspend fun getFollowedSalons(page: Int, size: Int): Result<List<CustomerFollowedSalon>> =
        safeApiCall { api.getFollowedSalons(page, size) }.map { paged -> paged.content.map { it.toDomain() } }

    override suspend fun favoriteSalon(salonId: String): Result<CustomerFavoriteSalon> =
        safeApiCall { api.favorite(salonId) }.map { it.toDomain() }

    override suspend fun unfavoriteSalon(salonId: String): Result<Unit> =
        safeApiCall { api.unfavorite(salonId) }

    override suspend fun getFavoriteSalons(page: Int, size: Int): Result<List<CustomerFavoriteSalon>> =
        safeApiCall { api.getFavoriteSalons(page, size) }.map { paged -> paged.content.map { it.toDomain() } }

    private fun SalonFollowDto.toDomain() = CustomerFollowedSalon(
        id = id,
        salonId = salonId,
        status = runCatching { SalonFollowStatus.valueOf(status) }.getOrDefault(SalonFollowStatus.ACTIVE),
        createdAt = createdAt,
    )

    private fun SalonFavoriteDto.toDomain() = CustomerFavoriteSalon(
        id = id,
        salonId = salonId,
        createdAt = createdAt,
    )
}
