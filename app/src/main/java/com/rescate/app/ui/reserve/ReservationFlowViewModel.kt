package com.rescate.app.ui.reserve

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rescate.app.data.model.Pack
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.data.repository.ReservationRepository
import com.rescate.app.util.NotificationHelper
import com.rescate.app.util.UiState
import com.rescate.app.util.launchState
import kotlinx.coroutines.launch





class ReservationFlowViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ReservationRepository(app)

    var pack: Pack? = null
        private set
    val quantity = MutableLiveData(1)
    val hold = MutableLiveData<UiState<Reservation>?>()
    val payment = MutableLiveData<UiState<Reservation>?>()
    val secondsLeft = MutableLiveData(HOLD_SECONDS)
    val expired = MutableLiveData(false)

    var reservation: Reservation? = null
        private set
    private var timer: CountDownTimer? = null

    fun start(pack: Pack) {
        if (this.pack?.id != pack.id || reservation != null) {
            cancelTimer()
            reservation = null
            hold.value = null
            payment.value = null
            expired.value = false
            secondsLeft.value = HOLD_SECONDS
            quantity.value = 1
        }
        this.pack = pack
    }

    fun changeQuantity(delta: Int) {
        val p = pack ?: return
        val next = ((quantity.value ?: 1) + delta).coerceIn(1, p.stock.coerceAtLeast(1))
        quantity.value = next
    }

    val total: Double get() = (pack?.rescuePrice ?: 0.0) * (quantity.value ?: 1)

     
    fun holdStock() {
        val p = pack ?: return
        launchState(hold) {
            repo.hold(p, quantity.value ?: 1).also {
                reservation = it
                startTimer()
            }
        }
    }

    private fun startTimer() {
        cancelTimer()
        expired.postValue(false)
        timer = object : CountDownTimer(HOLD_SECONDS * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                secondsLeft.value = ((ms + 999) / 1000).toInt()
            }

            override fun onFinish() {
                secondsLeft.value = 0
                onExpired()
            }
        }.start()
    }

    private fun onExpired() {
        val r = reservation ?: return
        if (r.status != ReservationStatus.RETENIDA) return
        expired.value = true
        viewModelScope.launch {
            runCatching { repo.release(r, ReservationStatus.CANCELADA) }
             
            NotificationHelper.show(getApplication(), NotificationHelper.CH_ALERTS,
                "Un pack se liberó", "${r.pack.title} de ${r.pack.store.name} vuelve a estar disponible.")
        }
    }

    fun cancelHold(onDone: () -> Unit = {}) {
        cancelTimer()
        val r = reservation ?: return onDone()
        viewModelScope.launch {
            runCatching { repo.release(r) }
            reservation = null
            onDone()
        }
    }

     
    fun reportYape(reference: String) {
        val r = reservation ?: return
        launchState(payment) {
            val reported = repo.reportYape(r, reference)
            cancelTimer()  
            val result = repo.awaitMerchantDecision(reported)
            reservation = result
            if (result.status == ReservationStatus.CONFIRMADA) {
                NotificationHelper.show(getApplication(), NotificationHelper.CH_REMINDERS,
                    "¡Pago confirmado!", "Recoge tu ${result.pack.title} en ${result.pack.store.name} hasta las ${result.pack.pickupUntil}.")
            }
            result
        }
    }

    private fun cancelTimer() {
        timer?.cancel()
        timer = null
    }

    override fun onCleared() {
        cancelTimer()
        super.onCleared()
    }

    companion object {
        const val HOLD_SECONDS = 180
    }
}
