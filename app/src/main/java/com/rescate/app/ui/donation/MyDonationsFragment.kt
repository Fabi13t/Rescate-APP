package com.rescate.app.ui.donation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rescate.app.R
import com.rescate.app.data.model.Donation
import com.rescate.app.data.repository.DonationRepository
import com.rescate.app.databinding.FragmentListScreenBinding
import com.rescate.app.databinding.ItemDonationBinding
import kotlinx.coroutines.launch

 
class MyDonationsFragment : Fragment(R.layout.fragment_list_screen) {

    private var _binding: FragmentListScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: DonationRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentListScreenBinding.bind(view)
        repo = DonationRepository(requireContext())
        binding.toolbar.title = "Mis donaciones"
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.fab.isVisible = true
        binding.fab.text = "Donar"
        binding.fab.setOnClickListener { findNavController().navigate(R.id.toDonate) }
        binding.empty.ivEmpty.setImageResource(R.drawable.empty_donaciones)
        binding.empty.tvEmptyTitle.text = "Aún no has donado"
        binding.empty.tvEmptyBody.text = "Tus donaciones aparecerán aquí."
        binding.empty.btnEmptyAction.isVisible = false
        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 6) binding.fab.shrink() else if (dy < -6) binding.fab.extend()
            }
        })
        load()
    }

    private fun load() {
        binding.progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val list = repo.myDonations()
            val b = _binding ?: return@launch
            b.progress.visibility = View.INVISIBLE
            b.recycler.adapter = Adapter(list)
            b.recycler.scheduleLayoutAnimation()
            b.empty.root.isVisible = list.isEmpty()
        }
    }

    private fun manage(d: Donation) {
        if (d.status != "Publicada") return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(d.title)
            .setMessage("${d.foodType} · ${d.kg.toInt()} kg · ${d.district}\n\n¿Ya no está disponible? Puedes retirar la publicación.")
            .setNegativeButton("Cerrar", null)
            .setPositiveButton("Retirar publicación") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch { repo.withdraw(d); load() }
            }
            .show()
    }

    private inner class Adapter(val items: List<Donation>) : RecyclerView.Adapter<Adapter.VH>() {
        inner class VH(val b: ItemDonationBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val d = items[position]
            val ctx = holder.itemView.context
            holder.b.tvTitle.text = d.title
            holder.b.tvInfo.text = "${d.foodType} · ${d.kg.toInt()} kg · ${d.createdAt}"
            holder.b.tvStatus.text = d.status
            val (bg, color) = when (d.status) {
                "Publicada" -> R.drawable.bg_badge_green to R.color.green_800
                "Entregada" -> R.drawable.bg_badge_yape to R.color.yape
                else -> R.drawable.bg_badge_gray to R.color.ink_muted
            }
            holder.b.tvStatus.setBackgroundResource(bg)
            holder.b.tvStatus.setTextColor(ContextCompat.getColor(ctx, color))
            holder.itemView.setOnClickListener { manage(d) }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
