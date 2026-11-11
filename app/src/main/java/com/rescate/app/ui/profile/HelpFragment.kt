package com.rescate.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rescate.app.R
import com.rescate.app.databinding.FragmentHelpBinding
import com.rescate.app.databinding.ItemFaqBinding
import com.rescate.app.util.fadeInUp

 
class HelpFragment : Fragment(R.layout.fragment_help) {

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    private val faqs = listOf(
        "¿Qué es un pack sorpresa?" to "Es una selección de productos del día que el comercio no vendió. Están en buen estado y se venden con descuento.",
        "¿Cómo pago mi pack?" to "Yapeas el monto al número del comercio y registras el número de operación en la app. El comercio lo valida antes de entregarte el pack.",
        "¿Por qué tengo solo 3 minutos?" to "Para evitar que dos personas compren el mismo pack, lo reservamos para ti durante 3 minutos mientras pagas.",
        "¿Qué pasa si no recojo mi pack?" to "Quedará como «No recogido». Si no puedes ir, cancela la reserva antes para que otro vecino lo aproveche.",
        "¿Puedo donar alimentos?" to "Sí. Desde Perfil › Donar alimentos puedes publicar comida con caducidad máxima de 7 días para comedores y familias."
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHelpBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        faqs.forEachIndexed { i, (q, a) ->
            val item = ItemFaqBinding.inflate(LayoutInflater.from(requireContext()), binding.faqContainer, false)
            item.tvQuestion.text = q
            item.tvAnswer.text = a
            item.header.setOnClickListener {
                val open = !item.tvAnswer.isVisible
                item.tvAnswer.isVisible = open
                item.ivChevron.animate().rotation(if (open) 180f else 0f).setDuration(200).start()
            }
            binding.faqContainer.addView(item.root)
            item.root.fadeInUp(i * 60L)
        }

        binding.rowEmail.setOnClickListener {
            runCatching {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:soporte@rescate.pe")).putExtra(Intent.EXTRA_SUBJECT, "Ayuda con Rescate"))
            }
        }
        binding.rowPhone.setOnClickListener {
            runCatching { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:016400000"))) }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
