package com.rescate.app

import com.rescate.app.util.starsText
import org.junit.Assert.assertEquals
import org.junit.Test

class StarsTest {

    @Test
    fun estrellasLlenasYVacias() {
        assertEquals("★★★★☆", starsText(4))
        assertEquals("★★★★★", starsText(5))
    }

    @Test
    fun puntuacionFueraDeRangoSeAjusta() {
        assertEquals("☆☆☆☆☆", starsText(0))
        assertEquals("★★★★★", starsText(9))
    }
}
