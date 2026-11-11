package com.rescate.app.data.remote

import android.content.Context
import com.rescate.app.BuildConfig
import com.rescate.app.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    private var service: ApiService? = null

    fun get(context: Context): ApiService = service ?: synchronized(this) {
        service ?: build(context.applicationContext).also { service = it }
    }

    private fun build(context: Context): ApiService {
        val session = SessionManager.get(context)

         
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                session.token?.let { header("Authorization", "Bearer $it") }
                header("Accept", "application/json")
            }.build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

 
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> serverMessage() ?: when (code()) {
        401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
        409 -> "Alguien se adelantó: ya no queda stock de este pack."
        410 -> "El tiempo de reserva terminó y el pack se liberó."
        422 -> "Revisa los datos ingresados e inténtalo de nuevo."
        in 500..599 -> "El servidor no responde. Intenta en unos minutos."
        else -> "Ocurrió un error (${code()})."
    }
    is IOException -> "Sin conexión. Revisa tu internet."
    is AppException -> message ?: "Ocurrió un error."
    else -> message ?: "Ocurrió un error inesperado."
}

 
class AppException(message: String) : Exception(message)

 
private fun HttpException.serverMessage(): String? {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull() ?: return null
    val message = runCatching {
        com.google.gson.JsonParser.parseString(body).asJsonObject.get("message")?.asString
    }.getOrNull()
    return message?.takeIf { it.isNotBlank() }
}
