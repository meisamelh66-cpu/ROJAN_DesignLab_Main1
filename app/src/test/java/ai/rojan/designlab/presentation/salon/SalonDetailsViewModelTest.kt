package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.domain.repository.GalleryImage
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.SalonWorkingHours
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategory
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.domain.repository.WorkingHoursRepository
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

private val SALON = Salon(id = "salon-1", name = "Glow Salon", description = null, phone = "0912", email = null, address = "Addr")

private class FakeSalonRepository(private val galleryResult: Result<List<GalleryImage>>) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?, sortDirection: String): Result<PagedResult<Salon>> = error("not used")
    override suspend fun getSalon(salonId: String): Result<Salon> = Result.success(SALON)
    override suspend fun myOwnedSalons(): Result<List<Salon>> = error("not used")
    override suspend fun getGallery(salonId: String): Result<List<GalleryImage>> = galleryResult
}

private class EmptyServiceCategoryRepository : ServiceCategoryRepository {
    override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> = Result.success(emptyList())
}

private class EmptyServiceRepository : ServiceRepository {
    override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> = Result.success(emptyList())
}

private class EmptySpecialistRepository : SpecialistRepository {
    override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = Result.success(emptyList())
    override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> = error("not used")
}

private class EmptyWorkingHoursRepository : WorkingHoursRepository {
    override suspend fun getWorkingHours(salonId: String): Result<List<SalonWorkingHours>> = Result.success(emptyList())
}

/**
 * D6 (Customer Gallery/Media, Build now): [SalonDetailsViewModel.load]'s gallery handling —
 * mirrors the "enrichment, not a hard gate" behaviour already proven for `workingHours`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SalonDetailsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(galleryResult: Result<List<GalleryImage>>) = SalonDetailsViewModel(
        salonId = "salon-1",
        salonRepository = FakeSalonRepository(galleryResult),
        serviceCategoryRepository = EmptyServiceCategoryRepository(),
        serviceRepository = EmptyServiceRepository(),
        specialistRepository = EmptySpecialistRepository(),
        workingHoursRepository = EmptyWorkingHoursRepository(),
    )

    @Test
    fun `a successful gallery fetch is reflected in SalonDetailsData`() = runTest {
        val images = listOf(GalleryImage(id = "img-1", url = "https://cdn.example.com/a.jpg"))
        val vm = viewModel(Result.success(images))

        val state = vm.state as UiState.Success
        assertEquals(images, state.data.gallery)
    }

    @Test
    fun `a gallery fetch failure degrades to an empty gallery, never a whole-screen Error state`() = runTest {
        val vm = viewModel(Result.failure(RuntimeException("boom")))

        val state = vm.state
        assertTrue(state is UiState.Success)
        assertTrue((state as UiState.Success).data.gallery.isEmpty())
    }

    @Test
    fun `a salon with no published photos shows a real empty gallery, not an error`() = runTest {
        val vm = viewModel(Result.success(emptyList()))

        val state = vm.state as UiState.Success
        assertTrue(state.data.gallery.isEmpty())
    }
}
