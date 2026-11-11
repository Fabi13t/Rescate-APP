package com.rescate.app.ui.merchant

import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.rescate.app.R
import com.rescate.app.data.remote.toUserMessage
import com.rescate.app.data.repository.MerchantRepository
import com.rescate.app.databinding.FragmentNewPackBinding
import com.rescate.app.util.NotificationHelper
import com.rescate.app.util.hideKeyboard
import com.rescate.app.util.pop
import com.rescate.app.util.shake
import com.rescate.app.util.snack
import com.rescate.app.util.soles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

 
class NewPackFragment : Fragment(R.layout.fragment_new_pack) {

    private var _binding: FragmentNewPackBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: MerchantRepository

    private var photoUri: Uri? = null
    private var quantity = 3
    private var pickup = "20:30"
    private val step get() = binding.flipper.displayedChild

     
    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) setPhoto(uri)
    }

     
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) saveBitmap(bmp)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNewPackBinding.bind(view)
        repo = MerchantRepository(requireContext())

        binding.photoBox.setOnClickListener { choosePhotoSource() }
        binding.btnMinus.setOnClickListener { changeQty(-1) }
        binding.btnPlus.setOnClickListener { changeQty(+1) }
        binding.rowTime.setOnClickListener { pickTime() }
        binding.etNormal.doAfterTextChanged { updateDiscount() }
        binding.etRescue.doAfterTextChanged { updateDiscount() }
        binding.btnNext.setOnClickListener { next() }
        binding.btnBack.setOnClickListener { back() }
        binding.tvQty.text = quantity.toString()
        binding.tvTime.text = pickup
        updateUi()
    }

    private fun choosePhotoSource() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Foto del pack")
            .setItems(arrayOf("Tomar foto", "Elegir de la galería")) { _, which ->
                if (which == 0) takePhoto.launch(null)
                else pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            .show()
    }

    private fun setPhoto(uri: Uri) {
        photoUri = uri
        val b = _binding ?: return
        b.photoPlaceholder.isVisible = false
        b.ivPhoto.isVisible = true
        b.ivPhoto.load(uri) { crossfade(400) }
    }

    private fun saveBitmap(bmp: Bitmap) {
        viewLifecycleOwner.lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) {
                val file = File(requireContext().cacheDir, "pack_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                Uri.fromFile(file)
            }
            setPhoto(uri)
        }
    }

    private fun changeQty(delta: Int) {
        quantity = (quantity + delta).coerceIn(1, 50)
        binding.tvQty.text = quantity.toString()
        binding.tvQty.pop()
    }

    private fun pickTime() {
        val (h, m) = pickup.split(":").map { it.toInt() }
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(h).setMinute(m)
            .setTitleText("Hora límite de recojo")
            .build()
        picker.addOnPositiveButtonClickListener {
            pickup = "%02d:%02d".format(picker.hour, picker.minute)
            _binding?.tvTime?.text = pickup
            _binding?.tvTime?.pop()
        }
        picker.show(childFragmentManager, "time")
    }

    private fun price(text: CharSequence?) = text?.toString()?.replace(",", ".")?.toDoubleOrNull()

    private fun updateDiscount() {
        val normal = price(binding.etNormal.text)
        val rescue = price(binding.etRescue.text)
        binding.tvDiscountPreview.text = if (normal != null && rescue != null && normal > 0 && rescue < normal)
            "Descuento: ${(100 - rescue / normal * 100).toInt()} % · el cliente ahorra ${(normal - rescue).soles()}"
        else "Descuento: —"
    }

    private fun next() {
        hideKeyboard()
        when (step) {
            0 -> {
                if (binding.etName.text.isNullOrBlank()) {
                    binding.tilName.error = "Ponle un nombre al pack."
                    binding.tilName.shake(); return
                }
                binding.tilName.error = null
                go(1)
            }
            1 -> publish()
            else -> resetForm()
        }
    }

    private fun publish() {
        val normal = price(binding.etNormal.text)
        val rescue = price(binding.etRescue.text)
        binding.tilNormal.error = if (normal == null || normal <= 0) "Ingresa el precio" else null
        binding.tilRescue.error = when {
            rescue == null || rescue <= 0 -> "Ingresa el precio"
            normal != null && rescue >= normal -> "Debe ser menor"
            else -> null
        }
        if (binding.tilNormal.error != null || binding.tilRescue.error != null) {
            binding.btnNext.shake(); return
        }
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repo.publish(
                    title = binding.etName.text.toString().trim(),
                    description = binding.etDescription.text?.toString()?.trim().orEmpty()
                        .ifBlank { "Selección de productos del día en buen estado." },
                    normalPrice = normal!!, rescuePrice = rescue!!, quantity = quantity,
                    pickupUntil = pickup, perishable = binding.swPerishable.isChecked,
                    photoUri = photoUri?.toString()
                )
            }.onSuccess { pack ->
                setLoading(false)
                val ctx = context ?: return@onSuccess
                 
                NotificationHelper.show(ctx, NotificationHelper.CH_PACKS,
                    "¡Pack disponible cerca!", "${pack.store.name} publicó ${pack.title} a ${pack.rescuePrice.soles()}")
                val b = _binding ?: return@onSuccess
                b.tvPublishedSummary.text = "${pack.title}\n${pack.rescuePrice.soles()} (antes ${pack.normalPrice.soles()})\n" +
                    "$quantity packs · recojo hasta las $pickup"
                b.success.tvSuccessTitle.text = "Pack publicado correctamente"
                b.success.tvSuccessBody.text = "Los vecinos cercanos ya pueden reservarlo."
                go(2)
                b.success.successHalo.pop()
                (b.success.ivSuccess.drawable as? Animatable)?.start()
            }.onFailure {
                setLoading(false)
                snack(it.toUserMessage())
            }
        }
    }

    private fun go(index: Int) {
        val forward = index > step
        binding.flipper.setInAnimation(requireContext(), if (forward) R.anim.slide_in_right else R.anim.slide_in_left)
        binding.flipper.setOutAnimation(requireContext(), if (forward) R.anim.slide_out_left else R.anim.slide_out_right)
        binding.flipper.displayedChild = index
        updateUi()
    }

    private fun back() { if (step == 1) go(0) }

    private fun updateUi() {
        binding.tvStep.text = when (step) {
            0 -> "Paso 1 de 2 · Producto"
            1 -> "Paso 2 de 2 · Precio y horario"
            else -> "¡Listo!"
        }
        binding.stepProgress.setProgressCompat(if (step == 0) 50 else 100, true)
        binding.btnBack.isVisible = step == 1
        binding.btnNext.text = when (step) {
            0 -> "Siguiente"
            1 -> "Publicar pack"
            else -> "Publicar otro pack"
        }
    }

    private fun resetForm() {
        photoUri = null
        binding.ivPhoto.isVisible = false
        binding.photoPlaceholder.isVisible = true
        binding.etName.setText("Pack sorpresa")
        binding.etDescription.setText("")
        binding.etNormal.setText("")
        binding.etRescue.setText("")
        quantity = 3
        binding.tvQty.text = "3"
        go(0)
    }

    private fun setLoading(loading: Boolean) {
        val b = _binding ?: return
        b.progress.isVisible = loading
        b.btnNext.isClickable = !loading
        b.btnNext.text = if (loading) "" else "Publicar pack"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
