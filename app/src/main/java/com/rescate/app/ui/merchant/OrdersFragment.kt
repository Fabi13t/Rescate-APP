package com.rescate.app.ui.merchant

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.rescate.app.R
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.data.remote.toUserMessage
import com.rescate.app.data.repository.MerchantRepository
import com.rescate.app.databinding.DialogInputBinding
import com.rescate.app.databinding.FragmentOrdersBinding
import com.rescate.app.ui.reservations.ReservationAdapter
import com.rescate.app.util.snack
import com.rescate.app.util.soles
import kotlinx.coroutines.launch

 
class OrdersFragment : Fragment(R.layout.fragment_orders) {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: MerchantRepository
    private lateinit var adapter: ReservationAdapter
    private var all: List<Reservation> = emptyList()

     
    private val scanner = registerForActivityResult(ScanContract()) { result ->
        val content = result.contents ?: return@registerForActivityResult
        val code = content.split("|").getOrNull(1) ?: content
        markPickedUp(code)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrdersBinding.bind(view)
        repo = MerchantRepository(requireContext())

        adapter = ReservationAdapter(merchantMode = true, onOpen = { showDetail(it) })
        binding.recycler.adapter = adapter
        binding.chips.setOnCheckedStateChangeListener { _, _ -> render() }
        binding.swipe.setColorSchemeResources(R.color.green_800)
        binding.swipe.setOnRefreshListener { load() }
        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 6) binding.fabScan.shrink() else if (dy < -6) binding.fabScan.extend()
            }
        })
        binding.fabScan.setOnClickListener { chooseScanMode() }
        childFragmentManager.setFragmentResultListener(PaymentReviewSheet.REQUEST, viewLifecycleOwner) { _, _ -> load() }
        load()
    }

    private fun load() {
        binding.swipe.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            all = runCatching { repo.orders() }.getOrDefault(emptyList())
            _binding?.swipe?.isRefreshing = false
            if (_binding != null) render()
        }
    }

    private fun render() {
        val list = when (binding.chips.checkedChipId) {
            R.id.chipPending -> all.filter { it.status == ReservationStatus.PAGO_EN_REVISION }
            R.id.chipConfirmed -> all.filter { it.status == ReservationStatus.CONFIRMADA }
            R.id.chipDone -> all.filter { !it.status.isActive }
            else -> all
        }
        adapter.submitList(list.map { it.copy() }) { _binding?.recycler?.scheduleLayoutAnimation() }
        binding.empty.root.isVisible = list.isEmpty()
        binding.empty.ivEmpty.setImageResource(R.drawable.empty_pedidos)
        binding.empty.tvEmptyTitle.text = "Sin pedidos aquí"
        binding.empty.tvEmptyBody.text = "Cuando un vecino reserve tus packs, aparecerá en esta lista."
        binding.empty.btnEmptyAction.isVisible = false
    }

    private fun showDetail(r: Reservation) {
        if (r.status == ReservationStatus.PAGO_EN_REVISION) {
            DemoData.payments.firstOrNull { it.reservationCode == r.code }?.let {
                PaymentReviewSheet.newInstance(it).show(childFragmentManager, "review")
                return
            }
        }
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pedido ${r.code}")
            .setMessage(
                "Cliente: ${r.customerName}\n" +
                    "Pack: ${r.quantity} × ${r.pack.title}\n" +
                    "Total: ${r.total.soles()} (Yape ${r.yapeReference ?: "—"})\n" +
                    "Reservado: ${r.createdAt}\n" +
                    "Estado: ${r.status.label}"
            )
            .setNegativeButton("Cerrar", null)
        if (r.status == ReservationStatus.CONFIRMADA) {
            builder.setPositiveButton("Marcar como entregado") { _, _ -> markPickedUp(r.code) }
        }
        builder.show()
    }

    private fun chooseScanMode() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Validar recojo")
            .setItems(arrayOf("Escanear QR con la cámara", "Ingresar código manualmente")) { _, which ->
                if (which == 0) {
                    scanner.launch(
                        ScanOptions()
                            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            .setPrompt("Apunta al QR del comprobante del cliente")
                            .setBeepEnabled(false)
                            .setOrientationLocked(false)
                    )
                } else askCode()
            }
            .show()
    }

    private fun askCode() {
        val input = DialogInputBinding.inflate(LayoutInflater.from(requireContext()))
        input.til.hint = "Código (ej. RSC-DEMO-0899)"
        input.et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ingresar código")
            .setView(input.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Validar") { _, _ -> markPickedUp(input.et.text.toString().trim().uppercase()) }
            .show()
    }

    private fun markPickedUp(code: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { repo.markPickedUp(code) }
                .onSuccess { snack("✅ ${it.customerName}: ${it.pack.title} entregado") ; load() }
                .onFailure { snack(it.toUserMessage(), long = true) }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
