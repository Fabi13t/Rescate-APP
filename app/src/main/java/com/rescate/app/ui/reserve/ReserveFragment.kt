package com.rescate.app.ui.reserve

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rescate.app.R
import com.rescate.app.databinding.FragmentReserveBinding
import com.rescate.app.util.AppImages
import com.rescate.app.util.AppImages.showPhoto
import com.rescate.app.util.UiState
import com.rescate.app.util.pop
import com.rescate.app.util.soles
import com.rescate.app.util.staggerIn

 
class ReserveFragment : Fragment(R.layout.fragment_reserve) {

    private var _binding: FragmentReserveBinding? = null
    private val binding get() = _binding!!
    private val flow: ReservationFlowViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReserveBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val pack = flow.pack ?: run { findNavController().navigateUp(); return }

        binding.tvStore.text = pack.store.name
        binding.tvPackInfo.text = "${pack.title} · ${pack.rescuePrice.soles()} por unidad"
        binding.ivCategory.showPhoto(AppImages.pack(pack))
        binding.tvMax.text = "Máximo ${pack.stock}"

        binding.rowPickup.ivRowIcon.setImageResource(R.drawable.ic_schedule)
        binding.rowPickup.tvRowLabel.text = "Hora límite de recojo"
        binding.rowPickup.tvRowValue.text = pack.pickupUntil
        binding.rowAddress.ivRowIcon.setImageResource(R.drawable.ic_place)
        binding.rowAddress.tvRowLabel.text = "Dirección"
        binding.rowAddress.tvRowValue.text = pack.store.address
        binding.rowMethod.ivRowIcon.setImageResource(R.drawable.ic_wallet)
        binding.rowMethod.ivRowIcon.setBackgroundResource(R.drawable.bg_icon_box_yape)
        binding.rowMethod.ivRowIcon.setColorFilter(requireContext().getColor(R.color.yape))
        binding.rowMethod.tvRowLabel.text = "Método de pago"
        binding.rowMethod.tvRowValue.text = "Yape"
        binding.rowMethod.tvRowValue.setTextColor(requireContext().getColor(R.color.yape))

        binding.btnMinus.setOnClickListener { flow.changeQuantity(-1) }
        binding.btnPlus.setOnClickListener {
            if ((flow.quantity.value ?: 1) >= pack.stock) {
                binding.tvMax.text = "¡Stock máximo alcanzado!"
                binding.tvMax.pop()
            }
            flow.changeQuantity(+1)
        }

        flow.quantity.observe(viewLifecycleOwner) { q ->
            if (binding.tvQty.text.toString() != q.toString()) binding.tvQty.pop()
            binding.tvQty.text = q.toString()
            binding.btnMinus.isEnabled = q > 1
            binding.tvTotal.text = flow.total.soles()
            binding.tvSaving.text = "Ahorras ${((pack.normalPrice - pack.rescuePrice) * q).soles()}"
        }

        binding.btnContinue.setOnClickListener { flow.holdStock() }
        flow.hold.observe(viewLifecycleOwner) { state ->
            val loading = state is UiState.Loading
            binding.progress.isVisible = loading
            binding.btnContinue.isClickable = !loading
            binding.btnContinue.text = if (loading) "" else "Continuar con la reserva"
            when (state) {
                is UiState.Success -> {
                    flow.hold.value = null
                    findNavController().navigate(R.id.toHold)
                }
                is UiState.Error -> {
                    flow.hold.value = null
                    MaterialAlertDialogBuilder(requireContext())
                        .setIcon(R.drawable.ic_warning)
                        .setTitle("No se pudo reservar")
                        .setMessage(state.message)
                        .setPositiveButton("Entendido") { _, _ -> findNavController().navigateUp() }
                        .show()
                }
                else -> Unit
            }
        }

        if (savedInstanceState == null) staggerIn(binding.storeCard, binding.rowPickup.root, binding.rowAddress.root, binding.rowMethod.root, binding.totalCard)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
