package com.rescate.app.util

import java.util.Locale

fun Double.soles(): String = "S/ " + String.format(Locale.US, "%.2f", this)

fun Double.kg(): String = String.format(Locale.US, "%.1f kg", this)

fun Int.distanceLabel(): String = if (this < 1000) "$this m" else String.format(Locale.US, "%.1f km", this / 1000f)

fun starsText(n: Int): String {
    val full = n.coerceIn(0, 5)
    return "★".repeat(full) + "☆".repeat(5 - full)
}
