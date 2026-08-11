package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.SalonRelationshipApi
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.SalonFavoriteDto
import ai.rojan.designlab.data.remote.dto.SalonFollowDto
import ai.rojan.designlab.domain.repository.SalonFollowStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSalonRelationshipApi : SalonRelationshipApi {
    var followResponse: SalonFollowDto? = null
    var favoriteResponse: SalonFavoriteDto? = null
    var followedSalons: List<SalonFollowDto> = emptyList()
    var favoriteSalons: List<SalonFavoriteDto> = emptyList()
    var thrownOnUnfollow: Throwable? = null
    var thrownOnUnfavorite: Throwable? = null

    override suspend fun follow(salonId: String): SalonFollowDto = followResponse!!
    override suspend fun unfollow(salonId: String) {
        thrownOnUnfollow?.let { throw it }
    }

    override suspend fun getFollowedSalons(page: Int, size: Int): PagedResponseDto<SalonFollowDto> =
        PagedResponseDto(followedSalons, page, size, followedSalons.size.toLong(), 1)

    override suspend fun favorite(salonId: String): SalonFavoriteDto = favoriteResponse!!
    override suspend fun unfavorite(salonId: String) {
        thrownOnUnfavorite?.let { throw it }
    }

    override suspend fun getFavoriteSalons(page: Int, size: Int): PagedResponseDto<SalonFavoriteDto> =
        PagedResponseDto(favoriteSalons, page, size, favoriteSalons.size.toLong(), 1)
}

class CustomerRelationshipRepositoryImplTest {

    private val api = FakeSalonRelationshipApi()
    private val repository = CustomerRelationshipRepositoryImpl(api)

    @Test
    fun `followSalon maps the dto to the domain model`() = runTest {
        api.followResponse = SalonFollowDto(id = "f1", salonId = "s1", status = "ACTIVE", createdAt = "2026-01-01T00:00:00Z")

        val result = repository.followSalon("s1")

        assertTrue(result.isSuccess)
        val follow = result.getOrThrow()
        assertEquals("f1", follow.id)
        assertEquals("s1", follow.salonId)
        assertEquals(SalonFollowStatus.ACTIVE, follow.status)
    }

    @Test
    fun `favoriteSalon maps the dto to the domain model`() = runTest {
        api.favoriteResponse = SalonFavoriteDto(id = "fav1", salonId = "s1", createdAt = "2026-01-01T00:00:00Z")

        val result = repository.favoriteSalon("s1")

        assertTrue(result.isSuccess)
        val favorite = result.getOrThrow()
        assertEquals("fav1", favorite.id)
        assertEquals("s1", favorite.salonId)
    }

    @Test
    fun `getFollowedSalons maps every list item`() = runTest {
        api.followedSalons = listOf(
            SalonFollowDto(id = "f1", salonId = "s1", status = "ACTIVE", createdAt = "t1"),
            SalonFollowDto(id = "f2", salonId = "s2", status = "ACTIVE", createdAt = "t2"),
        )

        val result = repository.getFollowedSalons()

        assertTrue(result.isSuccess)
        assertEquals(listOf("s1", "s2"), result.getOrThrow().map { it.salonId })
    }

    @Test
    fun `getFavoriteSalons maps every list item`() = runTest {
        api.favoriteSalons = listOf(SalonFavoriteDto(id = "fav1", salonId = "s1", createdAt = "t1"))

        val result = repository.getFavoriteSalons()

        assertTrue(result.isSuccess)
        assertEquals(listOf("s1"), result.getOrThrow().map { it.salonId })
    }

    @Test
    fun `unfollowSalon propagates a backend failure as Result failure`() = runTest {
        api.thrownOnUnfollow = java.io.IOException("boom")

        val result = repository.unfollowSalon("s1")

        assertTrue(result.isFailure)
    }
}
