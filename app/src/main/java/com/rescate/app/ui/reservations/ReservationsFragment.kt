package com.rescate.app.ui.reservations

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rescate.app.R
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.repository.ReservationRepository
import com.rescate.app.databinding.FragmentReservationsBinding
import com.rescate.app.ui.feedback.RatingSheet
import com.rescate.app.util.UiState
import com.rescate.app.util.launchState
import com.rescate.app.util.snack
import kotlinx.coroutines.launch

class ReservationsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ReservationRepository(app)
    val list = MutableLiveData<UiState<List<Reservation>>?>()
    val message = MutableLiveData<String?>()

    fun load() = launchState(list) { repo.myReservations() }

    fun cancel(r: Reservation) = viewModelScope.launch {
        runCatching { repo.cancel(r) }
            .onSuccess { message.value = "Reserva cancelada. El pack se liberó." }
            .onFailure { message.value = it.message }
        load()
    }
}

class ReservationsFragment : Fragment(R.layout.fragment_reservations) {

    private var _binding: FragmentReservationsBinding? = null
    private val binding get() = _binding!!
    private val vm: ReservationsViewModel by viewModels()
    private lateinit var adapter: ReservationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReservationsBinding.bind(view)

        adapter = ReservationAdapter(
            onOpen = { findNavController().navigate(R.id.toVoucher, bundleOf("reservation" to it)) },
            onCancel = { confirmCancel(it) },
            onRate = { RatingSheet.newInstance(it).show(childFragmentManager, "rating") },
            onComplain = { findNavController().navigate(R.id.toComplaintForm, bundleOf("reservation" to it)) }
        )
        childFragmentManager.setFragmentResultListener(RatingSheet.REQUEST, viewLifecycleOwner) { _, b ->
            snack("¡Gracias! Calificaste con ${b.getInt("stars")} ★")
            vm.load()
        }
        binding.recycler.adapter = adapter
        binding.swipe.setColorSchemeResources(R.color.green_800)
        binding.swipe.setOnRefreshListener { vm.load() }
        binding.chips.setOnCheckedStateChangeListener { _, _ -> render(animate = true) }
        binding.empty.btnEmptyAction.text = "Explorar packs"
        binding.empty.btnEmptyAction.setOnClickListener { findNavController().navigate(R.id.homeFragment) }

        vm.list.observe(viewLifecycleOwner) { state ->
            binding.swipe.isRefreshing = state is UiState.Loading
            if (state is UiState.Success) render(animate = true)
            if (state is UiState.Error) snack(state.message)
        }
        vm.message.observe(viewLifecycleOwner) { msg ->
            if (msg != null) { snack(msg); vm.message.value = null }
        }
        vm.load()
    }

    private fun render(animate: Boolean) {
        val all = (vm.list.value as? UiState.Success)?.data ?: return
        val filtered = when (binding.chips.checkedChipId) {
            R.id.chipActive -> all.filter { it.status.isActive }
            R.id.chipHistory -> all.filter { !it.status.isActive }
            else -> all
        }
        adapter.submitList(filtered.map { it.copy() }) {
            if (animate) _binding?.recycler?.scheduleLayoutAnimation()
        }
        binding.empty.root.isVisible = filtered.isEmpty()
        binding.empty.ivEmpty.setImageResource(R.drawable.empty_reservas)
        binding.empty.tvEmptyTitle.text = if (binding.chips.checkedChipId == R.id.chipActive) "Sin reservas activas" else "Aún no tienes reservas"
        binding.empty.tvEmptyBody.text = "Reserva un pack sorpresa y rescata comida cerca de ti."
    }

    private fun confirmCancel(r: Reservation) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Cancelar reserva?")
            .setMessage("${r.quantity} × ${r.pack.title} de ${r.pack.store.name}.\nSi ya pagaste, coordina la devolución con el comercio.")
            .setNegativeButton("Volver", null)
            .setPositiveButton("Cancelar reserva") { _, _ -> vm.cancel(r) }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
