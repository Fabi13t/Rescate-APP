package com.rescate.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rescate.app.R
import com.rescate.app.data.model.Category
import com.rescate.app.data.model.SearchFilters
import com.rescate.app.databinding.SheetFiltersBinding

 
class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SheetFiltersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val current = arguments?.let { BundleCompat.getSerializable(it, KEY, SearchFilters::class.java) } ?: SearchFilters()
        render(current)

        binding.sliderKm.addOnChangeListener { _, value, _ -> binding.tvKm.text = "${value.toInt()} km" }
        binding.sliderPrice.addOnChangeListener { _, value, _ -> binding.tvPrice.text = "S/ ${value.toInt()}" }
        binding.btnReset.setOnClickListener { render(SearchFilters(query = current.query)) }
        binding.btnApply.setOnClickListener {
            val category = when (binding.chipsCategory.checkedChipId) {
                R.id.fBakery -> Category.PANADERIA
                R.id.fGrocery -> Category.ABARROTES
                R.id.fPrepared -> Category.PREPARADOS
                else -> null
            }
            val result = current.copy(
                category = category,
                maxKm = binding.sliderKm.value,
                maxPrice = binding.sliderPrice.value,
                onlyWithStock = binding.swStock.isChecked
            )
            setFragmentResult(REQUEST, bundleOf(KEY to result))
            dismiss()
        }
    }

    private fun render(f: SearchFilters) {
        binding.chipsCategory.check(
            when (f.category) {
                Category.PANADERIA -> R.id.fBakery
                Category.ABARROTES -> R.id.fGrocery
                Category.PREPARADOS -> R.id.fPrepared
                null -> R.id.fAll
            }
        )
        binding.sliderKm.value = f.maxKm.coerceIn(1f, 10f)
        binding.sliderPrice.value = f.maxPrice.coerceIn(5f, 30f)
        binding.tvKm.text = "${f.maxKm.toInt()} km"
        binding.tvPrice.text = "S/ ${f.maxPrice.toInt()}"
        binding.swStock.isChecked = f.onlyWithStock
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST = "filters_request"
        const val KEY = "filters"
        fun newInstance(f: SearchFilters) = FilterBottomSheet().apply { arguments = bundleOf(KEY to f) }
    }
}
