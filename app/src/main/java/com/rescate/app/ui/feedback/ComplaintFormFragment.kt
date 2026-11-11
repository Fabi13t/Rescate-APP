package com.rescate.app.ui.feedback

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.rescate.app.R
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.data.remote.toUserMessage
import com.rescate.app.data.repository.FeedbackRepository
import com.rescate.app.data.repository.ReservationRepository
import com.rescate.app.databinding.FragmentComplaintFormBinding
import com.rescate.app.util.hideKeyboard
import com.rescate.app.util.pop
import com.rescate.app.util.shake
import com.rescate.app.util.snack
import com.rescate.app.util.soles
import kotlinx.coroutines.launch

 
class ComplaintFormFragment : Fragment(R.layout.fragment_complaint_form) {

    private var _binding: FragmentComplaintFormBinding? = null
    private val binding get() = _binding!!
    private var reservations: List<Reservation> = emptyList()
    private var selected: Reservation? = null
    private var sent = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentComplaintFormBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        selected = arguments?.let { BundleCompat.getSerializable(it, "reservation", Reservation::class.java) }
        loadReservations()

        binding.toggleType.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            val queja = id == R.id.btnQueja
            binding.tvTypeHint.text = if (queja)
                "Queja: no estás conforme con la atención (ej. mal trato, demora en el recojo)."
            else "Reclamo: no estás conforme con el producto (ej. pack en mal estado o incompleto)."
            binding.tilAmount.isVisible = !queja
            binding.btnSend.text = if (queja) "Enviar queja" else "Enviar reclamo"
            binding.tvTypeHint.pop()
        }
        binding.acReservation.setOnItemClickListener { _, _, pos, _ -> selected = reservations[pos]; binding.tilReservation.error = null }
        binding.etDetail.doAfterTextChanged { binding.tilDetail.error = null }
        binding.etRequest.doAfterTextChanged { binding.tilRequest.error = null }
        binding.cbDeclare.setOnCheckedChangeListener { _, checked -> binding.btnSend.isEnabled = checked || sent }
        binding.btnSend.setOnClickListener { if (sent) openList() else send() }
    }

    private fun label(r: Reservation) = "${r.code} · ${r.pack.title} · ${r.pack.store.name}"

    private fun loadReservations() {
        viewLifecycleOwner.lifecycleScope.launch {
            reservations = runCatching { ReservationRepository(requireContext()).myReservations() }.getOrDefault(emptyList())
                .filter { it.status != ReservationStatus.CANCELADA }
            val b = _binding ?: return@launch
            b.acReservation.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, reservations.map(::label)))
            selected?.let { s ->
                reservations.firstOrNull { it.id == s.id }?.let { selected = it }
                b.acReservation.setText(label(s), false)
            }
            if (reservations.isEmpty()) b.tilReservation.helperText = "Aún no tienes reservas para reclamar."
        }
    }

    private fun send() {
        hideKeyboard()
        val detail = binding.etDetail.text?.toString()?.trim().orEmpty()
        val request = binding.etRequest.text?.toString()?.trim().orEmpty()
        val r = selected
        binding.tilReservation.error = if (r == null) "Elige la reserva relacionada." else null
        binding.tilDetail.error = if (detail.length < 10) "Describe lo ocurrido (mínimo 10 caracteres)." else null
        binding.tilRequest.error = if (request.length < 5) "Indica qué solicitas." else null
        val firstError = listOf(binding.tilReservation, binding.tilDetail, binding.tilRequest).firstOrNull { it.error != null }
        if (firstError != null || r == null) { firstError?.shake(); return }

        val queja = binding.toggleType.checkedButtonId == R.id.btnQueja
        val amount = binding.etAmount.text?.toString()?.replace(",", ".")?.toDoubleOrNull()
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                FeedbackRepository(requireContext()).createComplaint(
                    reservation = r, storeId = r.pack.store.id, storeName = r.pack.store.name,
                    type = if (queja) "QUEJA" else "RECLAMO", goodType = if (queja) "SERVICIO" else "PRODUCTO",
                    amount = if (queja) null else amount, detail = detail, request = request
                )
            }.onSuccess { c ->
                setLoading(false)
                val b = _binding ?: return@onSuccess
                sent = true
                b.flipper.displayedChild = 1
                b.toolbar.title = "${c.typeLabel} registrado"
                b.success.tvSuccessTitle.text = "${c.typeLabel} enviado"
                b.success.tvSuccessBody.text = "Guarda tu código de seguimiento."
                b.tvSummary.text = buildString {
                    appendLine("Código: ${c.code}")
                    appendLine("Comercio: ${c.storeName}")
                    c.reservationCode?.let { appendLine("Reserva: $it") }
                    c.amount?.let { appendLine("Monto reclamado: ${it.soles()}") }
                    append("Respuesta a más tardar: ${c.dueDate}")
                }
                b.btnSend.isEnabled = true
                b.btnSend.text = "Ver mis reclamos"
                b.success.successHalo.pop()
                (b.success.ivSuccess.drawable as? Animatable)?.start()
            }.onFailure {
                setLoading(false)
                snack(it.toUserMessage(), long = true)
            }
        }
    }

    private fun openList() {
        val options = NavOptions.Builder().setPopUpTo(R.id.complaintFormFragment, true)
            .setEnterAnim(R.anim.slide_in_right).setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left).setPopExitAnim(R.anim.slide_out_right).build()
        findNavController().navigate(R.id.complaintsFragment, null, options)
    }

    private fun setLoading(loading: Boolean) {
        val b = _binding ?: return
        b.progress.isVisible = loading
        b.btnSend.isClickable = !loading
        b.btnSend.text = if (loading) "" else if (b.toggleType.checkedButtonId == R.id.btnQueja) "Enviar queja" else "Enviar reclamo"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
