package com.rescate.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rescate.app.BuildConfig
import com.rescate.app.R
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.local.SessionManager
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.databinding.FragmentProfileBinding
import com.rescate.app.ui.auth.AuthActivity
import com.rescate.app.util.staggerIn
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        val nav = findNavController()
        binding.rowEdit.setOnClickListener { nav.navigate(R.id.toEditProfile) }
        binding.rowFavorites.setOnClickListener { nav.navigate(R.id.toFavorites, bundleOf("favorites" to true)) }
        binding.rowDonate.setOnClickListener { nav.navigate(R.id.toDonate) }
        binding.rowDonations.setOnClickListener { nav.navigate(R.id.toMyDonations) }
        binding.rowSettings.setOnClickListener { nav.navigate(R.id.toSettings) }
        binding.rowHelp.setOnClickListener { nav.navigate(R.id.toHelp) }
        binding.rowComplaints.setOnClickListener { nav.navigate(R.id.toComplaints) }
        binding.rowPrivacy.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_shield)
                .setTitle("Privacidad y seguridad")
                .setMessage("• Tu sesión se protege con un token JWT.\n• Solo compartimos tu nombre con el comercio donde reservas.\n• Tu ubicación se usa para mostrar packs cercanos y no se guarda en el servidor.\n• Puedes solicitar la eliminación de tu cuenta escribiendo a soporte.")
                .setPositiveButton("Entendido", null)
                .show()
        }
        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Cerrar sesión?")
                .setMessage("Tendrás que volver a ingresar tus datos.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar sesión") { _, _ -> logout() }
                .show()
        }
        binding.tvVersion.text = "Rescate v${BuildConfig.VERSION_NAME}" + if (BuildConfig.DEMO_MODE) " · modo demo" else ""

        if (savedInstanceState == null) staggerIn(binding.headerCard, binding.statsRow, binding.menuCard, binding.menuCard2, binding.btnLogout)
    }

    override fun onResume() {
        super.onResume()
        val user = SessionManager.get(requireContext()).user ?: return
        binding.tvAvatar.text = user.initials
        binding.tvName.text = user.fullName
        binding.tvEmail.text = user.email
        binding.tvDistrict.text = user.district
        binding.tvStatReservations.text = DemoData.reservations.count { it.status != ReservationStatus.RETENIDA }.toString()
        val kg = 14.0 + DemoData.reservations.filter { it.status == ReservationStatus.RECOGIDA }.sumOf { it.kg }
        binding.tvStatKg.text = String.format(Locale.US, "%.1f", kg)
        binding.tvStatDonations.text = DemoData.donations.size.toString()
    }

    private fun logout() {
        SessionManager.get(requireContext()).logout()
        startActivity(Intent(requireContext(), AuthActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
