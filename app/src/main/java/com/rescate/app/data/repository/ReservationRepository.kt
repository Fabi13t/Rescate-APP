package com.rescate.app.data.repository

import android.content.Context
import com.rescate.app.BuildConfig
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.model.Impact
import com.rescate.app.data.model.ImpactPeriod
import com.rescate.app.data.model.Pack
import com.rescate.app.data.model.Payment
import com.rescate.app.data.model.PaymentStatus
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.data.remote.ApiClient
import com.rescate.app.data.remote.AppException
import com.rescate.app.data.remote.ReserveRequest
import com.rescate.app.data.remote.YapeReportRequest
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale




class ReservationRepository(context: Context) {

    private val api by lazy { ApiClient.get(context) }

     
    suspend fun hold(pack: Pack, quantity: Int): Reservation {
        if (!BuildConfig.DEMO_MODE) return api.reserve(ReserveRequest(pack.id, quantity))
        delay(900)
        val live = DemoData.packs.first { it.id == pack.id }
        if (live.stock <= 0) throw AppException("Alguien se adelantó: este pack se agotó.")
        if (live.stock < quantity) throw AppException("Solo quedan ${live.stock} packs disponibles.")
        live.stock -= quantity
        val reservation = Reservation(DemoData.nextId(), DemoData.nextCode(), live, quantity,
            ReservationStatus.RETENIDA, "Hoy · " + now())
        DemoData.reservations.add(0, reservation)
        return reservation
    }

     
    suspend fun release(reservation: Reservation, status: ReservationStatus = ReservationStatus.CANCELADA): Reservation {
        if (!BuildConfig.DEMO_MODE) return api.cancel(reservation.id)
        delay(400)
        val live = DemoData.reservations.firstOrNull { it.id == reservation.id } ?: return reservation
        if (live.status.isActive) {
            DemoData.packs.firstOrNull { it.id == live.pack.id }?.let { it.stock += live.quantity }
        }
        live.status = status
        return live
    }

     
    suspend fun reportYape(reservation: Reservation, reference: String): Reservation {
        if (!BuildConfig.DEMO_MODE) return api.reportYape(reservation.id, YapeReportRequest(reference))
        delay(1000)
        val live = DemoData.reservations.first { it.id == reservation.id }
        live.yapeReference = reference
        live.status = ReservationStatus.PAGO_EN_REVISION
        DemoData.payments.add(0, Payment(DemoData.nextId(), live.code, "Eduardo Palomino", live.total,
            live.pack.title, reference, PaymentStatus.PENDIENTE, now()))
        return live
    }

    



    suspend fun awaitMerchantDecision(reservation: Reservation): Reservation {
        if (!BuildConfig.DEMO_MODE) {
            for (attempt in 0 until 24) {
                val r = api.reservation(reservation.id)
                if (r.status != ReservationStatus.PAGO_EN_REVISION) return r
                delay(5000)
            }
            return reservation
        }
        delay(3500)
        val live = DemoData.reservations.first { it.id == reservation.id }
        val payment = DemoData.payments.firstOrNull { it.reservationCode == live.code }
        when (payment?.status) {
            PaymentStatus.RECHAZADO -> live.status = ReservationStatus.RECHAZADA
            else -> {
                payment?.status = PaymentStatus.CONFIRMADO
                live.status = ReservationStatus.CONFIRMADA
            }
        }
        return live
    }

    suspend fun myReservations(): List<Reservation> {
        if (!BuildConfig.DEMO_MODE) return api.myReservations()
        delay(600)
        return DemoData.reservations.filter { it.status != ReservationStatus.RETENIDA }.toList()
    }

    suspend fun cancel(reservation: Reservation): Reservation = release(reservation, ReservationStatus.CANCELADA)

    suspend fun impact(period: String): Impact {
        if (!BuildConfig.DEMO_MODE) return api.impact(period)
        delay(500)
        val picked = DemoData.reservations.filter { it.status == ReservationStatus.RECOGIDA }
        val extraKg = picked.sumOf { it.kg }
        val series = when (period) {
            "semanal" -> listOf("L" to 0.4, "M" to 0.0, "X" to 1.2, "J" to 0.6, "V" to 0.0, "S" to 2.1, "D" to 0.9)
            "mensual" -> listOf("Sem 1" to 1.1, "Sem 2" to 2.4, "Sem 3" to 0.8, "Sem 4" to 3.0)
            "total" -> listOf("2024" to 3.1, "2025" to 9.4, "2026" to 18.6)
            else -> listOf("Ene" to 1.2, "Feb" to 2.8, "Mar" to 2.1, "Abr" to 4.0, "May" to 3.3, "Jun" to 5.2)
        }.map { ImpactPeriod(it.first, it.second) }
        val factor = when (period) { "semanal" -> 0.28; "mensual" -> 0.4; else -> 1.0 }
        val kg = (14.0 + extraKg) * factor
        return Impact(
            kg = kg,
            co2Kg = kg * 0.5,
            savedSoles = kg * 3.97,
            packs = (picked.size + 10).let { (it * factor).toInt().coerceAtLeast(1) },
            stores = 5,
            series = series
        )
    }

    private fun now(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
