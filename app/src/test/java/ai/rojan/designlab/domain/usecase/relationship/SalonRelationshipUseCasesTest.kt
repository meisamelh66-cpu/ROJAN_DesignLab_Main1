package ai.rojan.designlab.domain.usecase.relationship

import ai.rojan.designlab.domain.repository.CustomerFavoriteSalon
import ai.rojan.designlab.domain.repository.CustomerFollowedSalon
import ai.rojan.designlab.domain.repository.CustomerRelationshipRepository
import ai.rojan.designlab.domain.repository.SalonFollowStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeCustomerRelationshipRepository : CustomerRelationshipRepository {
    var followedSalons: List<CustomerFollowedSalon> = emptyList()
    var favoriteSalons: List<CustomerFavoriteSalon> = emptyList()
    var lastFollowedSalonId: String? = null
    var lastUnfollowedSalonId: String? = null
    var lastFavoritedSalonId: String? = null
    var lastUnfavoritedSalonId: String? = null

    override suspend fun followSalon(salonId: String): Result<CustomerFollowedSalon> {
        lastFollowedSalonId = salonId
        return Result.success(CustomerFollowedSalon("f1", salonId, SalonFollowStatus.ACTIVE, "t1"))
    }

    override suspend fun unfollowSalon(salonId: String): Result<Unit> {
        lastUnfollowedSalonId = salonId
        return Result.success(Unit)
    }

    override suspend fun getFollowedSalons(page: Int, size: Int): Result<List<CustomerFollowedSalon>> =
        Result.success(followedSalons)

    override suspend fun favoriteSalon(salonId: String): Result<CustomerFavoriteSalon> {
        lastFavoritedSalonId = salonId
        return Result.success(CustomerFavoriteSalon("fav1", salonId, "t1"))
    }

    override suspend fun unfavoriteSalon(salonId: String): Result<Unit> {
        lastUnfavoritedSalonId = salonId
        return Result.success(Unit)
    }

    override suspend fun getFavoriteSalons(page: Int, size: Int): Result<List<CustomerFavoriteSalon>> =
        Result.success(favoriteSalons)
}

class SalonRelationshipUseCasesTest {

    private val repository = FakeCustomerRelationshipRepository()

    @Test
    fun `FollowSalonUseCase delegates to the repository with the given salon id`() = runTest {
        val result = FollowSalonUseCase(repository)("s1")

        assertEquals("s1", repository.lastFollowedSalonId)
        assertEquals("s1", result.getOrThrow().salonId)
    }

    @Test
    fun `UnfollowSalonUseCase delegates to the repository with the given salon id`() = runTest {
        UnfollowSalonUseCase(repository)("s1")

        assertEquals("s1", repository.lastUnfollowedSalonId)
    }

    @Test
    fun `GetFollowedSalonsUseCase returns whatever the repository returns`() = runTest {
        repository.followedSalons = listOf(CustomerFollowedSalon("f1", "s1", SalonFollowStatus.ACTIVE, "t1"))

        val result = GetFollowedSalonsUseCase(repository)()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
    }

    @Test
    fun `FavoriteSalonUseCase delegates to the repository with the given salon id`() = runTest {
        val result = FavoriteSalonUseCase(repository)("s1")

        assertEquals("s1", repository.lastFavoritedSalonId)
        assertEquals("s1", result.getOrThrow().salonId)
    }

    @Test
    fun `UnfavoriteSalonUseCase delegates to the repository with the given salon id`() = runTest {
        UnfavoriteSalonUseCase(repository)("s1")

        assertEquals("s1", repository.lastUnfavoritedSalonId)
    }

    @Test
    fun `GetFavoriteSalonsUseCase returns whatever the repository returns`() = runTest {
        repository.favoriteSalons = listOf(CustomerFavoriteSalon("fav1", "s1", "t1"))

        val result = GetFavoriteSalonsUseCase(repository)()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
    }
}
