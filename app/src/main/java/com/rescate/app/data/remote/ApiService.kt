package com.rescate.app.data.remote

import com.rescate.app.data.model.Complaint
import com.rescate.app.data.model.Donation
import com.rescate.app.data.model.Impact
import com.rescate.app.data.model.Pack
import com.rescate.app.data.model.Payment
import com.rescate.app.data.model.Rating
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

 
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(
    val names: String, val lastNames: String, val email: String, val phone: String,
    val district: String, val password: String, val role: String,
    val document: String? = null, val storeName: String? = null, val yapeNumber: String? = null
)
data class AuthResponse(val token: String, val user: User)
data class ReserveRequest(val packId: Long, val quantity: Int)
data class YapeReportRequest(val reference: String)
data class DonationRequest(
    val title: String, val foodType: String, val kg: Double,
    val expiresInDays: Int, val district: String, val notes: String
)
data class MessageResponse(val message: String)
data class ComplaintRequest(
    val reservationId: Long?, val storeId: Long?, val type: String, val goodType: String,
    val amount: Double?, val detail: String, val request: String
)
data class RatingRequest(val reservationId: Long, val stars: Int, val comment: String?)




interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/recover")
    suspend fun recoverPassword(@Body body: Map<String, String>): MessageResponse

    @GET("packs")
    suspend fun packs(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("km") km: Float,
        @Query("category") category: String?,
        @Query("q") query: String?
    ): List<Pack>

    @GET("packs/{id}")
    suspend fun pack(@Path("id") id: Long): Pack

     
    @POST("reservations")
    suspend fun reserve(@Body body: ReserveRequest): Reservation

     
    @POST("reservations/{id}/yape")
    suspend fun reportYape(@Path("id") id: Long, @Body body: YapeReportRequest): Reservation

    @PATCH("reservations/{id}/cancel")
    suspend fun cancel(@Path("id") id: Long): Reservation

    @GET("reservations/me")
    suspend fun myReservations(): List<Reservation>

    @GET("reservations/{id}")
    suspend fun reservation(@Path("id") id: Long): Reservation

    @GET("impact/me")
    suspend fun impact(@Query("period") period: String): Impact

    @POST("donations")
    suspend fun donate(@Body body: DonationRequest): Donation

    @GET("donations/me")
    suspend fun myDonations(): List<Donation>

    @PATCH("donations/{id}/withdraw")
    suspend fun withdrawDonation(@Path("id") id: Long): MessageResponse

     
    @Multipart
    @POST("merchant/packs")
    suspend fun publishPack(
        @Part("data") data: RequestBody,
        @Part photo: MultipartBody.Part?
    ): Pack

    @GET("merchant/packs")
    suspend fun merchantPacks(): List<Pack>

    @GET("merchant/orders")
    suspend fun merchantOrders(): List<Reservation>

    @PUT("merchant/yape")
    suspend fun updateYape(@Body body: Map<String, String>): MessageResponse

    @GET("merchant/payments")
    suspend fun merchantPayments(): List<Payment>

    @PATCH("merchant/payments/{id}/approve")
    suspend fun approvePayment(@Path("id") id: Long): Payment

    @PATCH("merchant/payments/{id}/reject")
    suspend fun rejectPayment(@Path("id") id: Long): Payment

    @PATCH("merchant/reservations/{code}/pickup")
    suspend fun markPickedUp(@Path("code") code: String): Reservation

     
    @POST("complaints")
    suspend fun createComplaint(@Body body: ComplaintRequest): Complaint

    @GET("complaints/me")
    suspend fun myComplaints(): List<Complaint>

    @GET("merchant/complaints")
    suspend fun merchantComplaints(): List<Complaint>

    @PATCH("merchant/complaints/{id}/respond")
    suspend fun respondComplaint(@Path("id") id: Long, @Body body: Map<String, String>): Complaint

     
    @POST("ratings")
    suspend fun rate(@Body body: RatingRequest): Rating

    @GET("stores/{id}/ratings")
    suspend fun storeRatings(@Path("id") storeId: Long): List<Rating>

    @GET("merchant/ratings")
    suspend fun merchantRatings(): List<Rating>

    @PATCH("merchant/ratings/{id}/reply")
    suspend fun replyRating(@Path("id") id: Long, @Body body: Map<String, String>): Rating
}
