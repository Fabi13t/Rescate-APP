package com.rescate.app.data.demo

import com.rescate.app.data.model.AppNotification
import com.rescate.app.data.model.Category
import com.rescate.app.data.model.Complaint
import com.rescate.app.data.model.Donation
import com.rescate.app.data.model.NotificationType
import com.rescate.app.data.model.Pack
import com.rescate.app.data.model.PackStatus
import com.rescate.app.data.model.Payment
import com.rescate.app.data.model.PaymentStatus
import com.rescate.app.data.model.Rating
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.data.model.Store






object DemoData {

    val donPedro = Store(1, "Panadería Don Pedro", Category.PANADERIA, "Av. Los Olivos 234", "Los Olivos", -11.9915, -77.0705, 4.6, "21:00", "987 654 321")
    val frugal = Store(2, "Mercado Frugal", Category.ABARROTES, "Jr. Las Palmeras 512", "Los Olivos", -11.9862, -77.0651, 4.4, "20:00", "986 321 457")
    val aroma = Store(3, "Cafetería Aroma", Category.PREPARADOS, "Av. Carlos Izaguirre 980", "Los Olivos", -11.9958, -77.0612, 4.8, "22:00", "985 111 222")
    val esquina = Store(4, "Frutería La Esquina", Category.ABARROTES, "Calle Los Pinos 145", "San Martín de Porres", -12.0003, -77.0742, 4.3, "21:00", "984 222 333")
    val elSol = Store(5, "Minimarket El Sol", Category.ABARROTES, "Av. Naranjal 1201", "Independencia", -11.9811, -77.0590, 4.2, "22:00", "983 444 555")

    val stores = listOf(donPedro, frugal, aroma, esquina, elSol)

     
    val merchantStore = donPedro

    val packs = mutableListOf(
        Pack(1, donPedro, "Pack sorpresa", "Selección de panes y bollería del día, próximos a vencer y en perfectas condiciones.", Category.PANADERIA, 15.0, 6.0, 4, 6, "20:30", 0, true, 1.2, 350, "09/10/2026"),
        Pack(2, frugal, "Verduras variadas", "Verduras frescas de estación: tomate, zanahoria, zapallo y hojas verdes.", Category.ABARROTES, 20.0, 8.0, 2, 4, "20:00", 2, true, 2.5, 900, "09/10/2026"),
        Pack(3, aroma, "Pack de postres", "Postres y tortas del día: porciones de torta, alfajores y keke.", Category.PREPARADOS, 18.0, 7.0, 3, 5, "21:30", 0, true, 0.8, 1300, "09/10/2026"),
        Pack(4, esquina, "Frutas de temporada", "Mix de frutas maduras ideales para jugos o postres.", Category.ABARROTES, 16.0, 6.5, 5, 6, "20:30", 1, true, 3.0, 1600, "09/10/2026"),
        Pack(5, donPedro, "Pack panes dulces", "Panes dulces, empanadas y croissants del día.", Category.PANADERIA, 12.0, 5.0, 1, 3, "20:30", 0, true, 0.9, 350, "09/10/2026"),
        Pack(6, aroma, "Almuerzo del día", "Menú del día listo para calentar: entrada y segundo.", Category.PREPARADOS, 22.0, 9.0, 0, 4, "15:00", 0, true, 1.0, 1300, "09/10/2026"),
        Pack(7, elSol, "Abarrotes no perecibles", "Arroz, menestras y fideos con empaque dañado pero producto intacto.", Category.ABARROTES, 25.0, 10.0, 6, 8, "21:00", 5, false, 2.0, 2100, "08/10/2026"),
        Pack(8, donPedro, "Pack panes", "Pan francés, ciabatta e integral del día.", Category.PANADERIA, 10.0, 4.0, 0, 2, "20:30", 0, true, 1.0, 350, "01/10/2026", PackStatus.FINALIZADO)
    )

    val reservations = mutableListOf(
        Reservation(101, "RSC-DEMO-0847", packs[0], 1, ReservationStatus.CONFIRMADA, "Hoy · 19:40", "5321"),
        Reservation(102, "RSC-DEMO-0812", packs[3], 1, ReservationStatus.CONFIRMADA, "Hoy · 18:15", "4410"),
        Reservation(103, "RSC-DEMO-0733", packs[6], 2, ReservationStatus.RECOGIDA, "28 oct · 20:05", "1984"),
        Reservation(104, "RSC-DEMO-0690", packs[0], 1, ReservationStatus.RECOGIDA, "21 oct · 19:30", "7731", myRating = 5),
        Reservation(105, "RSC-DEMO-0611", packs[1], 1, ReservationStatus.NO_RECOGIDA, "15 oct · 18:50", "2290")
    )

     
    val merchantOrders = mutableListOf(
        Reservation(201, "RSC-DEMO-0901", packs[0], 1, ReservationStatus.PAGO_EN_REVISION, "Hoy · 19:52", "DEMO-00001", "Julio Mendoza"),
        Reservation(202, "RSC-DEMO-0899", packs[4], 1, ReservationStatus.CONFIRMADA, "Hoy · 19:20", "DEMO-00002", "Diana Gómez"),
        Reservation(203, "RSC-DEMO-0870", packs[0], 1, ReservationStatus.RECOGIDA, "Ayer · 20:10", "DEMO-00003", "Laura López")
    )

    val payments = mutableListOf(
        Payment(301, "RSC-DEMO-0901", "Julio Mendoza", 6.0, "Pack sorpresa", "DEMO-00001", PaymentStatus.PENDIENTE, "19:52"),
        Payment(302, "RSC-DEMO-0899", "Diana Gómez", 5.0, "Pack panes dulces", "DEMO-00002", PaymentStatus.CONFIRMADO, "19:20"),
        Payment(303, "RSC-DEMO-0870", "Laura López", 6.0, "Pack sorpresa", "DEMO-00003", PaymentStatus.CONFIRMADO, "Ayer")
    )

    val donations = mutableListOf(
        Donation(401, "Panes del día", "Perecible", 3.0, 2, "Los Olivos", "Listo para recoger después de las 7 p. m.", "Publicada", "02 nov"),
        Donation(402, "Arroz y menestras", "No perecible", 10.0, 7, "Los Olivos", "Bolsas cerradas.", "Entregada", "20 oct")
    )

    val notifications = mutableListOf(
        AppNotification(1, "¡Pack disponible cerca!", "Panadería Don Pedro publicó un Pack sorpresa a S/ 6.00", "Hace 5 min", NotificationType.PACK),
        AppNotification(2, "Un pack se liberó", "Se liberó 1 unidad de Verduras variadas en Mercado Frugal", "Hace 20 min", NotificationType.ALERTA),
        AppNotification(3, "Recuerda tu recojo", "Tu reserva RSC-DEMO-0847 se recoge hasta las 20:30", "Hace 1 h", NotificationType.RECORDATORIO, true),
        AppNotification(4, "Tu impacto de la semana", "Rescataste 4.2 kg de alimentos. ¡Sigue así!", "Ayer", NotificationType.IMPACTO, true)
    )

     
    val ratings = mutableListOf(
        Rating(501, 104, donPedro.name, "Eduardo P.", 5, "El pan llegó fresquito, ¡excelente!", "¡Gracias Eduardo, te esperamos pronto!", "21 oct"),
        Rating(502, 203, donPedro.name, "Laura L.", 5, "Todo muy rico y bien empacado.", null, "Ayer"),
        Rating(503, 299, donPedro.name, "Diana G.", 4, "Muy buena atención, rápido el recojo.", null, "02 nov"),
        Rating(504, 298, aroma.name, "Julio M.", 5, "Los postres estaban deliciosos.", null, "30 oct")
    )

     
    val complaints = mutableListOf(
        Complaint(601, "REC-2026-000001", "RECLAMO", "PRODUCTO", 6.0, donPedro.name, "Julio Mendoza", "RSC-DEMO-0901",
            "Dos panes del pack estaban duros, no parecían del día.", "Solicito la reposición de los panes.",
            "PENDIENTE", null, "Ayer", null, "26 nov"),
        Complaint(602, "REC-2026-000002", "QUEJA", "SERVICIO", null, aroma.name, "Eduardo Palomino", null,
            "Tuve que esperar 20 minutos para que me entreguen el pack.", "Mejorar el tiempo de atención en el recojo.",
            "RESPONDIDO", "Lamentamos la demora. Reforzamos el personal en horario de recojo. ¡Gracias por avisarnos!",
            "28 oct", "29 oct", "18 nov")
    )

    private var nextComplaint = 3
    fun nextComplaintCode(): String = "REC-2026-%06d".format(nextComplaint++)

    private var nextId = 1000L
    fun nextId(): Long = nextId++

    private var nextCode = 950
    fun nextCode(): String = "RSC-DEMO-%04d".format(nextCode++)
}
