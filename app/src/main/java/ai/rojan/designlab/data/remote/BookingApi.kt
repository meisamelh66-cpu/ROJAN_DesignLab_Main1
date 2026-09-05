package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateBookingRequestDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.RescheduleBookingRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit contract for the ROJAN backend's Booking API (`ROJAN_Backend/API_CONTRACT.md`). */
interface BookingApi {

    @POST("api/v1/bookings")
    suspend fun createBooking(
        @Body request: CreateBookingRequestDto,
        @Header("Idempotency-Key") idempotencyKey: String?,
    ): BookingResponseDto

    @GET("api/v1/bookings/mine")
    suspend fun myBookings(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("status") status: String?,
    ): PagedResponseDto<BookingResponseDto>

    @GET("api/v1/bookings/{bookingId}")
    suspend fun getBooking(@Path("bookingId") bookingId: String): BookingResponseDto

    @PATCH("api/v1/bookings/{bookingId}/cancel")
    suspend fun cancelBooking(@Path("bookingId") bookingId: String): BookingResponseDto

    /** TEAM2-003: salon owner only — backend returns 403 for any other caller. */
    @PATCH("api/v1/bookings/{bookingId}/confirm")
    suspend fun confirmBooking(@Path("bookingId") bookingId: String): BookingResponseDto

    /** TEAM2-003: salon owner only — backend returns 403 for any other caller. */
    @PATCH("api/v1/bookings/{bookingId}/complete")
    suspend fun completeBooking(@Path("bookingId") bookingId: String): BookingResponseDto

    /** TEAM2-003: the booking's customer or the salon owner. */
    @PUT("api/v1/bookings/{bookingId}/reschedule")
    suspend fun rescheduleBooking(
        @Path("bookingId") bookingId: String,
        @Body request: RescheduleBookingRequestDto,
    ): BookingResponseDto
}
