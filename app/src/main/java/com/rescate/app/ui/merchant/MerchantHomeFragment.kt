package com.rescate.app.ui.merchant

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rescate.app.R
import com.rescate.app.data.local.SessionManager
import com.rescate.app.data.model.PackStatus
import com.rescate.app.data.model.PaymentStatus
import com.rescate.app.data.remote.toUserMessage
import com.rescate.app.data.repository.MerchantRepository
import com.rescate.app.databinding.FragmentMerchantHomeBinding
import com.rescate.app.util.countUp
import com.rescate.app.util.fadeInUp
import com.rescate.app.util.snack
import kotlinx.coroutines.launch

class MerchantHomeFragment : Fragment(R.layout.fragment_merchant_home) {

    private var _binding: FragmentMerchantHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: MerchantRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMerchantHomeBinding.bind(view)
        repo = MerchantRepository(requireContext())

        val user = SessionManager.get(requireContext()).user
        binding.tvStore.text = user?.storeName ?: "Mi comercio"
        if (user?.verified == false) {
            binding.tvVerified.text = "Por verificar"
            binding.tvVerified.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_schedule, 0, 0, 0)
        }
        binding.btnPublish.setOnClickListener { (requireActivity() as MerchantActivity).goTo(R.id.mNewPackFragment) }
        binding.swipe.setColorSchemeResources(R.color.green_800)
        binding.swipe.setOnRefreshListener { load() }

        childFragmentManager.setFragmentResultListener(PaymentReviewSheet.REQUEST, viewLifecycleOwner) { _, b ->
            snack(if (b.getBoolean("approved")) "Pago aprobado ✅ El cliente ya puede recoger." else "Operación rechazada. El stock se liberó.")
            load()
        }
        if (savedInstanceState == null) binding.header.fadeInUp(0, 10f, 300)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val packs = repo.myPacks()
                val payments = repo.payments()
                val b = _binding ?: return@launch
                val pending = payments.filter { it.status == PaymentStatus.PENDIENTE }
                b.rvPayments.adapter = PaymentAdapter(pending) {
                    PaymentReviewSheet.newInstance(it).show(childFragmentManager, "review")
                }
                b.tvNoPayments.isVisible = pending.isEmpty()
                b.rvPacks.adapter = MerchantPackAdapter(packs)
                b.rvPacks.scheduleLayoutAnimation()
                b.rvPayments.scheduleLayoutAnimation()

                val active = packs.count { it.status == PackStatus.EN_PROCESO && it.stock > 0 }
                b.tvStatPacks.countUp(active.toDouble(), { it.toInt().toString() }, 700)
                b.tvStatPending.countUp(pending.size.toDouble(), { it.toInt().toString() }, 700)
                val cobrado = payments.filter { it.status == PaymentStatus.CONFIRMADO }.sumOf { it.amount }
                b.tvStatSales.countUp(cobrado, { "S/ ${it.toInt()}" }, 900)
            } catch (e: Exception) {
                snack(e.toUserMessage())
            } finally {
                _binding?.swipe?.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
