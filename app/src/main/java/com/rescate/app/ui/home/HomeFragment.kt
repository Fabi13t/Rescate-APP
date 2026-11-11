package com.rescate.app.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.rescate.app.R
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.local.SessionManager
import com.rescate.app.data.model.Category
import com.rescate.app.databinding.FragmentHomeBinding
import com.rescate.app.util.LocationHelper
import com.rescate.app.util.UiState
import com.rescate.app.util.fadeInUp
import com.rescate.app.util.snack

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val vm: HomeViewModel by viewModels()
    private lateinit var adapter: PackAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        val user = SessionManager.get(requireContext()).user
        binding.tvHello.text = "Hola, ${user?.names ?: "vecino"} 👋"
        binding.tvLocation.text = "${user?.district ?: "Los Olivos"}, Lima"
        binding.bellDot.isVisible = DemoData.notifications.any { !it.read }

        adapter = PackAdapter(
            onClick = { findNavController().navigate(R.id.toDetail, bundleOf("packId" to it.id)) },
            onFavorite = { pack ->
                vm.toggleFavorite(pack).also { fav -> if (fav) snack("Guardado en favoritos ❤") }
            }
        )
        binding.rvPacks.adapter = adapter

        binding.swipe.setColorSchemeResources(R.color.green_800, R.color.green_500)
        binding.swipe.setOnRefreshListener { refreshLocationAndLoad() }

        binding.chips.setOnCheckedStateChangeListener { _, ids ->
            val category = when (ids.firstOrNull()) {
                R.id.chipBakery -> Category.PANADERIA
                R.id.chipGrocery -> Category.ABARROTES
                R.id.chipPrepared -> Category.PREPARADOS
                else -> null
            }
            if (category != vm.filters.category) vm.setCategory(category)
        }

        binding.searchBar.setOnClickListener { findNavController().navigate(R.id.toSearch) }
        binding.btnBell.setOnClickListener { findNavController().navigate(R.id.toNotifications) }
        binding.tvSeeMap.setOnClickListener { findNavController().navigate(R.id.mapFragment) }
        binding.cardImpact.setOnClickListener { findNavController().navigate(R.id.impactFragment) }
        binding.cardDonate.setOnClickListener { findNavController().navigate(R.id.toDonate) }
        binding.empty.btnEmptyAction.setOnClickListener {
            binding.chips.check(R.id.chipAll)
            vm.load(vm.filters.copy(category = null, maxKm = 10f))
        }

        vm.packs.observe(viewLifecycleOwner) { state ->
            val loading = state is UiState.Loading && !binding.swipe.isRefreshing && adapter.itemCount == 0
            binding.shimmer.isVisible = loading
            if (loading) binding.shimmer.startShimmer() else binding.shimmer.stopShimmer()
            if (state !is UiState.Loading) binding.swipe.isRefreshing = false

            when (state) {
                is UiState.Success -> {
                    adapter.favorites = vm.favoriteIds()
                    val list = state.data.map { it.copy() }
                    val firstTime = adapter.itemCount == 0
                    adapter.submitList(list) {
                        if (firstTime) _binding?.rvPacks?.scheduleLayoutAnimation()
                    }
                    binding.rvPacks.isVisible = list.isNotEmpty()
                    binding.empty.root.isVisible = list.isEmpty()
                    binding.tvSection.text = if (list.isEmpty()) "Packs cerca de ti" else "Packs cerca de ti (${list.size})"
                }
                is UiState.Error -> {
                    binding.empty.ivEmpty.setImageResource(R.drawable.empty_notificaciones)
                    binding.rvPacks.isVisible = false
                    binding.empty.root.isVisible = true
                    binding.empty.tvEmptyTitle.text = "No pudimos cargar los packs"
                    binding.empty.tvEmptyBody.text = state.message
                    binding.empty.btnEmptyAction.text = "Reintentar"
                }
                else -> binding.empty.root.isVisible = false
            }
        }

        if (savedInstanceState == null) {
            binding.header.fadeInUp(0, 12f, 350)
            binding.cardImpact.fadeInUp(120)
        }
    }

    override fun onResume() {
        super.onResume()
         
        if (vm.packs.value is UiState.Success) vm.load()
    }

    private fun refreshLocationAndLoad() {
        LocationHelper.current(requireContext()) { vm.load() }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
