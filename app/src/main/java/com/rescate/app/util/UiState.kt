package com.rescate.app.util

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescate.app.data.remote.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

 
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

 
fun <T> ViewModel.launchState(target: MutableLiveData<UiState<T>?>, block: suspend () -> T): Job {
    target.value = UiState.Loading
    return viewModelScope.launch {
        target.value = try {
            UiState.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiState.Error(e.toUserMessage())
        }
    }
}

 
class Event<out T>(private val content: T) {
    private var handled = false
    fun getIfNotHandled(): T? = if (handled) null else { handled = true; content }
}
