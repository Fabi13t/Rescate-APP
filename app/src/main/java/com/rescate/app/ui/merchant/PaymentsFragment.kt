package com.rescate.app.ui.merchant

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rescate.app.R
import com.rescate.app.data.local.SessionManager
import com.rescate.app.data.remote.toUserMessage
import com.rescate.app.data.repository.MerchantRepository
import com.rescate.app.databinding.DialogInputBinding
import com.rescate.app.databinding.FragmentPaymentsBinding
import com.rescate.app.ui.auth.Validators
import com.rescate.app.util.pop
import com.rescate.app.util.snack
import kotlinx.coroutines.launch

 
class PaymentsFragment : Fragment(R.layout.fragment_payments) {

    private var _binding: FragmentPaymentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: MerchantRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPaymentsBinding.bind(view)
        repo = MerchantRepository(requireContext())

        renderYape()
        binding.btnConfigYape.setOnClickListener { editYape(this) { renderYape(); binding.yapeCard.pop() } }
        binding.swipe.setColorSchemeResources(R.color.yape)
        binding.swipe.setOnRefreshListener { load() }
        childFragmentManager.setFragmentResultListener(PaymentReviewSheet.REQUEST, viewLifecycleOwner) { _, b ->
            snack(if (b.getBoolean("approved")) "Pago aprobado ✅" else "Operación rechazada")
            load()
        }
        load()
    }

    private fun renderYape() {
        val number = SessionManager.get(requireContext()).user?.yapeNumber ?: "987 654 321"
        binding.tvYape.text = "Yape · $number"
    }

    private fun load() {
        binding.swipe.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val list = runCatching { repo.payments() }.getOrDefault(emptyList())
            val b = _binding ?: return@launch
            b.swipe.isRefreshing = false
            b.recycler.adapter = PaymentAdapter(list) {
                PaymentReviewSheet.newInstance(it).show(childFragmentManager, "review")
            }
            b.recycler.scheduleLayoutAnimation()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
         
        fun editYape(fragment: Fragment, onSaved: () -> Unit) {
            val ctx = fragment.requireContext()
            val session = SessionManager.get(ctx)
            val input = DialogInputBinding.inflate(LayoutInflater.from(ctx))
            input.til.hint = "Número de celular Yape"
            input.til.prefixText = "+51 "
            input.et.inputType = InputType.TYPE_CLASS_PHONE
            input.et.setText(session.user?.yapeNumber?.replace(" ", ""))
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Configurar Yape")
                .setMessage("Tus clientes yapearán a este número.")
                .setView(input.root)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar") { _, _ ->
                    val n = input.et.text.toString().trim()
                    if (Validators.phone(n) == null) {
                        fragment.viewLifecycleOwner.lifecycleScope.launch {
                            runCatching { MerchantRepository(ctx).updateYape(n) }
                                .onSuccess {
                                    session.user = session.user?.copy(yapeNumber = n.chunked(3).joinToString(" "))
                                    onSaved()
                                    fragment.snack("Yape actualizado")
                                }
                                .onFailure { fragment.snack(it.toUserMessage()) }
                        }
                    } else fragment.snack("Ingresa un celular válido de 9 dígitos.")
                }
                .show()
        }
    }
}
