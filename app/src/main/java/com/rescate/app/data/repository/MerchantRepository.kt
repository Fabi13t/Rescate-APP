package com.rescate.app.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.rescate.app.BuildConfig
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.model.Category
import com.rescate.app.data.model.Pack
import com.rescate.app.data.model.PackStatus
import com.rescate.app.data.model.Payment
import com.rescate.app.data.model.PaymentStatus
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.data.remote.ApiClient
import com.rescate.app.data.remote.AppException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MerchantRepository(context: Context) {

    private val api by lazy { ApiClient.get(context) }
    private val appContext = context.applicationContext
    private val store = DemoData.merchantStore

    suspend fun myPacks(): List<Pack> {
        if (!BuildConfig.DEMO_MODE) return api.merchantPacks()
        delay(600)
        return DemoData.packs.filter { it.store.id == store.id }
    }

    suspend fun publish(
        title: String, description: String, normalPrice: Double, rescuePrice: Double,
        quantity: Int, pickupUntil: String, perishable: Boolean, photoUri: String?
    ): Pack {
        if (rescuePrice >= normalPrice) throw AppException("El precio rescate debe ser menor al precio real.")
        if (quantity <= 0) throw AppException("La cantidad debe ser mayor a 0.")
        if (!BuildConfig.DEMO_MODE) {
             
            val json = Gson().toJson(mapOf(
                "title" to title, "description" to description, "normalPrice" to normalPrice,
                "rescuePrice" to rescuePrice, "quantity" to quantity, "pickupUntil" to pickupUntil,
                "perishable" to perishable
            ))
            var photoPart: MultipartBody.Part? = null
            if (photoUri != null) {
                val bytes: ByteArray? = withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream -> stream.readBytes() }
                }
                if (bytes != null) {
                    photoPart = MultipartBody.Part.createFormData("photo", "pack.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
                }
            }
            return api.publishPack(json.toRequestBody("application/json".toMediaType()), photoPart)
        }
        delay(1300)
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val pack = Pack(DemoData.nextId(), store, title, description, Category.PANADERIA, normalPrice, rescuePrice,
            quantity, quantity, pickupUntil, if (perishable) 0 else 7, perishable, 1.0, 350, today,
            PackStatus.EN_PROCESO, photoUri)
        DemoData.packs.add(0, pack)
        return pack
    }

    suspend fun orders(): List<Reservation> {
        if (!BuildConfig.DEMO_MODE) return api.merchantOrders()
        delay(600)
        return (DemoData.merchantOrders + DemoData.reservations)
            .filter { it.pack.store.id == store.id && it.status != ReservationStatus.RETENIDA }
            .distinctBy { it.id }
    }

    suspend fun payments(): List<Payment> {
        if (!BuildConfig.DEMO_MODE) return api.merchantPayments()
        delay(600)
        return DemoData.payments.toList()
    }

    suspend fun approve(payment: Payment): Payment {
        if (!BuildConfig.DEMO_MODE) return api.approvePayment(payment.id)
        delay(900)
        val live = DemoData.payments.first { it.id == payment.id }
        live.status = PaymentStatus.CONFIRMADO
        findReservation(live.reservationCode)?.status = ReservationStatus.CONFIRMADA
        return live
    }

    suspend fun reject(payment: Payment): Payment {
        if (!BuildConfig.DEMO_MODE) return api.rejectPayment(payment.id)
        delay(900)
        val live = DemoData.payments.first { it.id == payment.id }
        live.status = PaymentStatus.RECHAZADO
        findReservation(live.reservationCode)?.let { r ->
            if (r.status.isActive) DemoData.packs.firstOrNull { it.id == r.pack.id }?.let { it.stock += r.quantity }
            r.status = ReservationStatus.RECHAZADA
        }
        return live
    }

     
    suspend fun markPickedUp(code: String): Reservation {
        if (!BuildConfig.DEMO_MODE) return api.markPickedUp(code)
        delay(700)
        val r = findReservation(code) ?: throw AppException("No se encontró la reserva $code.")
        if (r.status != ReservationStatus.CONFIRMADA) throw AppException("La reserva está en estado: ${r.status.label}.")
        r.status = ReservationStatus.RECOGIDA
        return r
    }

    suspend fun updateYape(phone: String) {
        if (!BuildConfig.DEMO_MODE) api.updateYape(mapOf("phone" to phone))
    }

    private fun findReservation(code: String): Reservation? =
        (DemoData.reservations + DemoData.merchantOrders).firstOrNull { it.code == code }
}
