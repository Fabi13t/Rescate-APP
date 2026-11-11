package com.rescate.app

import com.rescate.app.data.model.Category
import com.rescate.app.data.model.Complaint
import com.rescate.app.data.model.Pack
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.data.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    private val store = Store(1, "Panadería Don Pedro", Category.PANADERIA, "Av. Los Olivos 234", "Los Olivos",
        -11.99, -77.07, 4.6, "21:00", "987 654 321")

    private fun pack(normal: Double = 15.0, rescue: Double = 6.0, days: Int = 0, stock: Int = 4) =
        Pack(1, store, "Pack sorpresa", "Panes del día", Category.PANADERIA, normal, rescue, stock, 6,
            "20:30", days, true, 1.2, 350, "09/10/2026")

    @Test
    fun descuentoSeCalculaSobreElPrecioOriginal() {
        assertEquals(60, pack(15.0, 6.0).discountPercent)
    }

    @Test
    fun etiquetaDeVencimiento() {
        assertEquals("Vence hoy", pack(days = 0).expiresLabel)
        assertEquals("Vence mañana", pack(days = 1).expiresLabel)
        assertEquals("Vence en 3 días", pack(days = 3).expiresLabel)
    }

    @Test
    fun totalYKilosDeLaReservaDependenDeLaCantidad() {
        val r = Reservation(1, "RSC-000001", pack(), 2, ReservationStatus.RETENIDA, "Hoy")
        assertEquals(12.0, r.total, 0.001)
        assertEquals(2.4, r.kg, 0.001)
    }

    @Test
    fun soloRetenidaEnRevisionYConfirmadaSonActivas() {
        assertTrue(ReservationStatus.RETENIDA.isActive)
        assertTrue(ReservationStatus.PAGO_EN_REVISION.isActive)
        assertTrue(ReservationStatus.CONFIRMADA.isActive)
        assertFalse(ReservationStatus.RECOGIDA.isActive)
        assertFalse(ReservationStatus.CANCELADA.isActive)
    }

    @Test
    fun reclamoPendienteEstaAbierto() {
        val c = Complaint(1, "REC-2026-000001", "QUEJA", "SERVICIO", null, "Cafetería Aroma", "Eduardo",
            null, "Demora de 20 minutos en el recojo", "Mejorar la atención", "PENDIENTE",
            createdAt = "Hoy", dueDate = "26 nov")
        assertTrue(c.isOpen)
        assertEquals("Queja", c.typeLabel)
        assertEquals("Pendiente", c.statusLabel)
        c.status = "RESPONDIDO"
        assertFalse(c.isOpen)
    }
}
