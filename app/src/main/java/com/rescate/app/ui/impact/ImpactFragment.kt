package com.rescate.app.ui.impact

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.rescate.app.R
import com.rescate.app.data.model.Impact
import com.rescate.app.data.repository.ReservationRepository
import com.rescate.app.databinding.FragmentImpactBinding
import com.rescate.app.util.UiState
import com.rescate.app.util.countUp
import com.rescate.app.util.launchState
import com.rescate.app.util.shareText
import com.rescate.app.util.snack
import com.rescate.app.util.staggerIn
import java.util.Locale

class ImpactViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ReservationRepository(app)
    val impact = MutableLiveData<UiState<Impact>?>()
    var period = "resumen"
        private set

    fun load(p: String = period) {
        period = p
        launchState(impact) { repo.impact(p) }
    }

    init { load() }
}

 
class ImpactFragment : Fragment(R.layout.fragment_impact) {

    private var _binding: FragmentImpactBinding? = null
    private val binding get() = _binding!!
    private val vm: ImpactViewModel by viewModels()
    private var current: Impact? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImpactBinding.bind(view)

        binding.chips.setOnCheckedStateChangeListener { _, ids ->
            val p = when (ids.firstOrNull()) {
                R.id.chipWeek -> "semanal"
                R.id.chipMonth -> "mensual"
                R.id.chipTotal -> "total"
                else -> "resumen"
            }
            if (p != vm.period) vm.load(p)
        }

        vm.impact.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> render(state.data)
                is UiState.Error -> snack(state.message)
                else -> Unit
            }
        }

        binding.btnShare.setOnClickListener {
            val i = current ?: return@setOnClickListener
            requireContext().shareText(
                "🌱 Mi impacto con Rescate (${periodLabel()})\n" +
                    "• ${fmt(i.kg)} kg de alimentos rescatados\n" +
                    "• ${fmt(i.co2Kg)} kg de CO₂ evitado\n" +
                    "• S/ ${fmt(i.savedSoles)} ahorrados en ${i.packs} packs\n" +
                    "¡Únete y rescata comida cerca de ti!",
                "Compartir mi impacto"
            )
        }

        if (savedInstanceState == null) staggerIn(binding.heroCard, binding.statsRow, binding.chartCard, binding.tvEquivalence, binding.btnShare)
    }

    private fun render(i: Impact) {
        current = i
        binding.tvPeriodLabel.text = when (vm.period) {
            "semanal" -> "Rescatado esta semana"
            "mensual" -> "Rescatado este mes"
            "total" -> "Total histórico"
            else -> "Total rescatado"
        }
        binding.tvHeroKg.countUp(i.kg, { "${fmt(it)} kg" })
        binding.tvHeroSub.text = "${i.packs} packs · ${i.stores} comercios distintos"
        binding.tvKg.countUp(i.kg, { "${fmt(it)} kg" })
        binding.tvCo2.countUp(i.co2Kg, { "${fmt(it)} kg" })
        binding.tvSaved.countUp(i.savedSoles, { "S/ ${it.toInt()}" })
        binding.chart.setData(i.series)
        binding.tvChartTitle.text = when (vm.period) {
            "semanal" -> "Kilos rescatados por día"
            "mensual" -> "Kilos rescatados por semana"
            "total" -> "Kilos rescatados por año"
            else -> "Evolución de alimentos rescatados (kg)"
        }
        val goal = 25.0
        binding.goalBar.setProgressCompat(((i.kg / goal) * 100).toInt().coerceAtMost(100), true)
        binding.tvGoal.text = "Meta mensual: ${goal.toInt()} kg · ${((i.kg / goal) * 100).toInt().coerceAtMost(100)} % logrado"
        val meals = (i.kg / 0.5).toInt()
        binding.tvEquivalence.text = "Equivale a $meals platos de comida que no terminaron en la basura."
    }

    private fun periodLabel() = when (vm.period) {
        "semanal" -> "esta semana"
        "mensual" -> "este mes"
        "total" -> "histórico"
        else -> "resumen"
    }

    private fun fmt(v: Double) = String.format(Locale.US, "%.1f", v)

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
