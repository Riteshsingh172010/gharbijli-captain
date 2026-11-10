package me.gharbijli.partner.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface PartnerApiService {

    @POST("api/partner/auth/login")
    suspend fun login(@Body req: PartnerLoginRequest): Response<PartnerAuthResponse>

    @POST("api/partner/auth/register")
    suspend fun register(@Body req: PartnerRegisterRequest): Response<PartnerAuthResponse>

    @GET("api/partner/me")
    suspend fun getProfile(@Header("Authorization") token: String): Response<PartnerProfileResponse>

    @POST("api/partner/availability")
    suspend fun toggleAvailability(
        @Header("Authorization") token: String,
        @Body req: ToggleAvailabilityRequest
    ): Response<ApiResponse<PartnerProfile>>

    @GET("api/partner/requests")
    suspend fun getPendingDispatches(@Header("Authorization") token: String): Response<PendingDispatchesResponse>

    @POST("api/partner/requests/{id}/accept")
    suspend fun acceptBooking(
        @Header("Authorization") token: String,
        @Path("id") dispatchId: String
    ): Response<AcceptBookingResponse>

    @POST("api/partner/requests/{id}/reject")
    suspend fun rejectBooking(
        @Header("Authorization") token: String,
        @Path("id") dispatchId: String
    ): Response<ApiResponse<Unit>>

    @GET("api/partner/bookings")
    suspend fun getActiveBookings(@Header("Authorization") token: String): Response<ActiveBookingsResponse>

    @GET("api/partner/bookings/{id}")
    suspend fun getBookingDetail(
        @Header("Authorization") token: String,
        @Path("id") bookingId: String
    ): Response<SingleBookingResponse>

    @POST("api/partner/bookings/{id}/status")
    suspend fun updateBookingStatus(
        @Header("Authorization") token: String,
        @Path("id") bookingId: String,
        @Body req: UpdateStatusRequest
    ): Response<SingleBookingResponse>

    @GET("api/partner/earnings")
    suspend fun getEarnings(@Header("Authorization") token: String): Response<EarningsSummaryResponse>

    companion object {
        const val BASE_URL = "https://gharbijli-fwnz.vercel.app/"

        fun create(): PartnerApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PartnerApiService::class.java)
        }
    }
}
