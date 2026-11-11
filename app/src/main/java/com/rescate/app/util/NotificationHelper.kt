package com.rescate.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rescate.app.R
import com.rescate.app.data.local.SessionManager
import com.rescate.app.ui.main.MainActivity





object NotificationHelper {

    const val CH_PACKS = "packs_nuevos"
    const val CH_ALERTS = "alertas_stock"
    const val CH_REMINDERS = "recordatorios"
    const val CH_OFFERS = "ofertas"
    const val CH_IMPACT = "impacto"

    val channels = listOf(
        Triple(CH_PACKS, "Packs nuevos cerca", "Cuando un comercio cercano publica un pack"),
        Triple(CH_ALERTS, "Alertas de stock", "Cuando se libera un pack que estabas esperando"),
        Triple(CH_REMINDERS, "Recordatorios de recojo", "Antes de que termine tu horario de recojo"),
        Triple(CH_OFFERS, "Ofertas y novedades", "Promociones de comercios de tu zona"),
        Triple(CH_IMPACT, "Resumen de impacto", "Tu impacto semanal: kilos y CO₂ evitado")
    )

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        channels.forEach { (id, name, desc) ->
            val importance = if (id == CH_ALERTS || id == CH_REMINDERS)
                NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            nm.createNotificationChannel(NotificationChannel(id, name, importance).apply { description = desc })
        }
    }

    fun show(context: Context, channelId: String, title: String, body: String) {
        if (!SessionManager.get(context).isChannelEnabled(channelId)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_eco)
            .setColor(ContextCompat.getColor(context, R.color.green_800))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }
}
