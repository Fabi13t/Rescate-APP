package com.rescate.app

import com.rescate.app.util.distanceLabel
import com.rescate.app.util.kg
import com.rescate.app.util.soles
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun precioEnSolesConDosDecimales() {
        assertEquals("S/ 6.00", 6.0.soles())
        assertEquals("S/ 12.50", 12.5.soles())
    }

    @Test
    fun kilosConUnDecimal() {
        assertEquals("18.6 kg", 18.6.kg())
    }

    @Test
    fun distanciaEnMetrosOKilometros() {
        assertEquals("350 m", 350.distanceLabel())
        assertEquals("1.6 km", 1600.distanceLabel())
    }
}
