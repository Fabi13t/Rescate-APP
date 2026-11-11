package com.rescate.app.ui.auth

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.rescate.app.R
import com.rescate.app.data.local.SessionManager
import com.rescate.app.databinding.FragmentOnboardingBinding
import com.rescate.app.databinding.ItemOnboardingPageBinding
import kotlin.math.abs

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private data class Page(val image: Int, val title: String, val body: String)

    private val pages = listOf(
        Page(R.drawable.onboarding_rescata, "Rescata alimentos",
            "Panaderías, mercados y cafeterías publican packs sorpresa con productos en buen estado."),
        Page(R.drawable.onboarding_ahorra, "Ahorra cada día",
            "Reserva desde la app, paga con Yape y recoge en tienda hasta con 70 % de descuento."),
        Page(R.drawable.onboarding_impacto, "Genera impacto",
            "Mide los kilos rescatados y el CO₂ que evitas. También puedes donar lo que no usarás.")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        binding.pager.adapter = PageAdapter()
         
        binding.pager.setPageTransformer { page, position ->
            val p = abs(position)
            page.alpha = 1f - p * 0.6f
            page.scaleX = 1f - p * 0.12f
            page.scaleY = 1f - p * 0.12f
            page.findViewById<View>(R.id.illustration)?.translationX = position * page.width * 0.35f
        }
        buildDots()
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                binding.btnNext.text = if (position == pages.lastIndex) "Crear mi cuenta" else "Continuar"
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.pager.currentItem
            if (current < pages.lastIndex) binding.pager.currentItem = current + 1
            else finish(R.id.toRegister)
        }
        binding.btnSkip.setOnClickListener { finish(R.id.toLogin) }
        binding.btnLogin.setOnClickListener { finish(R.id.toLogin) }
    }

    private fun finish(action: Int) {
        SessionManager.get(requireContext()).onboardingSeen = true
        findNavController().navigate(action)
    }

    private fun buildDots() {
        val d = resources.displayMetrics.density
        repeat(pages.size) { i ->
            val dot = View(requireContext()).apply {
                background = ContextCompat.getDrawable(requireContext(), if (i == 0) R.drawable.bg_dot_active else R.drawable.bg_dot)
            }
            val lp = LinearLayout.LayoutParams(((if (i == 0) 24 else 8) * d).toInt(), (8 * d).toInt())
            lp.marginStart = (4 * d).toInt(); lp.marginEnd = (4 * d).toInt()
            binding.dots.addView(dot, lp)
        }
    }

     
    private fun updateDots(selected: Int) {
        val d = resources.displayMetrics.density
        for (i in 0 until binding.dots.childCount) {
            val dot = binding.dots.getChildAt(i)
            val target = ((if (i == selected) 24 else 8) * d).toInt()
            ValueAnimator.ofInt(dot.width, target).apply {
                duration = 250
                addUpdateListener { a -> dot.updateLayoutParams<LinearLayout.LayoutParams> { width = a.animatedValue as Int } }
                start()
            }
            dot.background = ContextCompat.getDrawable(requireContext(), if (i == selected) R.drawable.bg_dot_active else R.drawable.bg_dot)
        }
    }

    private inner class PageAdapter : RecyclerView.Adapter<PageAdapter.VH>() {
        inner class VH(val b: ItemOnboardingPageBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = pages.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = pages[position]
            holder.b.tvCounter.text = "0${position + 1} / 0${pages.size}"
            holder.b.ivIcon.setImageResource(page.image)
            holder.b.tvTitle.text = page.title
            holder.b.tvBody.text = page.body
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
