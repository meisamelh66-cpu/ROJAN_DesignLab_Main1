package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CustomerNoteDto
import ai.rojan.designlab.data.remote.dto.CustomerRecordDto
import ai.rojan.designlab.data.remote.dto.CustomerTagDto
import ai.rojan.designlab.data.remote.dto.CustomerTimelineEntryDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * FIX-006. Retrofit contract for the backend Customer CRM
 * (`ROJAN_Backend`'s `CustomerController`, base path
 * `/api/v1/salons/{salonId}/customer-records`). Owner-only; a non-owner
 * caller gets 403, an unknown record gets 404.
 *
 * [list] exists only to resolve the legacy roster id the Customers list
 * navigates with (a linked account's `UserId`) to this salon's CRM
 * [CustomerRecordDto.id] - there is no `?userId=` filter on the backend and
 * the profile detail endpoint is keyed on the CRM id.
 */
interface CustomerApi {

    /** `size` is capped at 100 by the backend. */
    @GET("api/v1/salons/{salonId}/customer-records")
    suspend fun list(
        @Path("salonId") salonId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
    ): PagedResponseDto<CustomerRecordDto>

    @GET("api/v1/salons/{salonId}/customer-records/{customerId}")
    suspend fun get(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
    ): CustomerRecordDto

    @GET("api/v1/salons/{salonId}/customer-records/{customerId}/notes")
    suspend fun notes(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
    ): List<CustomerNoteDto>

    @GET("api/v1/salons/{salonId}/customer-records/{customerId}/tags")
    suspend fun tags(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
    ): List<CustomerTagDto>

    @GET("api/v1/salons/{salonId}/customer-records/{customerId}/timeline")
    suspend fun timeline(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): PagedResponseDto<CustomerTimelineEntryDto>

    @GET("api/v1/salons/{salonId}/customer-records/{customerId}/bookings")
    suspend fun bookings(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): PagedResponseDto<BookingResponseDto>
}
