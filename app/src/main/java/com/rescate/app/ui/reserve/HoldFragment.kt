package com.rescate.app.ui.reserve

import android.animation.ArgbEvaluator
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rescate.app.R
import com.rescate.app.databinding.FragmentHoldBinding
import com.rescate.app.util.pop
import com.rescate.app.util.pulse
import com.rescate.app.util.shake

 
class HoldFragment : Fragment(R.layout.fragment_hold) {

    private var _binding: FragmentHoldBinding? = null
    private val binding get() = _binding!!
    private val flow: ReservationFlowViewModel by activityViewModels()
    private val evaluator = ArgbEvaluator()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHoldBinding.bind(view)
        val reservation = flow.reservation ?: run { findNavController().navigateUp(); return }

        binding.toolbar.setNavigationOnClickListener { confirmCancel() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmCancel()
        })

        val left = reservation.pack.stock
        binding.tvBannerTitle.text = if (left == 0) "¡Última unidad para ti!" else "¡Pack reservado para ti!"
        binding.tvBannerBody.text = "${reservation.pack.store.name} · ${reservation.quantity} × ${reservation.pack.title}"
        binding.lastUnitCard.pop()

        val green = ContextCompat.getColor(requireContext(), R.color.green_600)
        val amber = ContextCompat.getColor(requireContext(), R.color.amber)
        val red = ContextCompat.getColor(requireContext(), R.color.red)

        binding.timerRing.max = ReservationFlowViewModel.HOLD_SECONDS
        flow.secondsLeft.observe(viewLifecycleOwner) { s ->
            binding.tvTimer.text = "%02d:%02d".format(s / 60, s % 60)
            binding.timerRing.setProgressCompat(s, true)
             
            val color = when {
                s > 60 -> green
                s > 30 -> evaluator.evaluate((60 - s) / 30f, green, amber) as Int
                else -> evaluator.evaluate((30 - s) / 30f, amber, red) as Int
            }
            binding.timerRing.setIndicatorColor(color)
            binding.tvTimer.setTextColor(if (s <= 30) red else ContextCompat.getColor(requireContext(), R.color.green_900))
            if (s in 1..30) binding.timerBox.pulse()
        }

        flow.expired.observe(viewLifecycleOwner) { expired ->
            if (expired && binding.flipper.displayedChild == 0) {
                binding.flipper.displayedChild = 1
                binding.ivExpired.shake()
                binding.toolbar.title = "Reserva expirada"
                binding.btnPay.text = "Volver a intentar"
                binding.btnCancel.text = "Volver al inicio"
            }
        }

        binding.btnPay.setOnClickListener {
            if (flow.expired.value == true) {
                flow.reservation?.pack?.let { flow.start(it) }
                findNavController().popBackStack(R.id.packDetailFragment, false)
            } else {
                findNavController().navigate(R.id.toYape)
            }
        }
        binding.btnCancel.setOnClickListener {
            if (flow.expired.value == true) findNavController().popBackStack(R.id.homeFragment, false)
            else confirmCancel()
        }
    }

    private fun confirmCancel() {
        if (flow.expired.value == true) { findNavController().popBackStack(R.id.homeFragment, false); return }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Cancelar la reserva?")
            .setMessage("El pack se liberará para otros vecinos.")
            .setNegativeButton("Seguir con la reserva", null)
            .setPositiveButton("Sí, cancelar") { _, _ ->
                flow.cancelHold { if (_binding != null) findNavController().popBackStack(R.id.packDetailFragment, false) }
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
