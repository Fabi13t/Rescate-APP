package com.rescate.app.ui.feedback

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rescate.app.R
import com.rescate.app.data.model.Complaint
import com.rescate.app.data.remote.toUserMessage
import com.rescate.app.data.repository.FeedbackRepository
import com.rescate.app.databinding.FragmentListScreenBinding
import com.rescate.app.util.snack
import com.rescate.app.util.soles
import kotlinx.coroutines.launch

 
class ComplaintsFragment : Fragment(R.layout.fragment_list_screen) {

    private var _binding: FragmentListScreenBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentListScreenBinding.bind(view)
        binding.toolbar.title = "Libro de reclamaciones"
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.fab.isVisible = true
        binding.fab.text = "Nuevo reclamo"
        binding.fab.setOnClickListener { findNavController().navigate(R.id.toComplaintForm) }
        binding.empty.ivEmpty.setImageResource(R.drawable.empty_busqueda)
        binding.empty.tvEmptyTitle.text = "No tienes reclamos"
        binding.empty.tvEmptyBody.text = "Si algo salió mal con un pack o la atención, regístralo aquí."
        binding.empty.btnEmptyAction.isVisible = false
        load()
    }

    private fun load() {
        binding.progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching { FeedbackRepository(requireContext()).myComplaints() }
            val b = _binding ?: return@launch
            b.progress.visibility = View.INVISIBLE
            result.onFailure { snack(it.toUserMessage()) }
            val list = result.getOrDefault(emptyList())
            b.recycler.adapter = ComplaintAdapter(list, merchantMode = false) { showDetail(it) }
            b.recycler.scheduleLayoutAnimation()
            b.empty.root.isVisible = list.isEmpty()
        }
    }

    private fun showDetail(c: Complaint) {
        val message = buildString {
            appendLine("Comercio: ${c.storeName}")
            c.reservationCode?.let { appendLine("Reserva: $it") }
            c.amount?.let { appendLine("Monto reclamado: ${it.soles()}") }
            appendLine("Registrado: ${c.createdAt}")
            appendLine()
            appendLine("Lo que pasó:\n${c.detail}")
            appendLine()
            appendLine("Lo que solicitas:\n${c.request}")
            appendLine()
            append(
                if (c.response != null) "Respuesta del comercio (${c.respondedAt}):\n${c.response}"
                else "Esperando respuesta del comercio (plazo: ${c.dueDate})."
            )
        }
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_assignment)
            .setTitle("${c.typeLabel} ${c.code}")
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
