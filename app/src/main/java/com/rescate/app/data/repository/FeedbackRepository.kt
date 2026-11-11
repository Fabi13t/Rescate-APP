package com.rescate.app.data.repository

import android.content.Context
import com.rescate.app.BuildConfig
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.model.Complaint
import com.rescate.app.data.model.Rating
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.data.remote.ApiClient
import com.rescate.app.data.remote.AppException
import com.rescate.app.data.remote.ComplaintRequest
import com.rescate.app.data.remote.RatingRequest
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale





class FeedbackRepository(context: Context) {

    private val api by lazy { ApiClient.get(context) }
    private val shortDate = SimpleDateFormat("dd MMM", Locale("es", "PE"))

     

    suspend fun createComplaint(
        reservation: Reservation?, storeId: Long?, storeName: String, type: String, goodType: String,
        amount: Double?, detail: String, request: String
    ): Complaint {
        if (detail.length < 10) throw AppException("Describe lo ocurrido (mínimo 10 caracteres).")
        if (request.length < 5) throw AppException("Indica qué solicitas al comercio.")
        if (!BuildConfig.DEMO_MODE) {
            return api.createComplaint(ComplaintRequest(reservation?.id, storeId, type, goodType, amount, detail, request))
        }
        delay(1100)
        val complaint = Complaint(
            DemoData.nextId(), DemoData.nextComplaintCode(), type, goodType, amount,
            reservation?.pack?.store?.name ?: storeName, "Eduardo Palomino", reservation?.code,
            detail, request, "PENDIENTE", null, "Hoy", null, dueDate()
        )
        DemoData.complaints.add(0, complaint)
        return complaint
    }

    suspend fun myComplaints(): List<Complaint> {
        if (!BuildConfig.DEMO_MODE) return api.myComplaints()
        delay(500)
        return DemoData.complaints.filter { it.customerName == "Eduardo Palomino" }
    }

    suspend fun merchantComplaints(): List<Complaint> {
        if (!BuildConfig.DEMO_MODE) return api.merchantComplaints()
        delay(500)
        return DemoData.complaints.filter { it.storeName == DemoData.merchantStore.name }
            .sortedByDescending { it.isOpen }
    }

    suspend fun respondComplaint(complaint: Complaint, response: String): Complaint {
        if (response.length < 10) throw AppException("Escribe una respuesta (mínimo 10 caracteres).")
        if (!BuildConfig.DEMO_MODE) return api.respondComplaint(complaint.id, mapOf("response" to response))
        delay(800)
        val live = DemoData.complaints.first { it.id == complaint.id }
        live.response = response
        live.status = "RESPONDIDO"
        live.respondedAt = "Hoy"
        return live
    }

     
    private fun dueDate(): String {
        val cal = Calendar.getInstance()
        var added = 0
        while (added < 15) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val d = cal.get(Calendar.DAY_OF_WEEK)
            if (d != Calendar.SATURDAY && d != Calendar.SUNDAY) added++
        }
        return shortDate.format(cal.time)
    }

     

    suspend fun rate(reservation: Reservation, stars: Int, comment: String?): Rating {
        if (stars !in 1..5) throw AppException("Elige de 1 a 5 estrellas.")
        if (!BuildConfig.DEMO_MODE) return api.rate(RatingRequest(reservation.id, stars, comment))
        delay(900)
        if (reservation.status != ReservationStatus.RECOGIDA) throw AppException("Solo puedes calificar packs que ya recogiste.")
        if (DemoData.ratings.any { it.reservationId == reservation.id }) throw AppException("Ya calificaste esta reserva.")
        val rating = Rating(DemoData.nextId(), reservation.id, reservation.pack.store.name, "Eduardo P.", stars,
            comment.orEmpty(), null, "Hoy")
        DemoData.ratings.add(0, rating)
        DemoData.reservations.firstOrNull { it.id == reservation.id }?.myRating = stars
        return rating
    }

    suspend fun storeRatings(storeId: Long, storeName: String): List<Rating> {
        if (!BuildConfig.DEMO_MODE) return api.storeRatings(storeId)
        delay(300)
        return DemoData.ratings.filter { it.storeName == storeName }
    }

    suspend fun merchantRatings(): List<Rating> {
        if (!BuildConfig.DEMO_MODE) return api.merchantRatings()
        delay(500)
        return DemoData.ratings.filter { it.storeName == DemoData.merchantStore.name }
    }

    suspend fun replyRating(rating: Rating, reply: String): Rating {
        if (reply.isBlank()) throw AppException("Escribe una respuesta.")
        if (!BuildConfig.DEMO_MODE) return api.replyRating(rating.id, mapOf("reply" to reply))
        delay(600)
        val live = DemoData.ratings.first { it.id == rating.id }
        live.merchantReply = reply
        return live
    }
}
