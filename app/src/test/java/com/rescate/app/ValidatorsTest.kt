package com.rescate.app

import com.rescate.app.ui.auth.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun correoValidoNoDaError() {
        assertNull(Validators.email("vecino@rescate.pe"))
    }

    @Test
    fun correoVacioPideIngresarlo() {
        assertEquals("Ingresa tu correo electrónico.", Validators.email(""))
    }

    @Test
    fun correoSinArrobaEsInvalido() {
        assertEquals("El correo no es válido.", Validators.email("vecino.rescate.pe"))
    }

    @Test
    fun celularDebeTener9DigitosYEmpezarCon9() {
        assertNull(Validators.phone("912345678"))
        assertNotNull(Validators.phone("12345678"))
        assertNotNull(Validators.phone("812345678"))
    }

    @Test
    fun contrasenaCortaOSinNumerosEsRechazada() {
        assertNotNull(Validators.password("abc12"))
        assertNotNull(Validators.password("solamenteletras"))
        assertNull(Validators.password("rescate2026"))
    }

    @Test
    fun seguridadDeContrasenaAumentaConComplejidad() {
        val debil = Validators.strength("abc")
        val fuerte = Validators.strength("Rescate#2026segura")
        assertTrue(fuerte > debil)
        assertTrue(fuerte <= 100)
    }
}
