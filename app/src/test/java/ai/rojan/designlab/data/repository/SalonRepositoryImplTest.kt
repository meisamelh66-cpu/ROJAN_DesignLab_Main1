package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.SalonApi
import ai.rojan.designlab.data.remote.dto.GalleryImageResponseDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.SalonResponseDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSalonApi : SalonApi {
    var galleryResponse: List<GalleryImageResponseDto> = emptyList()
    var thrown: Throwable? = null

    override suspend fun browseSalons(page: Int, size: Int, name: String?, sortDirection: String): PagedResponseDto<SalonResponseDto> =
        error("not used by these tests")

    override suspend fun getSalon(salonId: String): SalonResponseDto = error("not used by these tests")

    override suspend fun mySalons(): List<SalonResponseDto> = error("not used by these tests")

    override suspend fun getGallery(salonId: String): List<GalleryImageResponseDto> {
        thrown?.let { throw it }
        return galleryResponse
    }
}

/** D6 (Customer Gallery/Media, Build now): [SalonRepositoryImpl.getGallery] mapping behaviour. */
class SalonRepositoryImplTest {

    private val api = FakeSalonApi()
    private val repository = SalonRepositoryImpl(api)

    @Test
    fun `getGallery maps every real dto field to the domain GalleryImage, dropping salonId and displayOrder the UI never needs`() = runTest {
        api.galleryResponse = listOf(
            GalleryImageResponseDto(id = "img-1", salonId = "salon-1", url = "https://cdn.example.com/a.jpg", displayOrder = 0),
            GalleryImageResponseDto(id = "img-2", salonId = "salon-1", url = "https://cdn.example.com/b.jpg", displayOrder = 1),
        )

        val result = repository.getGallery("salon-1")

        assertTrue(result.isSuccess)
        val images = result.getOrThrow()
        assertEquals(2, images.size)
        assertEquals("img-1", images[0].id)
        assertEquals("https://cdn.example.com/a.jpg", images[0].url)
        assertEquals("img-2", images[1].id)
    }

    @Test
    fun `getGallery degrades to a real empty list, never an exception, when the salon has no gallery images`() = runTest {
        api.galleryResponse = emptyList()

        val result = repository.getGallery("salon-1")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `getGallery surfaces a real backend failure as Result failure, never an uncaught crash`() = runTest {
        api.thrown = java.io.IOException("offline")

        val result = repository.getGallery("salon-1")

        assertTrue(result.isFailure)
    }
}
