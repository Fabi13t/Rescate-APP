package com.rescate.app.data.model

import java.io.Serializable

enum class Role { VECINO, COMERCIO }

enum class Category(val label: String) {
    PANADERIA("Panadería"),
    ABARROTES("Abarrotes"),
    PREPARADOS("Preparados")
}

data class User(
    val id: Long,
    val names: String,
    val lastNames: String,
    val email: String,
    val phone: String,
    val district: String,
    val role: Role,
    val storeName: String? = null,
    val yapeNumber: String? = null,
    val verified: Boolean = true
) : Serializable {
    val fullName: String get() = "$names $lastNames".trim()
    val initials: String
        get() = (names.take(1) + lastNames.take(1)).uppercase()
}

data class Store(
    val id: Long,
    val name: String,
    val category: Category,
    val address: String,
    val district: String,
    val lat: Double,
    val lon: Double,
    val rating: Double,
    val closesAt: String,
    val yapeNumber: String,
    val ratingCount: Int = 0
) : Serializable

enum class PackStatus(val label: String) { EN_PROCESO("En proceso"), FINALIZADO("Finalizado") }

data class Pack(
    val id: Long,
    val store: Store,
    val title: String,
    val description: String,
    val category: Category,
    val normalPrice: Double,
    val rescuePrice: Double,
    var stock: Int,
    val initialStock: Int,
    val pickupUntil: String,
    val expiresInDays: Int,
    val perishable: Boolean,
    val kgPerPack: Double,
    var distanceMeters: Int,
    val publishedAt: String,
    var status: PackStatus = PackStatus.EN_PROCESO,
    val photoUri: String? = null
) : Serializable {
    val discountPercent: Int get() = (100 - (rescuePrice / normalPrice * 100)).toInt()
    val expiresLabel: String
        get() = when (expiresInDays) {
            0 -> "Vence hoy"
            1 -> "Vence mañana"
            else -> "Vence en $expiresInDays días"
        }
}

enum class ReservationStatus(val label: String) {
    RETENIDA("Retenida"),
    PAGO_EN_REVISION("Pago en revisión"),
    CONFIRMADA("Confirmada"),
    RECOGIDA("Recogido"),
    CANCELADA("Cancelada"),
    NO_RECOGIDA("No recogido"),
    RECHAZADA("Pago rechazado");

    val isActive: Boolean get() = this == RETENIDA || this == PAGO_EN_REVISION || this == CONFIRMADA
}

data class Reservation(
    val id: Long,
    val code: String,
    val pack: Pack,
    val quantity: Int,
    var status: ReservationStatus,
    val createdAt: String,
    var yapeReference: String? = null,
    val customerName: String = "Tú",
    var myRating: Int? = null
) : Serializable {
    val total: Double get() = pack.rescuePrice * quantity
    val kg: Double get() = pack.kgPerPack * quantity
}

enum class PaymentStatus(val label: String) { PENDIENTE("Pendiente"), CONFIRMADO("Confirmado"), RECHAZADO("Rechazado") }

data class Payment(
    val id: Long,
    val reservationCode: String,
    val customerName: String,
    val amount: Double,
    val product: String,
    val reference: String,
    var status: PaymentStatus,
    val time: String
) : Serializable

data class Donation(
    val id: Long,
    val title: String,
    val foodType: String,
    val kg: Double,
    val expiresInDays: Int,
    val district: String,
    val notes: String,
    var status: String,
    val createdAt: String
) : Serializable

data class ImpactPeriod(val label: String, val kg: Double)

data class Impact(
    val kg: Double,
    val co2Kg: Double,
    val savedSoles: Double,
    val packs: Int,
    val stores: Int,
    val series: List<ImpactPeriod>
)

enum class NotificationType { PACK, ALERTA, RECORDATORIO, OFERTA, IMPACTO }

data class AppNotification(
    val id: Long,
    val title: String,
    val body: String,
    val time: String,
    val type: NotificationType,
    var read: Boolean = false
)

data class SearchFilters(
    val query: String = "",
    val category: Category? = null,
    val maxKm: Float = 5f,
    val maxPrice: Float = 30f,
    val onlyWithStock: Boolean = true
) : Serializable

 
 
data class Complaint(
    val id: Long,
    val code: String,
    val type: String,
    val goodType: String = "PRODUCTO",
    val amount: Double? = null,
    val storeName: String,
    val customerName: String = "",
    val reservationCode: String? = null,
    val detail: String,
    val request: String,
    var status: String,
    var response: String? = null,
    val createdAt: String,
    var respondedAt: String? = null,
    val dueDate: String
) : Serializable {
    val isOpen: Boolean get() = status == "PENDIENTE" || status == "EN_REVISION"
    val typeLabel: String get() = if (type == "QUEJA") "Queja" else "Reclamo"
    val statusLabel: String
        get() = when (status) {
            "PENDIENTE" -> "Pendiente"
            "EN_REVISION" -> "En revisión"
            "RESPONDIDO" -> "Respondido"
            else -> "Cerrado"
        }
}

 
data class Rating(
    val id: Long,
    val reservationId: Long,
    val storeName: String,
    val customerName: String,
    val stars: Int,
    val comment: String,
    var merchantReply: String? = null,
    val createdAt: String
) : Serializable
