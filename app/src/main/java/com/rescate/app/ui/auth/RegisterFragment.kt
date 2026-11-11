package com.rescate.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rescate.app.R
import com.rescate.app.data.model.Role
import com.rescate.app.data.remote.RegisterRequest
import com.rescate.app.databinding.FragmentRegisterBinding
import com.rescate.app.util.UiState
import com.rescate.app.util.hideKeyboard
import com.rescate.app.util.shake
import com.rescate.app.util.snack
import com.google.android.material.textfield.TextInputLayout

 
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val vm: AuthViewModel by viewModels()

    private var role: Role? = null
    private val step get() = binding.flipper.displayedChild
    private val totalSteps = 4

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener { back() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = back()
        })

         
        binding.cardVecino.setOnClickListener { selectRole(Role.VECINO) }
        binding.cardComercio.setOnClickListener { selectRole(Role.COMERCIO) }
        when (arguments?.getString("role")) {
            "COMERCIO" -> selectRole(Role.COMERCIO)
            "VECINO" -> selectRole(Role.VECINO)
        }

         
        binding.acDistrict.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, vm.districts()))
        binding.toggleDoc.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            val ruc = id == R.id.btnRuc
            binding.tilDoc.hint = if (ruc) "Número de RUC (11 dígitos)" else "Número de DNI (8 dígitos)"
            binding.tilDoc.counterMaxLength = if (ruc) 11 else 8
            binding.tilDoc.error = null
        }

         
        listOf(binding.tilNames, binding.tilLastNames, binding.tilEmail, binding.tilPhone,
            binding.tilDoc, binding.tilStoreName, binding.tilYape, binding.tilPassword).forEach { til ->
            til.editText?.doAfterTextChanged { til.error = null }
        }
        binding.cbTerms.setOnCheckedChangeListener { _, _ -> refreshButton() }

        binding.btnNext.setOnClickListener { next() }

        vm.register.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state is UiState.Loading
            binding.btnNext.isClickable = state !is UiState.Loading
            binding.btnNext.text = if (state is UiState.Loading) "" else buttonLabel()
            when (state) {
                is UiState.Success -> onRegistered(state.data.role)
                is UiState.Error -> snack(state.message)
                else -> Unit
            }
        }
        updateStepUi()
    }

    private fun selectRole(r: Role) {
        role = r
        binding.cardVecino.isChecked = r == Role.VECINO
        binding.cardComercio.isChecked = r == Role.COMERCIO
        binding.groupMerchant.isVisible = r == Role.COMERCIO
        binding.tvStep2Title.text = if (r == Role.COMERCIO) "Datos del responsable" else "Tus datos personales"
        binding.tvStep3Title.text = if (r == Role.COMERCIO) "Datos del comercio y Yape" else "Tu perfil"
        binding.tvMerchantNote.isVisible = r == Role.COMERCIO
        refreshButton()
    }

    private fun next() {
        hideKeyboard()
        if (!validate()) return
        if (step == totalSteps - 1) {
            submit()
        } else {
            binding.flipper.setInAnimation(requireContext(), R.anim.slide_in_right)
            binding.flipper.setOutAnimation(requireContext(), R.anim.slide_out_left)
            binding.flipper.showNext()
            if (step == totalSteps - 1) binding.tvSummary.text = summary()
            updateStepUi()
        }
    }

    private fun back() {
        if (step == 0) { findNavController().navigateUp(); return }
        binding.flipper.setInAnimation(requireContext(), R.anim.slide_in_left)
        binding.flipper.setOutAnimation(requireContext(), R.anim.slide_out_right)
        binding.flipper.showPrevious()
        updateStepUi()
    }

    private fun updateStepUi() {
        binding.tvStep.text = "Paso ${step + 1} de $totalSteps"
        binding.stepProgress.setProgressCompat((step + 1) * 100 / totalSteps, true)
        binding.btnNext.text = buttonLabel()
        refreshButton()
    }

    private fun buttonLabel() = when {
        step == 0 && role == Role.COMERCIO -> "Continuar como comercio"
        step == 0 -> "Continuar como vecino"
        step == totalSteps - 1 -> "Crear mi cuenta"
        else -> "Continuar"
    }

    private fun refreshButton() {
        binding.btnNext.isEnabled = when (step) {
            0 -> role != null
            totalSteps - 1 -> binding.cbTerms.isChecked
            else -> true
        }
        binding.btnNext.text = buttonLabel()
    }

    private fun text(til: TextInputLayout) = til.editText?.text?.toString()?.trim().orEmpty()

    private fun validate(): Boolean {
        val errors = mutableListOf<Pair<TextInputLayout, String?>>()
        when (step) {
            1 -> {
                errors += binding.tilNames to Validators.required(text(binding.tilNames), "nombres")
                errors += binding.tilLastNames to Validators.required(text(binding.tilLastNames), "apellidos")
                errors += binding.tilEmail to Validators.email(text(binding.tilEmail))
                errors += binding.tilPhone to Validators.phone(text(binding.tilPhone))
            }
            2 -> {
                if (role == Role.COMERCIO) {
                    val doc = text(binding.tilDoc)
                    val needed = if (binding.toggleDoc.checkedButtonId == R.id.btnRuc) 11 else 8
                    errors += binding.tilDoc to if (doc.length != needed) "Debe tener $needed dígitos." else null
                    errors += binding.tilStoreName to Validators.required(text(binding.tilStoreName), "datos del comercio")
                    errors += binding.tilYape to Validators.phone(text(binding.tilYape))
                }
                errors += binding.tilDistrict to if (binding.acDistrict.text.isNullOrBlank()) "Selecciona tu distrito." else null
                errors += binding.tilPassword to Validators.password(text(binding.tilPassword))
            }
        }
        errors.forEach { (til, msg) -> til.error = msg }
        val firstError = errors.firstOrNull { it.second != null }?.first
        firstError?.shake()
        return firstError == null
    }

    private fun summary(): String = buildString {
        appendLine("Tipo de cuenta: ${if (role == Role.COMERCIO) "Comercio" else "Vecino"}")
        appendLine("Nombre: ${text(binding.tilNames)} ${text(binding.tilLastNames)}")
        appendLine("Correo: ${text(binding.tilEmail)}")
        appendLine("Teléfono: +51 ${text(binding.tilPhone)}")
        if (role == Role.COMERCIO) {
            val docType = if (binding.toggleDoc.checkedButtonId == R.id.btnRuc) "RUC" else "DNI"
            appendLine("Comercio: ${text(binding.tilStoreName)}")
            appendLine("$docType: ${text(binding.tilDoc)}")
            appendLine("Yape: ${text(binding.tilYape)}")
        }
        append("Distrito: ${binding.acDistrict.text}")
    }

    private fun submit() {
        val r = role ?: return
        vm.register(
            RegisterRequest(
                names = text(binding.tilNames),
                lastNames = text(binding.tilLastNames),
                email = text(binding.tilEmail),
                phone = text(binding.tilPhone),
                district = binding.acDistrict.text.toString(),
                password = text(binding.tilPassword),
                role = r.name,
                document = if (r == Role.COMERCIO) text(binding.tilDoc) else null,
                storeName = if (r == Role.COMERCIO) text(binding.tilStoreName) else null,
                yapeNumber = if (r == Role.COMERCIO) text(binding.tilYape) else null
            )
        )
    }

    private fun onRegistered(r: Role) {
        val title = if (r == Role.COMERCIO) "Registro recibido" else "¡Cuenta creada!"
        val body = if (r == Role.COMERCIO)
            "Tu comercio está «Por verificar». Mientras tanto puedes explorar el panel en modo demostración."
        else "Bienvenido a Rescate. Ya puedes reservar packs cerca de ti."
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_eco)
            .setTitle(title)
            .setMessage(body)
            .setCancelable(false)
            .setPositiveButton("Empezar") { _, _ -> (requireActivity() as AuthActivity).openHome(r) }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
