package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.CreateServiceRequest
import ai.rojan.designlab.data.remote.dto.ServiceResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateServiceRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Owner-only write operations for the ROJAN backend's Service API
 * (`ROJAN_Backend/API_CONTRACT.md`). Reads reuse the existing
 * [ServiceApi]/[ServiceCategoryApi] (same endpoints Customer already
 * calls) — this interface only adds the create/update/deactivate
 * methods that have no meaning for the Customer flavor.
 */
interface ManagerServiceApi {

    @POST("api/v1/salons/{salonId}/categories/{categoryId}/services")
    suspend fun create(
        @Path("salonId") salonId: String,
        @Path("categoryId") categoryId: String,
        @Body request: CreateServiceRequest,
    ): ServiceResponseDto

    @PUT("api/v1/salons/{salonId}/categories/{categoryId}/services/{serviceId}")
    suspend fun update(
        @Path("salonId") salonId: String,
        @Path("categoryId") categoryId: String,
        @Path("serviceId") serviceId: String,
        @Body request: UpdateServiceRequest,
    ): ServiceResponseDto

    @DELETE("api/v1/salons/{salonId}/categories/{categoryId}/services/{serviceId}")
    suspend fun deactivate(
        @Path("salonId") salonId: String,
        @Path("categoryId") categoryId: String,
        @Path("serviceId") serviceId: String,
    )
}
