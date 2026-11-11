package com.rescate.app.ui.auth

object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun email(value: String): String? = when {
        value.isBlank() -> "Ingresa tu correo electrónico."
        !EMAIL.matches(value) -> "El correo no es válido."
        else -> null
    }

    fun required(value: String, field: String): String? =
        if (value.isBlank()) "Ingresa tus $field." else null

    fun phone(value: String): String? =
        if (value.length != 9 || !value.startsWith("9")) "Ingresa un celular de 9 dígitos." else null

    fun password(value: String): String? = when {
        value.length < 8 -> "Usa al menos 8 caracteres."
        !value.any { it.isDigit() } || !value.any { it.isLetter() } -> "Incluye letras y números."
        else -> null
    }

     
    fun strength(value: String): Int {
        var score = 0
        if (value.length >= 8) score += 30
        if (value.length >= 12) score += 15
        if (value.any { it.isDigit() }) score += 20
        if (value.any { it.isUpperCase() }) score += 15
        if (value.any { !it.isLetterOrDigit() }) score += 20
        return score.coerceAtMost(100)
    }
}
