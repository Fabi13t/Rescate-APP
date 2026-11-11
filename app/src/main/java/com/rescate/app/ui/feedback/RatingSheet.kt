package com.rescate.app.ui.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.rescate.app.R
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.remote.toUserMessage
import com.rescate.app.data.repository.FeedbackRepository
import com.rescate.app.databinding.SheetRatingBinding
import com.rescate.app.util.pop
import com.rescate.app.util.shake
import kotlinx.coroutines.launch

 
class RatingSheet : BottomSheetDialogFragment() {

    private var _binding: SheetRatingBinding? = null
    private val binding get() = _binding!!
    private var stars = 0
    private val starViews = mutableListOf<ImageView>()

    private val labels = listOf("Muy malo 😞", "Malo 😕", "Regular 😐", "Bueno 🙂", "¡Excelente! 🤩")
    private val tags = listOf("Productos frescos", "Buena atención", "Recojo rápido", "Buen precio", "Bien empacado")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetRatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val r = BundleCompat.getSerializable(requireArguments(), KEY, Reservation::class.java) ?: return dismiss()
        binding.tvStore.text = "${r.pack.store.name} · ${r.pack.title}"

         
        val size = (44 * resources.displayMetrics.density).toInt()
        repeat(5) { i ->
            val star = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_star_border)
                imageTintList = ContextCompat.getColorStateList(context, R.color.amber)
                setPadding(size / 8, size / 8, size / 8, size / 8)
                contentDescription = "${i + 1} estrellas"
                setOnClickListener { select(i + 1) }
            }
            starViews += star
            binding.stars.addView(star, LinearLayout.LayoutParams(size, size))
        }
        tags.forEach { t -> binding.chipsTags.addView(Chip(requireContext()).apply { text = t; isCheckable = true }) }

        binding.btnSend.setOnClickListener { send(r) }
    }

    private fun select(n: Int) {
        stars = n
        starViews.forEachIndexed { i, v ->
            val on = i < n
            v.setImageResource(if (on) R.drawable.ic_star else R.drawable.ic_star_border)
            if (on) {
                v.scaleX = 0.6f; v.scaleY = 0.6f
                v.animate().scaleX(1f).scaleY(1f).setStartDelay(i * 45L).setDuration(320)
                    .setInterpolator(OvershootInterpolator(3f)).start()
            }
        }
        binding.tvStarsLabel.text = labels[n - 1]
        binding.tvStarsLabel.setTextColor(ContextCompat.getColor(requireContext(), if (n >= 4) R.color.green_700 else if (n == 3) R.color.amber else R.color.red))
        binding.btnSend.isEnabled = true
    }

    private fun send(r: Reservation) {
        if (stars == 0) { binding.stars.shake(); return }
        val chips = (0 until binding.chipsTags.childCount).map { binding.chipsTags.getChildAt(it) as Chip }
            .filter { it.isChecked }.joinToString(" · ") { it.text }
        val text = binding.etComment.text?.toString()?.trim().orEmpty()
        val comment = listOf(chips, text).filter { it.isNotBlank() }.joinToString(". ")

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { FeedbackRepository(requireContext()).rate(r, stars, comment.ifBlank { null }) }
                .onSuccess {
                    setFragmentResult(REQUEST, bundleOf("stars" to stars))
                    binding.tvStarsLabel.text = "¡Gracias por tu opinión! 💚"
                    binding.stars.pop()
                    binding.root.postDelayed({ if (isAdded) dismiss() }, 900)
                }
                .onFailure {
                    setLoading(false)
                    binding.tvStarsLabel.text = it.toUserMessage()
                    binding.tvStarsLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnSend.isClickable = !loading
        binding.btnSend.text = if (loading) "" else "Enviar calificación"
    }

    override fun onDestroyView() {
        starViews.clear()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST = "rating_request"
        private const val KEY = "reservation"
        fun newInstance(r: Reservation) = RatingSheet().apply { arguments = bundleOf(KEY to r) }
    }
}
