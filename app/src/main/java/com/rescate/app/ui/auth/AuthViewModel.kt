package com.rescate.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.rescate.app.data.model.Role
import com.rescate.app.data.model.User
import com.rescate.app.data.remote.RegisterRequest
import com.rescate.app.data.repository.AuthRepository
import com.rescate.app.data.repository.PackRepository
import com.rescate.app.util.UiState
import com.rescate.app.util.launchState

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository(app)
    private val packRepo = PackRepository(app)

    val login = MutableLiveData<UiState<User>?>()
    val register = MutableLiveData<UiState<User>?>()
    val recover = MutableLiveData<UiState<Unit>?>()

    fun login(email: String, password: String, role: Role) =
        launchState(login) { repo.login(email, password, role) }

    fun register(request: RegisterRequest) = launchState(register) { repo.register(request) }

    fun sendCode(email: String) = launchState(recover) { repo.recoverPassword(email) }

    fun verifyCode(code: String) = launchState(recover) { repo.verifyCode(code) }

    fun resetPassword() = launchState(recover) { kotlinx.coroutines.delay(900) }

    fun districts(): List<String> = packRepo.districts()
}
