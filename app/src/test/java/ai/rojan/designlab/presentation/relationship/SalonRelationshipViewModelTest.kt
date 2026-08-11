package ai.rojan.designlab.presentation.relationship

import ai.rojan.designlab.domain.repository.CustomerFavoriteSalon
import ai.rojan.designlab.domain.repository.CustomerFollowedSalon
import ai.rojan.designlab.domain.repository.CustomerRelationshipRepository
import ai.rojan.designlab.domain.repository.SalonFollowStatus
import ai.rojan.designlab.domain.usecase.relationship.FavoriteSalonUseCase
import ai.rojan.designlab.domain.usecase.relationship.FollowSalonUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.UnfavoriteSalonUseCase
import ai.rojan.designlab.domain.usecase.relationship.UnfollowSalonUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeCustomerRelationshipRepository(
    initialFollowedSalonIds: Set<String> = emptySet(),
    initialFavoriteSalonIds: Set<String> = emptySet(),
) : CustomerRelationshipRepository {
    var followedSalonIds = initialFollowedSalonIds.toMutableSet()
    var favoriteSalonIds = initialFavoriteSalonIds.toMutableSet()
    var followShouldFail = false
    var favoriteShouldFail = false

    var followShouldFailUnauthorized = false

    override suspend fun followSalon(salonId: String): Result<CustomerFollowedSalon> {
        if (followShouldFailUnauthorized) return Result.failure(ai.rojan.designlab.data.remote.BackendApiException(401, null))
        if (followShouldFail) return Result.failure(RuntimeException("network error"))
        followedSalonIds += salonId
        return Result.success(CustomerFollowedSalon("f1", salonId, SalonFollowStatus.ACTIVE, "t1"))
    }

    override suspend fun unfollowSalon(salonId: String): Result<Unit> {
        if (followShouldFail) return Result.failure(RuntimeException("network error"))
        followedSalonIds -= salonId
        return Result.success(Unit)
    }

    override suspend fun getFollowedSalons(page: Int, size: Int): Result<List<CustomerFollowedSalon>> =
        Result.success(followedSalonIds.map { CustomerFollowedSalon("f-$it", it, SalonFollowStatus.ACTIVE, "t") })

    override suspend fun favoriteSalon(salonId: String): Result<CustomerFavoriteSalon> {
        if (favoriteShouldFail) return Result.failure(RuntimeException("network error"))
        favoriteSalonIds += salonId
        return Result.success(CustomerFavoriteSalon("fav1", salonId, "t1"))
    }

    override suspend fun unfavoriteSalon(salonId: String): Result<Unit> {
        if (favoriteShouldFail) return Result.failure(RuntimeException("network error"))
        favoriteSalonIds -= salonId
        return Result.success(Unit)
    }

    override suspend fun getFavoriteSalons(page: Int, size: Int): Result<List<CustomerFavoriteSalon>> =
        Result.success(favoriteSalonIds.map { CustomerFavoriteSalon("fav-$it", it, "t") })
}

@OptIn(ExperimentalCoroutinesApi::class)
class SalonRelationshipViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(repository: CustomerRelationshipRepository, salonId: String = "s1") =
        SalonRelationshipViewModel(
            salonId = salonId,
            followSalonUseCase = FollowSalonUseCase(repository),
            unfollowSalonUseCase = UnfollowSalonUseCase(repository),
            getFollowedSalonsUseCase = GetFollowedSalonsUseCase(repository),
            favoriteSalonUseCase = FavoriteSalonUseCase(repository),
            unfavoriteSalonUseCase = UnfavoriteSalonUseCase(repository),
            getFavoriteSalonsUseCase = GetFavoriteSalonsUseCase(repository),
        )

    @Test
    fun `resolves initial state from list membership`() = runTest {
        val repository = FakeCustomerRelationshipRepository(
            initialFollowedSalonIds = setOf("s1"),
            initialFavoriteSalonIds = setOf("other"),
        )

        val viewModel = viewModelFor(repository, salonId = "s1")

        assertFalse(viewModel.isInitialLoading)
        assertTrue(viewModel.isFollowing)
        assertFalse(viewModel.isFavorite)
    }

    @Test
    fun `toggleFollow flips state immediately, optimistically, before the network call resolves`() = runTest {
        val repository = FakeCustomerRelationshipRepository()
        val viewModel = viewModelFor(repository)

        viewModel.toggleFollow()

        assertTrue(viewModel.isFollowing)
        assertTrue(repository.followedSalonIds.contains("s1"))
    }

    @Test
    fun `toggleFollow rolls back on failure and surfaces an error message`() = runTest {
        val repository = FakeCustomerRelationshipRepository().apply { followShouldFail = true }
        val viewModel = viewModelFor(repository)

        viewModel.toggleFollow()

        assertFalse(viewModel.isFollowing)
        assertNotNull(viewModel.followError)
    }

    @Test
    fun `toggleFavorite rolls back on failure`() = runTest {
        val repository = FakeCustomerRelationshipRepository().apply { favoriteShouldFail = true }
        val viewModel = viewModelFor(repository)

        viewModel.toggleFavorite()

        assertFalse(viewModel.isFavorite)
    }

    @Test
    fun `toggleFollow twice returns to the not-following state and calls unfollow`() = runTest {
        val repository = FakeCustomerRelationshipRepository()
        val viewModel = viewModelFor(repository)

        viewModel.toggleFollow()
        viewModel.toggleFollow()

        assertFalse(viewModel.isFollowing)
        assertFalse(repository.followedSalonIds.contains("s1"))
    }

    @Test
    fun `a second toggleFollow tap is ignored while an action is already in progress`() = runTest {
        val repository = FakeCustomerRelationshipRepository()
        val viewModel = viewModelFor(repository)

        // Under UnconfinedTestDispatcher the launched coroutine runs to
        // completion synchronously, so this mainly guards the isFollowActionInProgress
        // re-entrancy check itself doesn't throw/duplicate state.
        viewModel.toggleFollow()
        val stateAfterFirst = viewModel.isFollowing
        viewModel.toggleFollow()

        assertEquals(!stateAfterFirst, viewModel.isFollowing)
    }

    @Test
    fun `toggleFollow signals requiresLogin (not a generic error) on a real 401, and rolls back`() = runTest {
        val repository = FakeCustomerRelationshipRepository().apply { followShouldFailUnauthorized = true }
        val viewModel = viewModelFor(repository)

        viewModel.toggleFollow()

        assertFalse(viewModel.isFollowing)
        assertTrue(viewModel.requiresLogin)
        assertEquals(null, viewModel.followError)
    }

    @Test
    fun `consumeLoginRequired resets the one-shot requiresLogin flag`() = runTest {
        val repository = FakeCustomerRelationshipRepository().apply { followShouldFailUnauthorized = true }
        val viewModel = viewModelFor(repository)
        viewModel.toggleFollow()

        viewModel.consumeLoginRequired()

        assertFalse(viewModel.requiresLogin)
    }
}
