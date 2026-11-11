package com.rescate.app.data.repository

import android.content.Context
import com.rescate.app.BuildConfig
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.model.Donation
import com.rescate.app.data.remote.ApiClient
import com.rescate.app.data.remote.AppException
import com.rescate.app.data.remote.DonationRequest
import kotlinx.coroutines.delay

class DonationRepository(context: Context) {

    private val api by lazy { ApiClient.get(context) }

    suspend fun publish(request: DonationRequest): Donation {
         
        if (request.expiresInDays > 7) throw AppException("La caducidad máxima permitida es de 7 días.")
        if (!BuildConfig.DEMO_MODE) return api.donate(request)
        delay(1000)
        val donation = Donation(DemoData.nextId(), request.title, request.foodType, request.kg,
            request.expiresInDays, request.district, request.notes, "Publicada", "Hoy")
        DemoData.donations.add(0, donation)
        return donation
    }

    suspend fun myDonations(): List<Donation> {
        if (!BuildConfig.DEMO_MODE) return api.myDonations()
        delay(500)
        return DemoData.donations.toList()
    }

    suspend fun withdraw(donation: Donation) {
        if (!BuildConfig.DEMO_MODE) { api.withdrawDonation(donation.id); return }
        delay(500)
        DemoData.donations.firstOrNull { it.id == donation.id }?.status = "Retirada"
    }
}
