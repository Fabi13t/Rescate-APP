package com.rescate.app

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rescate.app.data.local.SessionManager
import com.rescate.app.ui.auth.AuthActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginFlowTest {

    @Before
    fun limpiarSesion() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SessionManager.get(context).logout()
        SessionManager.get(context).onboardingSeen = true
    }

    @Test
    fun loginSinCorreoMuestraError() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.btnHaveAccount)).perform(click())
            onView(withId(R.id.btnLogin)).perform(click())
            onView(withText("Ingresa tu correo electrónico.")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun loginDemoAbreLaPantallaDeInicio() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.btnHaveAccount)).perform(click())
            onView(withId(R.id.etEmail)).perform(typeText("vecino@rescate.pe"), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(typeText("123456"), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())
            Thread.sleep(2500)
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
        }
    }
}
