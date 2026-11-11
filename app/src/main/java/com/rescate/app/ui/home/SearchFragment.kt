package com.rescate.app.ui.home

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.rescate.app.R
import com.rescate.app.data.model.SearchFilters
import com.rescate.app.databinding.FragmentSearchBinding
import com.rescate.app.util.UiState
import com.rescate.app.util.hideKeyboard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

 
class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val vm: HomeViewModel by viewModels()
    private lateinit var adapter: PackAdapter
    private var debounce: Job? = null

    private val favoritesMode get() = arguments?.getBoolean("favorites") == true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = PackAdapter(
            onClick = { findNavController().navigate(R.id.toDetail, bundleOf("packId" to it.id)) },
            onFavorite = { p ->
                vm.toggleFavorite(p).also { if (favoritesMode && !it) vm.loadFavorites() }
            }
        )
        binding.rvResults.adapter = adapter

        if (favoritesMode) {
            binding.toolbar.title = "Packs favoritos"
            binding.searchRow.isVisible = false
            binding.tvFiltersSummary.isVisible = false
            binding.suggestions.isVisible = false
            vm.loadFavorites()
        } else {
            setupSearch()
        }

        vm.packs.observe(viewLifecycleOwner) { state ->
            binding.progress.visibility = if (state is UiState.Loading) View.VISIBLE else View.INVISIBLE
            if (state is UiState.Success) {
                adapter.favorites = vm.favoriteIds()
                adapter.submitList(state.data.map { it.copy() }) { _binding?.rvResults?.scheduleLayoutAnimation() }
                val empty = state.data.isEmpty()
                binding.empty.root.isVisible = empty
                if (empty) {
                    binding.empty.tvEmptyTitle.text = if (favoritesMode) "Aún no tienes favoritos" else "Sin resultados cercanos"
                    binding.empty.tvEmptyBody.text = if (favoritesMode) "Toca el corazón de un pack para guardarlo aquí."
                    else "Prueba con otra palabra o amplía la distancia en filtros."
                    binding.empty.ivEmpty.setImageResource(if (favoritesMode) R.drawable.empty_favoritos else R.drawable.empty_busqueda)
                    binding.empty.btnEmptyAction.isVisible = !favoritesMode
                }
            }
            if (state is UiState.Error) {
                binding.empty.root.isVisible = true
                binding.empty.tvEmptyBody.text = state.message
            }
        }
    }

    private fun setupSearch() {
        listOf("pan", "verduras", "postres", "frutas", "abarrotes").forEach { word ->
            binding.chipsSuggest.addView(Chip(requireContext()).apply {
                text = word
                isCheckable = false
                setOnClickListener { binding.etSearch.setText(word); binding.etSearch.setSelection(word.length) }
            })
        }
        arguments?.getString("category")?.takeIf { it.isNotBlank() }?.let {   }

        binding.etSearch.requestFocus()
        binding.etSearch.doAfterTextChanged { text ->
             
            debounce?.cancel()
            debounce = viewLifecycleOwner.lifecycleScope.launch {
                delay(350)
                val q = text?.toString()?.trim().orEmpty()
                binding.suggestions.isVisible = q.isEmpty()
                vm.load(vm.filters.copy(query = q))
            }
        }
        binding.etSearch.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) { hideKeyboard(); true } else false
        }
        binding.btnFilters.setOnClickListener {
            FilterBottomSheet.newInstance(vm.filters).show(childFragmentManager, "filters")
        }
        childFragmentManager.setFragmentResultListener(FilterBottomSheet.REQUEST, viewLifecycleOwner) { _, bundle ->
            BundleCompat.getSerializable(bundle, FilterBottomSheet.KEY, SearchFilters::class.java)?.let {
                vm.load(it)
                renderSummary(it)
            }
        }
        binding.empty.btnEmptyAction.setOnClickListener {
            val reset = SearchFilters(query = vm.filters.query, maxKm = 10f)
            vm.load(reset); renderSummary(reset)
        }
        renderSummary(vm.filters)
    }

    private fun renderSummary(f: SearchFilters) {
        val parts = mutableListOf<String>()
        f.category?.let { parts += it.label }
        parts += "Hasta ${f.maxKm.toInt()} km"
        parts += "Hasta S/ ${f.maxPrice.toInt()}"
        if (f.onlyWithStock) parts += "Solo con stock"
        binding.tvFiltersSummary.text = parts.joinToString(" · ")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
