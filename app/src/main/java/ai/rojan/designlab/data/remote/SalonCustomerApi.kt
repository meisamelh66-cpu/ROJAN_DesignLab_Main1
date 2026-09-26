package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Manager Booking Creation Integrity follow-up. Retrofit contract for the
 * ROJAN backend's salon-scoped customer search (`ROJAN_Backend`'s
 * `CustomerController.list` — there is no separate `SalonCustomerController`).
 *
 * P0 contract-alignment fix: this previously declared a bare
 * `List<UserResponseDto>` response with a `query` wire parameter. The real
 * endpoint returns the same paginated `PagedResponseDto<CustomerResponseDto>`
 * envelope every other CRM list endpoint uses (see `ManagerCustomerApi.list`
 * against this identical path) and reads the search term as `search`, not
 * `query` — the old shape never deserialized against the real backend.
 * `size` defaults to 100 to match `ManagerCustomerApi.list`'s own default;
 * this caller only ever needed one page's worth of matches, not a
 * paged/scrolling UI. Requires `Permission.VIEW_CRM` (salon owner or a
 * manager-role membership) — not owner-only, a 403 is still possible for a
 * caller with neither.
 */
interface SalonCustomerApi {

    @GET("api/v1/salons/{salonId}/customers")
    suspend fun search(
        @Path("salonId") salonId: String,
        @Query("search") query: String?,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
    ): PagedResponseDto<CustomerResponseDto>
}
