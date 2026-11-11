package com.rescate.app.ui.profile

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.materialswitch.MaterialSwitch
import com.rescate.app.R
import com.rescate.app.data.local.SessionManager
import com.rescate.app.databinding.FragmentSettingsBinding
import com.rescate.app.util.NotificationHelper
import com.rescate.app.util.pop
import com.rescate.app.util.snack

 
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val switches = mutableMapOf<String, MaterialSwitch>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        val session = SessionManager.get(requireContext())

        NotificationHelper.channels.forEach { (id, name, desc) ->
            val sw = MaterialSwitch(requireContext()).apply {
                text = "$name\n$desc"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.ink))
                minHeight = (64 * resources.displayMetrics.density).toInt()
                isChecked = session.isChannelEnabled(id)
                trackTintList = ContextCompat.getColorStateList(context, R.color.selector_switch_track)
                setOnCheckedChangeListener { v, _ -> v.pop() }
            }
            switches[id] = sw
            binding.channels.addView(sw, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        binding.btnTest.setOnClickListener {
            NotificationHelper.show(requireContext(), NotificationHelper.CH_PACKS,
                "¡Pack disponible cerca!", "Panadería Don Pedro publicó un Pack sorpresa a S/ 6.00")
            snack("Si el canal está activo, verás la notificación arriba.")
        }
        binding.btnSave.setOnClickListener {
            switches.forEach { (id, sw) -> session.setChannelEnabled(id, sw.isChecked) }
            binding.btnSave.setIconResource(R.drawable.ic_check)
            binding.btnSave.text = "Cambios guardados"
            binding.btnSave.pop()
            snack("Preferencias de avisos guardadas")
        }
    }

    override fun onDestroyView() {
        switches.clear()
        _binding = null
        super.onDestroyView()
    }
}
