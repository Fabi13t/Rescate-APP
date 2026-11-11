package com.rescate.app.ui.profile

import android.app.Application
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.rescate.app.R
import com.rescate.app.data.model.User
import com.rescate.app.data.repository.AuthRepository
import com.rescate.app.data.repository.PackRepository
import com.rescate.app.databinding.FragmentEditProfileBinding
import com.rescate.app.ui.auth.Validators
import com.rescate.app.util.UiState
import com.rescate.app.util.hideKeyboard
import com.rescate.app.util.launchState
import com.rescate.app.util.pop
import com.rescate.app.util.shake
import com.rescate.app.util.snack

class EditProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(app)
    val districts: List<String> = PackRepository(app).districts()
    val save = MutableLiveData<UiState<User>?>()
    fun user() = repo.currentUser()
    fun save(user: User) = launchState(save) { repo.updateProfile(user) }
}

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val vm: EditProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditProfileBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val user = vm.user() ?: return
        binding.acDistrict.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, vm.districts))
        if (savedInstanceState == null) {
            binding.etNames.setText(user.names)
            binding.etLastNames.setText(user.lastNames)
            binding.etEmail.setText(user.email)
            binding.etPhone.setText(user.phone)
            binding.acDistrict.setText(user.district, false)
        }
        binding.tvAvatar.text = user.initials
        listOf(binding.etNames, binding.etLastNames).forEach { et ->
            et.doAfterTextChanged {
                val initials = ("${binding.etNames.text?.take(1) ?: ""}${binding.etLastNames.text?.take(1) ?: ""}").uppercase()
                if (initials != binding.tvAvatar.text) { binding.tvAvatar.text = initials; binding.tvAvatar.pop() }
            }
        }
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }

        binding.btnSave.setOnClickListener {
            hideKeyboard()
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val err = Validators.email(email)
            binding.tilEmail.error = err
            if (err != null) { binding.tilEmail.shake(); return@setOnClickListener }
            vm.save(
                user.copy(
                    names = binding.etNames.text?.toString()?.trim().orEmpty(),
                    lastNames = binding.etLastNames.text?.toString()?.trim().orEmpty(),
                    email = email,
                    phone = binding.etPhone.text?.toString()?.trim().orEmpty(),
                    district = binding.acDistrict.text.toString()
                )
            )
        }

        vm.save.observe(viewLifecycleOwner) { state ->
            val loading = state is UiState.Loading
            binding.progress.isVisible = loading
            binding.btnSave.isClickable = !loading
            binding.btnSave.text = if (loading) "" else "Guardar cambios"
            when (state) {
                is UiState.Success -> {
                    binding.btnSave.setIconResource(R.drawable.ic_check)
                    binding.btnSave.text = "Cambios guardados"
                    binding.btnSave.pop()
                    binding.btnSave.postDelayed({ if (_binding != null) findNavController().navigateUp() }, 900)
                }
                is UiState.Error -> snack(state.message)
                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
