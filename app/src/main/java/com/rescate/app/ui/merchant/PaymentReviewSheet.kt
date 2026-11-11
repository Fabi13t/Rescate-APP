package com.rescate.app.ui.merchant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rescate.app.R
import com.rescate.app.data.model.Payment
import com.rescate.app.data.model.PaymentStatus
import com.rescate.app.data.remote.toUserMessage
import com.rescate.app.data.repository.MerchantRepository
import com.rescate.app.databinding.SheetPaymentReviewBinding
import com.rescate.app.util.soles
import kotlinx.coroutines.launch

 
class PaymentReviewSheet : BottomSheetDialogFragment() {

    private var _binding: SheetPaymentReviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetPaymentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val p = BundleCompat.getSerializable(requireArguments(), KEY, Payment::class.java) ?: return dismiss()
        val repo = MerchantRepository(requireContext())

        fun row(r: com.rescate.app.databinding.ViewInfoRowBinding, icon: Int, label: String, value: String) {
            r.ivRowIcon.setImageResource(icon); r.tvRowLabel.text = label; r.tvRowValue.text = value
        }
        row(binding.rowClient, R.drawable.ic_person, "Cliente", p.customerName)
        row(binding.rowAmount, R.drawable.ic_wallet, "Importe", p.amount.soles())
        row(binding.rowRef, R.drawable.ic_qr, "N.º de operación", p.reference)
        row(binding.rowProduct, R.drawable.ic_shopping_bag, "Producto", "${p.product} · ${p.reservationCode}")

        val pending = p.status == PaymentStatus.PENDIENTE
        binding.btnApprove.isVisible = pending
        binding.btnReject.isVisible = pending
        if (!pending) binding.tvState.text = "Este pago ya fue ${p.status.label.lowercase()}."

        fun act(approve: Boolean) {
            binding.btnApprove.isEnabled = false
            binding.btnReject.isEnabled = false
            binding.tvState.text = if (approve) "Aprobando…" else "Rechazando…"
            viewLifecycleOwner.lifecycleScope.launch {
                runCatching { if (approve) repo.approve(p) else repo.reject(p) }
                    .onSuccess {
                        setFragmentResult(REQUEST, bundleOf("approved" to approve))
                        dismiss()
                    }
                    .onFailure {
                        binding.tvState.text = it.toUserMessage()
                        binding.btnApprove.isEnabled = true
                        binding.btnReject.isEnabled = true
                    }
            }
        }
        binding.btnApprove.setOnClickListener { act(true) }
        binding.btnReject.setOnClickListener { act(false) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST = "payment_review"
        private const val KEY = "payment"
        fun newInstance(p: Payment) = PaymentReviewSheet().apply { arguments = bundleOf(KEY to p) }
    }
}
