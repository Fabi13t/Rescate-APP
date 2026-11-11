package com.rescate.app.ui.auth

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.rescate.app.R
import com.rescate.app.data.model.Role
import com.rescate.app.databinding.FragmentLoginBinding
import com.rescate.app.util.UiState
import com.rescate.app.util.fadeInUp
import com.rescate.app.util.hideKeyboard
import com.rescate.app.util.shake
import com.rescate.app.util.snack
import com.rescate.app.util.staggerIn

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val vm: AuthViewModel by viewModels()

    private var roleOverride: Role? = null
    private val role: Role
        get() = roleOverride ?: if (arguments?.getString("role") == "COMERCIO") Role.COMERCIO else Role.VECINO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        setupRole()
        if (savedInstanceState == null) {
            staggerIn(binding.ivHero, binding.tvHeadline, binding.tvSubtitle, binding.tilEmail, binding.tilPassword, binding.btnLogin)
        }

        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null; binding.errorCard.isVisible = false }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null; binding.errorCard.isVisible = false }
        binding.etPassword.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
        }

        binding.btnLogin.setOnClickListener { submit() }
        binding.tvForgot.setOnClickListener { findNavController().navigate(R.id.toRecover) }
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.toRegister, bundleOf("role" to role.name))
        }
        binding.btnGoogle.setOnClickListener { snack("Inicio con Google disponible próximamente.") }
        binding.tvSwitchRole.setOnClickListener {
            roleOverride = if (role == Role.COMERCIO) Role.VECINO else Role.COMERCIO
            setupRole()
            binding.content.fadeInUp(0, 12f, 300)
        }

        vm.login.observe(viewLifecycleOwner) { state ->
            setLoading(state is UiState.Loading)
            when (state) {
                is UiState.Success -> (requireActivity() as AuthActivity).openHome(state.data.role)
                is UiState.Error -> {
                    binding.tvError.text = state.message
                    binding.errorCard.fadeInUp(0, 8f, 250)
                    binding.errorCard.shake()
                }
                else -> Unit
            }
        }
    }

    private fun setupRole() {
        val merchant = role == Role.COMERCIO
        binding.tvRoleBadge.isVisible = merchant
        binding.ivHero.setImageResource(if (merchant) R.drawable.comercio_bienvenida else R.drawable.welcome_hero)
        binding.tvHeadline.text = if (merchant) "Modo comercio" else "¡Hola de nuevo! 👋"
        binding.tvSubtitle.text = if (merchant) "Gestiona tus packs, pedidos y pagos por Yape."
        else "Ingresa tus datos para seguir rescatando comida."
        binding.tvSwitchRole.text = if (merchant) "¿Eres vecino? Ingresa aquí" else "¿Tienes un comercio? Ingresa aquí"
        binding.tvOr.isVisible = !merchant
        binding.btnGoogle.isVisible = !merchant
    }

    private fun submit() {
        hideKeyboard()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val pass = binding.etPassword.text?.toString().orEmpty()
        val emailError = Validators.email(email)
        val passError = if (pass.isBlank()) "Ingresa tu contraseña." else null
        binding.tilEmail.error = emailError
        binding.tilPassword.error = passError
        when {
            emailError != null -> binding.tilEmail.shake()
            passError != null -> binding.tilPassword.shake()
            else -> vm.login(email, pass, role)
        }
    }

     
    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isClickable = !loading
        binding.btnLogin.text = if (loading) "" else "Iniciar sesión"
        binding.progress.isVisible = loading
        binding.tilEmail.isEnabled = !loading
        binding.tilPassword.isEnabled = !loading
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
