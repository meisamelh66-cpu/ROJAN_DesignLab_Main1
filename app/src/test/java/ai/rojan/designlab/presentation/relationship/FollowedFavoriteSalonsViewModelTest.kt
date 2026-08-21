package ai.rojan.designlab.presentation.relationship

import ai.rojan.designlab.domain.repository.CustomerFavoriteSalon
import ai.rojan.designlab.domain.repository.CustomerFollowedSalon
import ai.rojan.designlab.domain.repository.CustomerRelationshipRepository
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonFollowStatus
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeRelationshipRepository : CustomerRelationshipRepository {
    var followed: List<CustomerFollowedSalon> = emptyList()
    var favorites: List<CustomerFavoriteSalon> = emptyList()
    var shouldFail = false

    override suspend fun followSalon(salonId: String) = error("not used")
    override suspend fun unfollowSalon(salonId: String) = error("not used")
    override suspend fun getFollowedSalons(page: Int, size: Int): Result<List<CustomerFollowedSalon>> =
        if (shouldFail) Result.failure(RuntimeException("boom")) else Result.success(followed)

    override suspend fun favoriteSalon(salonId: String) = error("not used")
    override suspend fun unfavoriteSalon(salonId: String) = error("not used")
    override suspend fun getFavoriteSalons(page: Int, size: Int): Result<List<CustomerFavoriteSalon>> =
        if (shouldFail) Result.failure(RuntimeException("boom")) else Result.success(favorites)
}

private class FakeSalonRepository(private val salonsById: Map<String, Salon>) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?, sortDirection: String): Result<PagedResult<Salon>> = error("not used")
    override suspend fun getSalon(salonId: String): Result<Salon> =
        salonsById[salonId]?.let { Result.success(it) } ?: Result.failure(RuntimeException("not found"))
    override suspend fun getGallery(salonId: String): Result<List<String>> = error("not used")
}

private fun salon(id: String, name: String) = Salon(id = id, name = name, description = null, phone = "0912", email = null, address = "Addr $id")

@OptIn(ExperimentalCoroutinesApi::class)
class FollowedFavoriteSalonsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FollowedSalonsViewModel starts empty when the customer follows nothing`() = runTest {
        val repository = FakeRelationshipRepository()
        val viewModel = FollowedSalonsViewModel(GetFollowedSalonsUseCase(repository), FakeSalonRepository(emptyMap()))

        assertEquals(UiState.Empty, viewModel.state)
    }

    @Test
    fun `FollowedSalonsViewModel enriches each row with the real salon name`() = runTest {
        val repository = FakeRelationshipRepository().apply {
            followed = listOf(CustomerFollowedSalon("f1", "s1", SalonFollowStatus.ACTIVE, "t1"))
        }
        val salonRepository = FakeSalonRepository(mapOf("s1" to salon("s1", "Salon One")))

        val viewModel = FollowedSalonsViewModel(GetFollowedSalonsUseCase(repository), salonRepository)

        val state = viewModel.state
        assertTrue(state is UiState.Success)
        val item = (state as UiState.Success).data.single()
        assertEquals("s1", item.salonId)
        assertEquals("Salon One", item.salonName)
    }

    @Test
    fun `FollowedSalonsViewModel surfaces a repository failure as Error`() = runTest {
        val repository = FakeRelationshipRepository().apply { shouldFail = true }
        val viewModel = FollowedSalonsViewModel(GetFollowedSalonsUseCase(repository), FakeSalonRepository(emptyMap()))

        assertTrue(viewModel.state is UiState.Error)
    }

    @Test
    fun `FavoriteSalonsViewModel enriches each row with the real salon name`() = runTest {
        val repository = FakeRelationshipRepository().apply {
            favorites = listOf(CustomerFavoriteSalon("fav1", "s1", "t1"))
        }
        val salonRepository = FakeSalonRepository(mapOf("s1" to salon("s1", "Salon One")))

        val viewModel = FavoriteSalonsViewModel(GetFavoriteSalonsUseCase(repository), salonRepository)

        val state = viewModel.state
        assertTrue(state is UiState.Success)
        assertEquals("Salon One", (state as UiState.Success).data.single().salonName)
    }

    @Test
    fun `FavoriteSalonsViewModel starts empty when the customer has no favorites`() = runTest {
        val repository = FakeRelationshipRepository()
        val viewModel = FavoriteSalonsViewModel(GetFavoriteSalonsUseCase(repository), FakeSalonRepository(emptyMap()))

        assertEquals(UiState.Empty, viewModel.state)
    }
}
