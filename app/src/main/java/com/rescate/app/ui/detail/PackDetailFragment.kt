package com.rescate.app.ui.detail

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.rescate.app.R
import com.rescate.app.data.model.Pack
import com.rescate.app.data.remote.toUserMessage
import com.rescate.app.data.repository.PackRepository
import com.rescate.app.databinding.FragmentPackDetailBinding
import com.rescate.app.databinding.SheetStorePacksBinding
import com.rescate.app.data.model.Rating
import com.rescate.app.data.repository.FeedbackRepository
import com.rescate.app.ui.feedback.ReviewAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale
import com.rescate.app.ui.home.PackAdapter
import com.rescate.app.ui.reserve.ReservationFlowViewModel
import com.rescate.app.util.AppImages
import com.rescate.app.util.distanceLabel
import com.rescate.app.util.fadeInUp
import com.rescate.app.util.openDirections
import com.rescate.app.util.pop
import com.rescate.app.util.snack
import com.rescate.app.util.soles
import com.rescate.app.util.staggerIn
import kotlinx.coroutines.launch

class PackDetailFragment : Fragment(R.layout.fragment_pack_detail) {

    private var _binding: FragmentPackDetailBinding? = null
    private val binding get() = _binding!!
    private val flow: ReservationFlowViewModel by activityViewModels()
    private lateinit var repo: PackRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPackDetailBinding.bind(view)
        repo = PackRepository(requireContext())
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val id = requireArguments().getLong("packId")
        binding.content.alpha = 0f
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                render(repo.pack(id))
            } catch (e: Exception) {
                snack(e.toUserMessage())
                findNavController().navigateUp()
            }
        }
    }

    private fun render(p: Pack) {
        val b = _binding ?: return
        b.content.alpha = 1f
        b.tvStore.text = p.store.name
        b.tvRating.text = p.store.rating.toString()
        b.tvMeta.text = "${p.category.label} · ${p.distanceMeters.distanceLabel()} · Abierto hasta ${p.store.closesAt}"
        b.tvAddress.text = "${p.store.address}, ${p.store.district}"
        b.tvTitle.text = p.title
        b.tvDescription.text = p.description
        b.tvNormal.text = p.normalPrice.soles()
        b.tvRescue.text = p.rescuePrice.soles()
        b.tvBottomPrice.text = p.rescuePrice.soles()
        b.tvDiscount.text = "-${p.discountPercent}% de descuento"
        b.tvStock.text = if (p.stock > 0) "${p.stock} packs" else "Agotado"
        b.tvStockHint.text = if (p.stock > 0) "Quedan ${p.stock} de ${p.initialStock} packs" else "Se agotó. Activa avisos para saber si se libera uno."
        b.tvPickup.text = "Recoge hoy hasta las ${p.pickupUntil}"
        b.tvExpiry.text = "${p.expiresLabel} · " + if (p.perishable) "Perecible, consumir pronto" else "No perecible"
        b.ivHeroIcon.setImageResource(PackAdapter.iconFor(p.category))
        b.hero.clipToOutline = true  
        b.ivHeroPhoto.isVisible = true
        b.ivHeroPhoto.load(AppImages.pack(p)) { crossfade(300) }

         
        b.stockBar.setProgressCompat(0, false)
        b.stockBar.postDelayed({
            _binding?.stockBar?.setProgressCompat((p.stock * 100 / p.initialStock.coerceAtLeast(1)), true)
        }, 350)

        bindFavorite(repo.isFavorite(p.id))
        b.btnFavorite.setOnClickListener {
            val fav = repo.toggleFavorite(p.id)
            bindFavorite(fav)
            b.btnFavorite.pop()
            if (fav) snack("Pack guardado en favoritos")
        }
        loadRatings(p)
        b.btnDirections.setOnClickListener { requireContext().openDirections(p.store.lat, p.store.lon, p.store.name) }

        b.btnReserve.isEnabled = p.stock > 0
        b.btnReserve.text = if (p.stock > 0) "Reservar pack" else "Agotado"
        b.btnReserve.setOnClickListener {
            flow.start(p)
            findNavController().navigate(R.id.toReserve)
        }

         
        staggerIn(b.tvStore, b.tvMeta, b.tvAddress, b.tvTitle, b.tvDescription, b.stats, b.stockBar, b.expiryCard, b.btnDirections, step = 45)
        b.bottomBar.fadeInUp(200, 40f)
        b.ivHeroIcon.pop()
    }

     
    private fun loadRatings(p: Pack) {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = runCatching { FeedbackRepository(requireContext()).storeRatings(p.store.id, p.store.name) }
                .getOrDefault(emptyList())
            val b = _binding ?: return@launch
            b.tvRating.text = if (list.isEmpty()) "Nuevo"
            else String.format(Locale.US, "%.1f (%d)", list.map { it.stars }.average(), list.size)
            b.tvRating.setOnClickListener { showReviews(p, list) }
        }
    }

    private fun showReviews(p: Pack, list: List<Rating>) {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = SheetStorePacksBinding.inflate(layoutInflater)
        sheet.tvSheetTitle.text = "Opiniones · ${p.store.name}"
        sheet.rvSheet.adapter = ReviewAdapter(list)
        sheet.tvSheetEmpty.text = "Este comercio aún no tiene opiniones. ¡Sé el primero después de recoger tu pack!"
        sheet.tvSheetEmpty.isVisible = list.isEmpty()
        dialog.setContentView(sheet.root)
        dialog.show()
    }

    private fun bindFavorite(fav: Boolean) {
        val b = _binding ?: return
        b.btnFavorite.setIconResource(if (fav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        b.btnFavorite.text = if (fav) "Guardado" else "Guardar"
        b.btnFavorite.iconTint = ContextCompat.getColorStateList(requireContext(), R.color.red)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
