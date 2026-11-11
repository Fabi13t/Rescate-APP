package com.rescate.app.ui.reserve

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.rescate.app.R
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.databinding.FragmentYapeBinding
import com.rescate.app.util.UiState
import com.rescate.app.util.copyToClipboard
import com.rescate.app.util.hideKeyboard
import com.rescate.app.util.pop
import com.rescate.app.util.shake
import com.rescate.app.util.snack
import com.rescate.app.util.soles

 
class YapeFragment : Fragment(R.layout.fragment_yape) {

    private var _binding: FragmentYapeBinding? = null
    private val binding get() = _binding!!
    private val flow: ReservationFlowViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentYapeBinding.bind(view)
        val r = flow.reservation ?: run { findNavController().navigateUp(); return }
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.tvMerchant.text = r.pack.store.name
        binding.tvYapeNumber.text = r.pack.store.yapeNumber
        binding.tvAmount.text = "Monto: ${r.total.soles()}"
        binding.btnCopy.setOnClickListener {
            requireContext().copyToClipboard("Yape", r.pack.store.yapeNumber.replace(" ", ""))
            binding.btnCopy.pop()
            snack("Número copiado")
        }
        binding.btnOpenYape.setOnClickListener { openYape() }
        binding.etOperation.doAfterTextChanged { binding.tilOperation.error = null }
        binding.btnRegister.setOnClickListener { register() }

        flow.secondsLeft.observe(viewLifecycleOwner) { s ->
            binding.chipTimer.text = "%02d:%02d".format(s / 60, s % 60)
        }
        flow.expired.observe(viewLifecycleOwner) { expired ->
            if (expired && flow.payment.value == null) findNavController().navigateUp()
        }

        flow.payment.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.chipTimer.isVisible = false
                    binding.flipper.displayedChild = 1
                    binding.bottom.isVisible = false
                    binding.toolbar.navigationIcon = null
                }
                is UiState.Success -> {
                    val result = state.data
                    if (result.status == ReservationStatus.CONFIRMADA) {
                        flow.payment.value = null
                        val options = NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, false)
                            .setEnterAnim(R.anim.fade_up).setExitAnim(R.anim.fade_out)
                            .build()
                        findNavController().navigate(R.id.voucherFragment,
                            bundleOf("reservation" to result, "justPaid" to true), options)
                    } else {
                        showRejected()
                    }
                }
                is UiState.Error -> {
                    binding.flipper.displayedChild = 0
                    binding.bottom.isVisible = true
                    snack(state.message, long = true)
                }
                null -> Unit
            }
        }
    }

    private fun register() {
        hideKeyboard()
        val op = binding.etOperation.text?.toString()?.trim().orEmpty()
        if (op.length < 4) {
            binding.tilOperation.error = "Ingresa el número de operación (mín. 4 dígitos)."
            binding.tilOperation.shake()
            return
        }
        flow.reportYape(op)
    }

    private fun showRejected() {
        binding.flipper.displayedChild = 2
        binding.bottom.isVisible = true
        binding.btnRegister.text = "Volver al inicio"
        binding.btnRegister.setOnClickListener {
            flow.payment.value = null
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

    private fun openYape() {
        val intent = requireContext().packageManager.getLaunchIntentForPackage(YAPE_PACKAGE)
        if (intent != null) startActivity(intent)
        else snack("No encontramos Yape en este teléfono. Yapea desde otro dispositivo y registra la operación.", long = true)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val YAPE_PACKAGE = "com.bcp.innovacxion.yapeapp"
    }
}
