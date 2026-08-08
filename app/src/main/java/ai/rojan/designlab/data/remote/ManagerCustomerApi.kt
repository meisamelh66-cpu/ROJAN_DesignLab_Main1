package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.CustomerNoteResponseDto
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateCustomerRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Owner-only Retrofit contract for the ROJAN backend Customer CRM API.
 *
 * Supports:
 * - customer list/search/filter
 * - customer profile
 * - customer notes
 * - customer booking history
 * - create/update customer
 *
 * Backend:
 * ROJAN_Backend CustomerController
 */
interface ManagerCustomerApi {

    @GET("api/v1/salons/{salonId}/customers")
    suspend fun list(
        @Path("salonId") salonId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
        @Query("status") status: String? = null,
        @Query("tag") tag: String? = null,
        @Query("search") search: String? = null,
        @Query("sortDirection") sortDirection: String = "ASC",
    ): PagedResponseDto<CustomerResponseDto>


    @GET("api/v1/salons/{salonId}/customers/{customerId}")
    suspend fun get(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
    ): CustomerResponseDto


    @GET("api/v1/salons/{salonId}/customers/{customerId}/notes")
    suspend fun notes(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
    ): List<CustomerNoteResponseDto>


    @GET("api/v1/salons/{salonId}/customers/{customerId}/bookings")
    suspend fun bookings(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("status") status: String? = null,
        @Query("sortDirection") sortDirection: String = "DESC",
    ): PagedResponseDto<BookingResponseDto>


    @POST("api/v1/salons/{salonId}/customers")
    suspend fun create(
        @Path("salonId") salonId: String,
        @Body request: CreateCustomerRequestDto,
    ): CustomerResponseDto


    @PATCH("api/v1/salons/{salonId}/customers/{customerId}")
    suspend fun update(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
        @Body request: UpdateCustomerRequestDto,
    ): CustomerResponseDto
}