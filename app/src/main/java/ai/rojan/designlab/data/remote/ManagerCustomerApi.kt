package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateCustomerNoteRequestDto
import ai.rojan.designlab.data.remote.dto.CreateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.CustomerNoteResponseDto
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.dto.LinkCustomerToUserRequestDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.UserLinkCandidateResponseDto
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


    /**
     * Phase F4 — the real, already backend-tested `POST .../customers/{customerId}/notes`
     * (`ROJAN_Backend`'s `CustomerController.addNote` / `AddCustomerNoteUseCase`, `Permission.MANAGE_CRM`).
     * Same route as [notes]; this is the write side of it, not a second endpoint.
     */
    @POST("api/v1/salons/{salonId}/customers/{customerId}/notes")
    suspend fun addNote(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
        @Body request: CreateCustomerNoteRequestDto,
    ): CustomerNoteResponseDto


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


    /**
     * CRM Customer -> User Account Linking, Phase 2. Resolves the User account matching this
     * customer's own already-on-file phone number, for explicit Manager confirmation before
     * calling [link]. No phone/email parameter exists here - the backend reads the phone number
     * directly from the requested Customer (`ROJAN_Backend`'s `LookupUserForCustomerLinkUseCase`,
     * commit 0abd7b1); a 404 means no eligible User was found (or the match was inactive, or the
     * customer has no phone on file - all three collapse to the same not-found response).
     */
    @GET("api/v1/salons/{salonId}/customers/{customerId}/link/lookup")
    suspend fun lookupLinkCandidate(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
    ): UserLinkCandidateResponseDto


    /**
     * CRM Customer -> User Account Linking, Phase 2. The only mutation that can ever set
     * `Customer.userId` - explicit, Manager-confirmed, never called automatically after
     * [lookupLinkCandidate] (`ROJAN_Backend`'s `LinkCustomerToUserUseCase`, commit 4937192).
     */
    @POST("api/v1/salons/{salonId}/customers/{customerId}/link")
    suspend fun link(
        @Path("salonId") salonId: String,
        @Path("customerId") customerId: String,
        @Body request: LinkCustomerToUserRequestDto,
    ): CustomerResponseDto
}