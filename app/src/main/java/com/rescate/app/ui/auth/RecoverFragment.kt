package com.rescate.app.ui.auth

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.rescate.app.R
import com.rescate.app.databinding.FragmentRecoverBinding
import com.rescate.app.util.UiState
import com.rescate.app.util.hideKeyboard
import com.rescate.app.util.pop
import com.rescate.app.util.shake
import com.rescate.app.util.snack

 
class RecoverFragment : Fragment(R.layout.fragment_recover) {

    private var _binding: FragmentRecoverBinding? = null
    private val binding get() = _binding!!
    private val vm: AuthViewModel by viewModels()
    private var resendTimer: CountDownTimer? = null

    private val step get() = binding.flipper.displayedChild

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRecoverBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener { goBack() }
        binding.btnAction.setOnClickListener { next() }

        binding.etNewPass.doAfterTextChanged {
            val score = Validators.strength(it?.toString().orEmpty())
            binding.passStrength.setProgressCompat(score, true)
            val (label, color) = when {
                score < 40 -> "Débil" to R.color.red
                score < 70 -> "Media" to R.color.amber
                else -> "Fuerte" to R.color.green_600
            }
            binding.passStrength.setIndicatorColor(ContextCompat.getColor(requireContext(), color))
            binding.tvStrength.text = "Seguridad: $label"
        }
        binding.tvResend.setOnClickListener {
            if (resendTimer == null) { vm.sendCode(binding.etRecoverEmail.text.toString()); startResendTimer() }
        }

        vm.recover.observe(viewLifecycleOwner) { state ->
            setLoading(state is UiState.Loading)
            when (state) {
                is UiState.Success -> advance()
                is UiState.Error -> { snack(state.message); binding.btnAction.shake() }
                else -> Unit
            }
        }
    }

    private fun next() {
        hideKeyboard()
        when (step) {
            0 -> {
                val email = binding.etRecoverEmail.text?.toString()?.trim().orEmpty()
                val error = Validators.email(email)
                binding.tilRecoverEmail.error = error
                if (error == null) vm.sendCode(email) else binding.tilRecoverEmail.shake()
            }
            1 -> {
                val code = binding.etCode.text?.toString().orEmpty()
                if (code.length != 6) { binding.tilCode.error = "Ingresa los 6 dígitos."; binding.tilCode.shake() }
                else { binding.tilCode.error = null; vm.verifyCode(code) }
            }
            2 -> {
                val pass = binding.etNewPass.text?.toString().orEmpty()
                val confirm = binding.etConfirmPass.text?.toString().orEmpty()
                binding.tilNewPass.error = Validators.password(pass)
                binding.tilConfirmPass.error = if (pass != confirm) "Las contraseñas no coinciden." else null
                if (binding.tilNewPass.error == null && binding.tilConfirmPass.error == null) vm.resetPassword()
                else binding.tilConfirmPass.shake()
            }
            else -> findNavController().navigateUp()
        }
    }

    private fun advance() {
        vm.recover.value = null
        binding.flipper.setInAnimation(requireContext(), R.anim.slide_in_right)
        binding.flipper.setOutAnimation(requireContext(), R.anim.slide_out_left)
        binding.flipper.showNext()
        when (step) {
            1 -> {
                binding.tvCodeHint.text = "Ingresa el código que enviamos a ${binding.etRecoverEmail.text}. (Demo: cualquier código de 6 dígitos)"
                binding.btnAction.text = "Verificar código"
                startResendTimer()
            }
            2 -> binding.btnAction.text = "Guardar contraseña"
            3 -> {
                binding.toolbar.navigationIcon = null
                binding.btnAction.text = "Volver a iniciar sesión"
                binding.success.tvSuccessTitle.text = "Contraseña actualizada"
                binding.success.tvSuccessBody.text = "Ya puedes ingresar con tu nueva contraseña."
                binding.success.successHalo.pop()
                (binding.success.ivSuccess.drawable as? Animatable)?.start()
            }
        }
    }

    private fun goBack() {
        if (step in 1..2) {
            binding.flipper.setInAnimation(requireContext(), R.anim.slide_in_left)
            binding.flipper.setOutAnimation(requireContext(), R.anim.slide_out_right)
            binding.flipper.showPrevious()
            binding.btnAction.text = if (step == 0) "Enviar código" else "Verificar código"
        } else findNavController().navigateUp()
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(30_000, 1_000) {
            override fun onTick(ms: Long) {
                _binding?.tvResend?.text = "Reenviar código en ${ms / 1000} s"
            }
            override fun onFinish() {
                resendTimer = null
                _binding?.tvResend?.apply {
                    text = "Reenviar código"
                    setTextColor(ContextCompat.getColor(context, R.color.green_700))
                }
            }
        }.start()
    }

    private var savedLabel: CharSequence? = null

     
    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnAction.isClickable = !loading
        if (loading && savedLabel == null) {
            savedLabel = binding.btnAction.text
            binding.btnAction.text = ""
        } else if (!loading && savedLabel != null) {
            binding.btnAction.text = savedLabel
            savedLabel = null
        }
    }

    override fun onDestroyView() {
        resendTimer?.cancel()
        _binding = null
        super.onDestroyView()
    }
}
