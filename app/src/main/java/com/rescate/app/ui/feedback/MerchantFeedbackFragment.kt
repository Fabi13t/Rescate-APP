package com.rescate.app.ui.feedback

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rescate.app.R
import com.rescate.app.data.model.Complaint
import com.rescate.app.data.model.Rating
import com.rescate.app.data.remote.toUserMessage
import com.rescate.app.data.repository.FeedbackRepository
import com.rescate.app.databinding.DialogInputBinding
import com.rescate.app.databinding.FragmentMerchantFeedbackBinding
import com.rescate.app.util.countUp
import com.rescate.app.util.fadeVisible
import com.rescate.app.util.snack
import com.rescate.app.util.soles
import com.rescate.app.util.starsText
import kotlinx.coroutines.launch
import java.util.Locale

 
class MerchantFeedbackFragment : Fragment(R.layout.fragment_merchant_feedback) {

    private var _binding: FragmentMerchantFeedbackBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: FeedbackRepository
    private var ratings: List<Rating> = emptyList()
    private var complaints: List<Complaint> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMerchantFeedbackBinding.bind(view)
        repo = FeedbackRepository(requireContext())
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        if (arguments?.getString("tab") == "complaints") binding.chips.check(R.id.chipComplaints)
        binding.chips.setOnCheckedStateChangeListener { _, _ -> render() }
        load()
    }

    private val complaintsTab get() = binding.chips.checkedChipId == R.id.chipComplaints

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            val r = runCatching { repo.merchantRatings() }
            val c = runCatching { repo.merchantComplaints() }
            if (_binding == null) return@launch
            (r.exceptionOrNull() ?: c.exceptionOrNull())?.let { snack(it.toUserMessage()) }
            ratings = r.getOrDefault(emptyList())
            complaints = c.getOrDefault(emptyList())
            renderSummary()
            render()
        }
    }

     
    private fun renderSummary() {
        val avg = if (ratings.isEmpty()) 0.0 else ratings.map { it.stars }.average()
        binding.tvAverage.countUp(avg, { String.format(Locale.US, "%.1f", it) }, 900)
        binding.tvAverageStars.text = starsText(Math.round(avg).toInt())
        binding.tvCount.text = "${ratings.size} opiniones"
        val bars = mapOf(5 to binding.bar5, 4 to binding.bar4, 3 to binding.bar3, 2 to binding.bar2, 1 to binding.bar1)
        bars.forEach { (n, bar) ->
            val pct = if (ratings.isEmpty()) 0 else ratings.count { it.stars == n } * 100 / ratings.size
            bar.setProgressCompat(0, false)
            bar.postDelayed({ _binding?.let { bar.setProgressCompat(pct, true) } }, 150L + (5 - n) * 60L)
        }
        val open = complaints.count { it.isOpen }
        binding.chipComplaints.text = if (open > 0) "Reclamos ($open por responder)" else "Reclamos"
    }

    private fun render() {
        binding.summaryCard.fadeVisible(!complaintsTab)
        if (complaintsTab) {
            binding.recycler.adapter = ComplaintAdapter(complaints, merchantMode = true) { showComplaint(it) }
            binding.empty.root.isVisible = complaints.isEmpty()
            binding.empty.ivEmpty.setImageResource(R.drawable.empty_busqueda)
            binding.empty.tvEmptyTitle.text = "Sin reclamos"
            binding.empty.tvEmptyBody.text = "¡Muy bien! Tus clientes no han registrado reclamos."
        } else {
            binding.recycler.adapter = ReviewAdapter(ratings) { reply(it) }
            binding.empty.root.isVisible = ratings.isEmpty()
            binding.empty.ivEmpty.setImageResource(R.drawable.empty_favoritos)
            binding.empty.tvEmptyTitle.text = "Aún no tienes calificaciones"
            binding.empty.tvEmptyBody.text = "Aparecerán cuando tus clientes recojan sus packs."
        }
        binding.empty.btnEmptyAction.isVisible = false
        binding.recycler.scheduleLayoutAnimation()
    }

    private fun input(hint: String, initial: String? = null): DialogInputBinding =
        DialogInputBinding.inflate(LayoutInflater.from(requireContext())).apply {
            til.hint = hint
            et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            et.minLines = 3
            et.setText(initial)
        }

    private fun reply(r: Rating) {
        val field = input("Tu respuesta pública", r.merchantReply)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${starsText(r.stars)}  ${r.customerName}")
            .setMessage(r.comment.ifBlank { "Sin comentario." })
            .setView(field.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Responder") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { repo.replyRating(r, field.et.text.toString().trim()) }
                        .onSuccess { snack("Respuesta publicada"); load() }
                        .onFailure { snack(it.toUserMessage()) }
                }
            }
            .show()
    }

    private fun showComplaint(c: Complaint) {
        val details = buildString {
            appendLine("Cliente: ${c.customerName}")
            c.reservationCode?.let { appendLine("Reserva: $it") }
            c.amount?.let { appendLine("Monto reclamado: ${it.soles()}") }
            appendLine("Registrado: ${c.createdAt} · Plazo: ${c.dueDate}")
            appendLine()
            appendLine("Detalle:\n${c.detail}")
            appendLine()
            append("Pedido del cliente:\n${c.request}")
            if (c.response != null) append("\n\nTu respuesta (${c.respondedAt}):\n${c.response}")
        }
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_assignment)
            .setTitle("${c.typeLabel} ${c.code}")
            .setMessage(details)
            .setNegativeButton("Cerrar", null)
        if (c.isOpen) {
            val field = input("Respuesta al cliente")
            builder.setView(field.root).setPositiveButton("Enviar respuesta") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { repo.respondComplaint(c, field.et.text.toString().trim()) }
                        .onSuccess { snack("Respuesta enviada al cliente ✅"); load() }
                        .onFailure { snack(it.toUserMessage(), long = true) }
                }
            }
        }
        builder.show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
