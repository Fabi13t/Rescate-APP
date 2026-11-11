package com.rescate.app.ui.donation

import android.app.Application
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.rescate.app.R
import com.rescate.app.data.local.SessionManager
import com.rescate.app.data.model.Donation
import com.rescate.app.data.remote.DonationRequest
import com.rescate.app.data.repository.DonationRepository
import com.rescate.app.data.repository.PackRepository
import com.rescate.app.databinding.FragmentDonateBinding
import com.rescate.app.util.UiState
import com.rescate.app.util.hideKeyboard
import com.rescate.app.util.launchState
import com.rescate.app.util.pop
import com.rescate.app.util.shake
import com.rescate.app.util.snack

class DonateViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DonationRepository(app)
    val districts = PackRepository(app).districts()
    val publish = MutableLiveData<UiState<Donation>?>()
    fun publish(r: DonationRequest) = launchState(publish) { repo.publish(r) }
}

 
class DonateFragment : Fragment(R.layout.fragment_donate) {

    private var _binding: FragmentDonateBinding? = null
    private val binding get() = _binding!!
    private val vm: DonateViewModel by viewModels()
    private val step get() = binding.flipper.displayedChild

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDonateBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { back() }
        binding.introIllustration.pop()

         
        (1..7).forEach { d -> addExpiryChip(d, if (d == 1) "1 día" else "$d días") }
        addExpiryChip(30, "Más de 7")
        binding.chipsExpiry.check(3)
        binding.chipsType.setOnCheckedStateChangeListener { _, _ -> updateExpiryRule() }
        updateExpiryRule()

        binding.sliderKg.addOnChangeListener { _, value, _ -> binding.tvKg.text = "${value.toInt()} kg" }
        binding.acDistrict.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, vm.districts))
        binding.acDistrict.setText(SessionManager.get(requireContext()).user?.district ?: "Los Olivos", false)

        binding.btnPrimary.setOnClickListener { next() }
        binding.btnSecondary.setOnClickListener {
            if (step == 3) findNavController().navigateUp() else findNavController().navigate(R.id.toMyDonations)
        }

        vm.publish.observe(viewLifecycleOwner) { state ->
            val loading = state is UiState.Loading
            binding.progress.isVisible = loading
            binding.btnPrimary.isClickable = !loading
            if (loading) binding.btnPrimary.text = ""
            when (state) {
                is UiState.Success -> showPublished()
                is UiState.Error -> { binding.btnPrimary.text = "Publicar donación"; snack(state.message) }
                else -> Unit
            }
        }
    }

    private fun addExpiryChip(days: Int, label: String) {
        binding.chipsExpiry.addView(Chip(requireContext()).apply {
            id = days
            text = label
            isCheckable = true
        })
    }

     
    private fun updateExpiryRule() {
        val nonPerishable = binding.chipsType.checkedChipId == R.id.chipNonPerishable
        val long = binding.chipsExpiry.findViewById<Chip>(30)
        long?.isEnabled = nonPerishable
        if (!nonPerishable && binding.chipsExpiry.checkedChipId == 30) binding.chipsExpiry.check(7)
        binding.tvExpiryHint.text = if (nonPerishable) "Los no perecibles pueden tener fecha más lejana."
        else "Los perecibles deben vencer en 7 días o menos."
    }

    private fun typeLabel() = when (binding.chipsType.checkedChipId) {
        R.id.chipNonPerishable -> "No perecible"
        R.id.chipCooked -> "Comida preparada"
        else -> "Perecible"
    }

    private fun request() = DonationRequest(
        title = binding.etTitle.text?.toString()?.trim().orEmpty(),
        foodType = typeLabel(),
        kg = binding.sliderKg.value.toDouble(),
         
        expiresInDays = binding.chipsExpiry.checkedChipId.coerceAtMost(7),
        district = binding.acDistrict.text.toString(),
        notes = binding.etNotes.text?.toString()?.trim().orEmpty()
    )

    private fun next() {
        hideKeyboard()
        when (step) {
            0 -> go(1, "Revisar publicación")
            1 -> {
                if (binding.etTitle.text.isNullOrBlank()) {
                    binding.tilTitle.error = "Cuéntanos qué vas a donar."
                    binding.tilTitle.shake()
                    return
                }
                binding.tilTitle.error = null
                val r = request()
                val days = binding.chipsExpiry.checkedChipId
                binding.tvReview.text = buildString {
                    appendLine("🍞  ${r.title}")
                    appendLine("Tipo: ${r.foodType}")
                    appendLine("Cantidad aproximada: ${r.kg.toInt()} kg")
                    appendLine("Vence en: ${if (days > 7) "más de 7 días" else "$days día(s)"}")
                    appendLine("Recojo en: ${r.district}")
                    if (r.notes.isNotBlank()) append("Notas: ${r.notes}")
                }
                go(2, "Publicar donación")
            }
            2 -> vm.publish(request())
            else -> findNavController().navigate(R.id.toMyDonations)
        }
    }

    private fun go(index: Int, label: String) {
        binding.flipper.setInAnimation(requireContext(), R.anim.slide_in_right)
        binding.flipper.setOutAnimation(requireContext(), R.anim.slide_out_left)
        binding.flipper.displayedChild = index
        binding.btnPrimary.text = label
        binding.btnSecondary.isVisible = index == 0
    }

    private fun back() {
        if (step in 1..2) {
            binding.flipper.setInAnimation(requireContext(), R.anim.slide_in_left)
            binding.flipper.setOutAnimation(requireContext(), R.anim.slide_out_right)
            val prev = step - 1
            binding.flipper.displayedChild = prev
            binding.btnPrimary.text = if (prev == 0) "Crear una donación" else "Revisar publicación"
            binding.btnSecondary.isVisible = prev == 0
        } else findNavController().navigateUp()
    }

    private fun showPublished() {
        go(3, "Ver mis donaciones")
        binding.btnSecondary.isVisible = true
        binding.btnSecondary.text = "Volver"
        binding.toolbar.title = "Donación publicada"
        binding.success.tvSuccessTitle.text = "¡Gracias por donar!"
        binding.success.tvSuccessBody.text = "Tu donación ya es visible para comedores\ny familias de ${binding.acDistrict.text}."
        binding.success.successHalo.pop()
        (binding.success.ivSuccess.drawable as? Animatable)?.start()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
