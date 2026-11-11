package com.rescate.app.data.repository

import android.content.Context
import android.util.Patterns
import com.rescate.app.BuildConfig
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.local.SessionManager
import com.rescate.app.data.model.Role
import com.rescate.app.data.model.User
import com.rescate.app.data.remote.ApiClient
import com.rescate.app.data.remote.AppException
import com.rescate.app.data.remote.LoginRequest
import com.rescate.app.data.remote.RegisterRequest
import kotlinx.coroutines.delay

class AuthRepository(context: Context) {

    private val session = SessionManager.get(context)
    private val api by lazy { ApiClient.get(context) }

    suspend fun login(email: String, password: String, role: Role): User {
        if (BuildConfig.DEMO_MODE) {
            delay(1100)
             
            if (password == "error" || password.length < 6) throw AppException("Correo o contraseña incorrectos.")
            val user = if (role == Role.COMERCIO) {
                User(2, "Pedro", "Quispe", email, "987654321", "Los Olivos", Role.COMERCIO,
                    DemoData.merchantStore.name, DemoData.merchantStore.yapeNumber)
            } else {
                User(1, "Eduardo", "Palomino", email, "912345678", "Los Olivos", Role.VECINO)
            }
            session.token = "demo-jwt-token"
            session.user = user
            return user
        }
        val res = api.login(LoginRequest(email, password))
        session.token = res.token
        session.user = res.user
        return res.user
    }

    suspend fun register(request: RegisterRequest): User {
        if (BuildConfig.DEMO_MODE) {
            delay(1200)
            val role = Role.valueOf(request.role)
            val user = User(
                DemoData.nextId(), request.names, request.lastNames, request.email, request.phone,
                request.district, role, request.storeName, request.yapeNumber,
                verified = role == Role.VECINO
            )
            session.token = "demo-jwt-token"
            session.user = user
            return user
        }
        val res = api.register(request)
        session.token = res.token
        session.user = res.user
        return res.user
    }

    suspend fun recoverPassword(email: String) {
        if (BuildConfig.DEMO_MODE) { delay(900); return }
        api.recoverPassword(mapOf("email" to email))
    }

    suspend fun verifyCode(code: String) {
        delay(700)
        if (code != "123456" && code.length != 6) throw AppException("El código no es válido.")
    }

    suspend fun updateProfile(user: User): User {
        delay(900)
        if (!Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) throw AppException("Ingresa un correo válido.")
        session.user = user
        return user
    }

    fun currentUser(): User? = session.user

    fun logout() = session.logout()
}
