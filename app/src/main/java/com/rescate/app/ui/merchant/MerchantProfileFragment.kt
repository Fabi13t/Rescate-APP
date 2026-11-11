package com.rescate.app.ui.merchant

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rescate.app.R
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.local.SessionManager
import com.rescate.app.data.repository.FeedbackRepository
import com.rescate.app.databinding.FragmentMerchantProfileBinding
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import java.util.Locale
import com.rescate.app.ui.auth.AuthActivity
import com.rescate.app.util.AppImages
import com.rescate.app.util.AppImages.showPhoto
import com.rescate.app.util.snack
import com.rescate.app.util.staggerIn

class MerchantProfileFragment : Fragment(R.layout.fragment_merchant_profile) {

    private var _binding: FragmentMerchantProfileBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMerchantProfileBinding.bind(view)
        val user = SessionManager.get(requireContext()).user
        binding.tvStore.text = user?.storeName ?: DemoData.merchantStore.name
        binding.tvOwner.text = "Responsable: ${user?.fullName ?: "—"}"
        AppImages.store(binding.tvStore.text.toString())?.let { binding.ivStorePhoto.showPhoto(it, binding.storeAvatar) }
        binding.tvVerified.text = if (user?.verified == false) "⏳ Por verificar" else "✓ Verificado"

        binding.rowRatings.setOnClickListener {
            findNavController().navigate(R.id.toFeedback, bundleOf("tab" to "ratings"))
        }
        binding.rowComplaints.setOnClickListener {
            findNavController().navigate(R.id.toFeedback, bundleOf("tab" to "complaints"))
        }
        loadFeedbackSummary()
        binding.rowYape.setOnClickListener { PaymentsFragment.editYape(this) { } }
        binding.rowAddress.setOnClickListener {
            val s = DemoData.merchantStore
            info("Dirección y horario", "${s.address}, ${s.district}\nAbierto hasta las ${s.closesAt}")
        }
        binding.rowDocs.setOnClickListener {
            info("Documento", "RUC 20601234567\nEstado: ${if (user?.verified == false) "en revisión por el administrador" else "verificado"}")
        }
        binding.rowHelp.setOnClickListener { snack("Escríbenos a comercios@rescate.pe") }
        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Cerrar sesión?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    SessionManager.get(requireContext()).logout()
                    startActivity(Intent(requireContext(), AuthActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                }
                .show()
        }
        if (savedInstanceState == null) staggerIn(binding.headerCard, binding.menuCard, binding.btnLogout)
    }

     
    private fun loadFeedbackSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            val repo = FeedbackRepository(requireContext())
            val ratings = runCatching { repo.merchantRatings() }.getOrDefault(emptyList())
            val open = runCatching { repo.merchantComplaints() }.getOrDefault(emptyList()).count { it.isOpen }
            val b = _binding ?: return@launch
            if (ratings.isNotEmpty()) {
                b.rowRatings.text = String.format(Locale.US, "Calificaciones · ★ %.1f (%d)", ratings.map { it.stars }.average(), ratings.size)
            }
            b.rowComplaints.text = if (open > 0) "Reclamos recibidos · $open por responder" else "Reclamos recibidos"
        }
    }

    private fun info(title: String, msg: String) {
        MaterialAlertDialogBuilder(requireContext()).setTitle(title).setMessage(msg).setPositiveButton("OK", null).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
