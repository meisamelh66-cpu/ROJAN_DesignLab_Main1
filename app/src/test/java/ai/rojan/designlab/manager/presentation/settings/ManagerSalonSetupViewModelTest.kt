package ai.rojan.designlab.manager.presentation.settings

import ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary
import ai.rojan.designlab.manager.domain.repository.ManagerSalonRepository
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * First Salon Pilot, Phase A — hermetic (no real backend, no Android
 * framework) coverage of [ManagerSalonSetupViewModel], same
 * fake-repository approach [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModelTest]
 * already establishes for a Manager ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerSalonSetupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val existingSalon = ManagerSalonSummary(
        id = "salon-1",
        name = "سالن رویان",
        description = "توضیحات",
        phone = "+989120000000",
        email = "salon@example.com",
        address = "تهران، خیابان اصلی",
        latitude = 35.6892,
        longitude = 51.3890,
        active = true,
    )

    @Test
    fun `no salon on load resolves to Empty create mode with a blank form`() = runTest {
        val viewModel = ManagerSalonSetupViewModel(FakeManagerSalonRepository(getMySalonResult = Result.success(null)))

        assertEquals(UiState.Empty, viewModel.loadState.value)
        assertEquals(SalonSetupFormState(), viewModel.formState.value)
    }

    @Test
    fun `existing salon on load resolves to Success edit mode with the form pre-filled`() = runTest {
        val viewModel = ManagerSalonSetupViewModel(FakeManagerSalonRepository(getMySalonResult = Result.success(existingSalon)))

        assertEquals(UiState.Success(existingSalon), viewModel.loadState.value)
        assertEquals(
            SalonSetupFormState(
                name = existingSalon.name,
                description = existingSalon.description.orEmpty(),
                phone = existingSalon.phone,
                email = existingSalon.email.orEmpty(),
                address = existingSalon.address,
                latitude = "35.6892",
                longitude = "51.389",
            ),
            viewModel.formState.value,
        )
    }

    @Test
    fun `existing salon with no coordinates on load resolves to a blank latitude and longitude`() = runTest {
        val viewModel = ManagerSalonSetupViewModel(
            FakeManagerSalonRepository(getMySalonResult = Result.success(existingSalon.copy(latitude = null, longitude = null))),
        )

        assertEquals("", viewModel.formState.value.latitude)
        assertEquals("", viewModel.formState.value.longitude)
    }

    @Test
    fun `load failure resolves to Error with a user-facing message`() = runTest {
        val viewModel = ManagerSalonSetupViewModel(
            FakeManagerSalonRepository(getMySalonResult = Result.failure(RuntimeException("boom"))),
        )

        assertTrue(viewModel.loadState.value is UiState.Error)
    }

    @Test
    fun `save in create mode calls createSalon, not updateSalon, and reports success`() = runTest {
        val created = existingSalon.copy(id = "salon-new")
        val repository = FakeManagerSalonRepository(
            getMySalonResult = Result.success(null),
            createSalonResult = Result.success(created),
        )
        val viewModel = ManagerSalonSetupViewModel(repository)

        viewModel.onNameChange("سالن جدید")
        viewModel.onPhoneChange("+989121111111")
        viewModel.onAddressChange("اصفهان")

        var savedCalled = false
        viewModel.save(onSaved = { savedCalled = true })

        assertEquals(1, repository.createSalonCallCount)
        assertEquals(0, repository.updateSalonCallCount)
        assertTrue(savedCalled)
        assertFalse(viewModel.isSubmitting.value)
        assertNull(viewModel.submitError.value)
        assertEquals(UiState.Success(created), viewModel.loadState.value)
    }

    @Test
    fun `save in edit mode calls updateSalon with the loaded salon id`() = runTest {
        val repository = FakeManagerSalonRepository(
            getMySalonResult = Result.success(existingSalon),
            updateSalonResult = Result.success(existingSalon.copy(name = "نام جدید")),
        )
        val viewModel = ManagerSalonSetupViewModel(repository)

        viewModel.onNameChange("نام جدید")
        var savedCalled = false
        viewModel.save(onSaved = { savedCalled = true })

        assertEquals(0, repository.createSalonCallCount)
        assertEquals(1, repository.updateSalonCallCount)
        assertEquals(existingSalon.id, repository.lastUpdateSalonId)
        assertTrue(savedCalled)
    }

    @Test
    fun `save in edit mode parses the coordinate fields and passes them to updateSalon`() = runTest {
        val repository = FakeManagerSalonRepository(
            getMySalonResult = Result.success(existingSalon),
            updateSalonResult = Result.success(existingSalon),
        )
        val viewModel = ManagerSalonSetupViewModel(repository)

        viewModel.onLatitudeChange("40.7128")
        viewModel.onLongitudeChange("-74.0060")
        viewModel.save(onSaved = {})

        assertEquals(40.7128, repository.lastUpdateLatitude)
        assertEquals(-74.0060, repository.lastUpdateLongitude)
    }

    @Test
    fun `save in edit mode treats blank coordinate fields as null, leaving them unchanged`() = runTest {
        val repository = FakeManagerSalonRepository(
            getMySalonResult = Result.success(existingSalon),
            updateSalonResult = Result.success(existingSalon),
        )
        val viewModel = ManagerSalonSetupViewModel(repository)

        viewModel.onLatitudeChange("")
        viewModel.onLongitudeChange("")
        viewModel.save(onSaved = {})

        assertEquals(1, repository.updateSalonCallCount)
        assertNull(repository.lastUpdateLatitude)
        assertNull(repository.lastUpdateLongitude)
    }

    @Test
    fun `save does not call the repository when latitude is out of range`() = runTest {
        val repository = FakeManagerSalonRepository(getMySalonResult = Result.success(existingSalon))
        val viewModel = ManagerSalonSetupViewModel(repository)

        viewModel.onLatitudeChange("120")
        viewModel.save(onSaved = {})

        assertEquals(0, repository.updateSalonCallCount)
    }

    @Test
    fun `save does not call the repository when longitude is not a number`() = runTest {
        val repository = FakeManagerSalonRepository(getMySalonResult = Result.success(existingSalon))
        val viewModel = ManagerSalonSetupViewModel(repository)

        viewModel.onLongitudeChange("not-a-number")
        viewModel.save(onSaved = {})

        assertEquals(0, repository.updateSalonCallCount)
    }

    @Test
    fun `save in create mode never sends coordinates since createSalon has no such parameters`() = runTest {
        val created = existingSalon.copy(id = "salon-new")
        val repository = FakeManagerSalonRepository(
            getMySalonResult = Result.success(null),
            createSalonResult = Result.success(created),
        )
        val viewModel = ManagerSalonSetupViewModel(repository)

        viewModel.onNameChange("سالن جدید")
        viewModel.onPhoneChange("+989121111111")
        viewModel.onAddressChange("اصفهان")
        viewModel.onLatitudeChange("35.6892")
        viewModel.onLongitudeChange("51.389")
        viewModel.save(onSaved = {})

        assertEquals(1, repository.createSalonCallCount)
        assertEquals(0, repository.updateSalonCallCount)
    }

    @Test
    fun `save does not call the repository when a required field is blank`() = runTest {
        val repository = FakeManagerSalonRepository(getMySalonResult = Result.success(null))
        val viewModel = ManagerSalonSetupViewModel(repository)

        // name/phone/address all still blank - never touched.
        var savedCalled = false
        viewModel.save(onSaved = { savedCalled = true })

        assertEquals(0, repository.createSalonCallCount)
        assertFalse(savedCalled)
    }

    @Test
    fun `save failure sets submitError, resets isSubmitting, and does not invoke onSaved`() = runTest {
        val repository = FakeManagerSalonRepository(
            getMySalonResult = Result.success(null),
            createSalonResult = Result.failure(RuntimeException("network down")),
        )
        val viewModel = ManagerSalonSetupViewModel(repository)

        viewModel.onNameChange("سالن جدید")
        viewModel.onPhoneChange("+989121111111")
        viewModel.onAddressChange("اصفهان")

        var savedCalled = false
        viewModel.save(onSaved = { savedCalled = true })

        assertFalse(savedCalled)
        assertFalse(viewModel.isSubmitting.value)
        assertTrue(viewModel.submitError.value != null)
    }

    private class FakeManagerSalonRepository(
        private val getMySalonResult: Result<ManagerSalonSummary?>,
        private val createSalonResult: Result<ManagerSalonSummary>? = null,
        private val updateSalonResult: Result<ManagerSalonSummary>? = null,
    ) : ManagerSalonRepository {

        var createSalonCallCount = 0
            private set
        var updateSalonCallCount = 0
            private set
        var lastUpdateSalonId: String? = null
            private set
        var lastUpdateLatitude: Double? = null
            private set
        var lastUpdateLongitude: Double? = null
            private set

        override suspend fun getMySalon(): Result<ManagerSalonSummary?> = getMySalonResult

        override suspend fun createSalon(
            name: String,
            description: String?,
            phone: String,
            email: String?,
            address: String,
        ): Result<ManagerSalonSummary> {
            createSalonCallCount++
            return createSalonResult ?: error("createSalonResult not stubbed")
        }

        override suspend fun updateSalon(
            salonId: String,
            name: String,
            description: String?,
            phone: String,
            email: String?,
            address: String,
            latitude: Double?,
            longitude: Double?,
        ): Result<ManagerSalonSummary> {
            updateSalonCallCount++
            lastUpdateSalonId = salonId
            lastUpdateLatitude = latitude
            lastUpdateLongitude = longitude
            return updateSalonResult ?: error("updateSalonResult not stubbed")
        }
    }
}
