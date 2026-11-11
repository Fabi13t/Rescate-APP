package com.rescate.app.ui.reserve

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rescate.app.R
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.databinding.FragmentVoucherBinding
import com.rescate.app.util.QrUtils
import com.rescate.app.util.fadeInUp
import com.rescate.app.util.openDirections
import com.rescate.app.util.shareText
import com.rescate.app.util.soles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

 
class VoucherFragment : Fragment(R.layout.fragment_voucher) {

    private var _binding: FragmentVoucherBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVoucherBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val args = requireArguments()
        val r = BundleCompat.getSerializable(args, "reservation", Reservation::class.java)
            ?: run { findNavController().navigateUp(); return }
        val justPaid = args.getBoolean("justPaid", false)

        binding.tvCode.text = r.code
        binding.tvDetails.text = buildString {
            appendLine("Comercio:  ${r.pack.store.name}")
            appendLine("Pack:  ${r.quantity} × ${r.pack.title}")
            appendLine("Método:  Yape")
            appendLine("Total pagado:  ${r.total.soles()}")
            appendLine("Referencia Yape:  •••• ${r.yapeReference?.takeLast(4) ?: "—"}")
            append("Fecha:  ${r.createdAt}")
        }
        binding.tvPickupLimit.text = "Recoge hoy hasta las ${r.pack.pickupUntil}"
        val (label, bg, color) = when (r.status) {
            ReservationStatus.CONFIRMADA -> Triple("Pago confirmado", R.drawable.bg_badge_green, R.color.green_800)
            ReservationStatus.RECOGIDA -> Triple("Pack recogido", R.drawable.bg_badge_gray, R.color.ink_muted)
            ReservationStatus.PAGO_EN_REVISION -> Triple("Pago en revisión", R.drawable.bg_badge_yape, R.color.yape)
            else -> Triple(r.status.label, R.drawable.bg_badge_red, R.color.red)
        }
        binding.tvStatus.text = label
        binding.tvStatus.setBackgroundResource(bg)
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), color))
        binding.tvPickupLimit.isVisible = r.status == ReservationStatus.CONFIRMADA

         
        binding.ivQr.alpha = 0f
        viewLifecycleOwner.lifecycleScope.launch {
            val bmp = withContext(Dispatchers.Default) { QrUtils.generate("RESCATE|${r.code}|${r.pack.store.id}|${r.quantity}") }
            _binding?.ivQr?.apply {
                setImageBitmap(bmp)
                scaleX = 0.85f; scaleY = 0.85f
                animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(450).start()
            }
        }

        if (justPaid) {
            binding.successBanner.fadeInUp()
            (binding.ivCheck.drawable as? Animatable)?.start()
        }
        binding.ticket.fadeInUp(if (justPaid) 150 else 0)

        binding.btnShare.setOnClickListener {
            requireContext().shareText(
                "🧾 Comprobante Rescate\nCódigo: ${r.code}\n${r.quantity} × ${r.pack.title} — ${r.pack.store.name}\n" +
                    "Total: ${r.total.soles()} (Yape)\nRecojo hasta las ${r.pack.pickupUntil} en ${r.pack.store.address}",
                "Compartir comprobante"
            )
        }
        binding.btnDirections.setOnClickListener {
            requireContext().openDirections(r.pack.store.lat, r.pack.store.lon, r.pack.store.name)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
