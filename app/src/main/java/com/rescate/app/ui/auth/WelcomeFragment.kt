package com.rescate.app.ui.auth

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rescate.app.R
import com.rescate.app.data.local.SessionManager
import com.rescate.app.databinding.FragmentWelcomeBinding
import com.rescate.app.util.fadeInUp

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!
    private val loops = mutableListOf<ObjectAnimator>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWelcomeBinding.bind(view)

         
        binding.logoContainer.apply {
            scaleX = 0.6f; scaleY = 0.6f; alpha = 0f
            animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650)
                .setInterpolator(OvershootInterpolator(1.4f)).start()
        }
        binding.tvBrand.fadeInUp(250)
        binding.tvTagline.fadeInUp(350)
        binding.btnStart.fadeInUp(500)
        binding.btnHaveAccount.fadeInUp(570)
        binding.tvMerchant.fadeInUp(640)
        float(binding.blob1, 18f, 3200)
        float(binding.blob2, -14f, 2800)

        val seen = SessionManager.get(requireContext()).onboardingSeen
        binding.btnStart.setOnClickListener {
            if (seen) findNavController().navigate(R.id.toLogin)
            else findNavController().navigate(R.id.toOnboarding)
        }
        binding.btnHaveAccount.setOnClickListener { findNavController().navigate(R.id.toLogin) }
        binding.tvMerchant.setOnClickListener {
            findNavController().navigate(R.id.toLogin, bundleOf("role" to "COMERCIO"))
        }
    }

     
    private fun float(view: View, distance: Float, duration: Long) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.translationY, view.translationY + distance * resources.displayMetrics.density).apply {
            this.duration = duration
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }.also { loops.add(it) }
    }

    override fun onDestroyView() {
        loops.forEach { it.cancel() }
        loops.clear()
        _binding = null
        super.onDestroyView()
    }
}
