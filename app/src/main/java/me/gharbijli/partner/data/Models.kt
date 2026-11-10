package me.gharbijli.partner.data

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
)

data class PartnerLoginRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("pin") val pin: String
)

data class PartnerRegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("pin") val pin: String,
    @SerializedName("experienceYears") val experienceYears: Int = 2,
    @SerializedName("skills") val skills: List<String> = emptyList(),
    @SerializedName("serviceAreas") val serviceAreas: List<String> = emptyList()
)

data class PartnerAuthResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("token") val token: String? = null,
    @SerializedName("partner") val partner: PartnerProfile? = null,
    @SerializedName("message") val message: String? = null
)

data class PartnerProfile(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = "Electrician",
    @SerializedName("phone") val phone: String? = "",
    @SerializedName("skills") val skills: List<String>? = emptyList(),
    @SerializedName("serviceAreas") val serviceAreas: List<String>? = emptyList(),
    @SerializedName("isOnline") val isOnline: Boolean = false,
    @SerializedName("status") val status: String? = "APPROVED",
    @SerializedName("rating") val rating: Double = 4.9,
    @SerializedName("totalJobsCompleted") val totalJobsCompleted: Int = 0,
    @SerializedName("todayEarnings") val todayEarnings: Int = 0,
    @SerializedName("totalEarnings") val totalEarnings: Int = 0
)

data class PartnerProfileResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("partner") val partner: PartnerProfile? = null
)

data class ToggleAvailabilityRequest(
    @SerializedName("isOnline") val isOnline: Boolean
)

data class PendingDispatchesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("requests") val requests: List<DispatchRequest>? = emptyList()
)

data class DispatchRequest(
    @SerializedName("dispatchId") val dispatchId: String,
    @SerializedName("bookingId") val bookingId: String,
    @SerializedName("serviceName") val serviceName: String? = "Electrical Repair",
    @SerializedName("locality") val locality: String? = "Bhilai",
    @SerializedName("customerAddress") val customerAddress: String? = "",
    @SerializedName("scheduledTime") val scheduledTime: String? = "Today, Urgent",
    @SerializedName("payoutAmount") val payoutAmount: Int = 350,
    @SerializedName("distanceKm") val distanceKm: Double = 2.4,
    @SerializedName("urgency") val urgency: String? = "URGENT",
    @SerializedName("remainingSeconds") val remainingSeconds: Int = 90
)

data class AcceptBookingResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("booking") val booking: BookingDetail? = null
)

data class BookingDetail(
    @SerializedName("bookingId") val bookingId: String,
    @SerializedName("customerName") val customerName: String? = "Customer",
    @SerializedName("customerPhone") val customerPhone: String? = "",
    @SerializedName("address") val address: String? = "",
    @SerializedName("locality") val locality: String? = "Bhilai",
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
    @SerializedName("serviceName") val serviceName: String? = "Electrical Service",
    @SerializedName("issueDescription") val issueDescription: String? = "",
    @SerializedName("scheduledDate") val scheduledDate: String? = "Today",
    @SerializedName("scheduledTime") val scheduledTime: String? = "Now",
    @SerializedName("bookingStatus") val bookingStatus: String? = "TECHNICIAN_ASSIGNED",
    @SerializedName("totalAmount") val totalAmount: Int = 399,
    @SerializedName("partsAmount") val partsAmount: Int = 0,
    @SerializedName("finalAmount") val finalAmount: Int = 399,
    @SerializedName("paymentStatus") val paymentStatus: String? = "PENDING_COLLECTION",
    @SerializedName("startOtp") val startOtp: String? = "1234"
)

data class ActiveBookingsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("bookings") val bookings: List<BookingDetail>? = emptyList()
)

data class SingleBookingResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("booking") val booking: BookingDetail? = null
)

data class UpdateStatusRequest(
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("partsCost") val partsCost: Int? = null,
    @SerializedName("otp") val otp: String? = null
)

data class EarningsSummaryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("todayEarnings") val todayEarnings: Int = 0,
    @SerializedName("weekEarnings") val weekEarnings: Int = 0,
    @SerializedName("totalEarnings") val totalEarnings: Int = 0,
    @SerializedName("totalCashCollected") val totalCashCollected: Int = 0,
    @SerializedName("payableToAdmin") val payableToAdmin: Int = 0,
    @SerializedName("jobsToday") val jobsToday: Int = 0,
    @SerializedName("jobsTotal") val jobsTotal: Int = 0,
    @SerializedName("transactions") val transactions: List<EarningTransaction>? = emptyList()
)

data class EarningTransaction(
    @SerializedName("bookingId") val bookingId: String? = null,
    @SerializedName("amount") val amount: Int = 0,
    @SerializedName("serviceName") val serviceName: String? = "Electrical Service",
    @SerializedName("timestamp") val timestamp: String? = null
)
