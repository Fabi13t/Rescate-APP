package com.rescate.app.ui.home

import android.graphics.Paint
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rescate.app.R
import com.rescate.app.data.model.Category
import com.rescate.app.data.model.Pack
import com.rescate.app.databinding.ItemPackBinding
import com.rescate.app.util.AppImages
import com.rescate.app.util.distanceLabel
import com.rescate.app.util.pop
import com.rescate.app.util.soles

class PackAdapter(
    private val onClick: (Pack) -> Unit,
    private val onFavorite: (Pack) -> Boolean
) : ListAdapter<Pack, PackAdapter.VH>(Diff) {

    var favorites: Set<Long> = emptySet()

    inner class VH(val b: ItemPackBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPackBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            (it.ivPhoto.parent as View).clipToOutline = true  
        })

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        val ctx = holder.itemView.context
        with(holder.b) {
            tvStore.text = p.store.name
            tvSubtitle.text = "${p.title} · ${p.distanceMeters.distanceLabel()}"
            tvPrice.text = p.rescuePrice.soles()
            tvOldPrice.text = p.normalPrice.soles()
            tvOldPrice.paintFlags = tvOldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            tvDiscount.text = "-${p.discountPercent}%"
            tvPickup.text = "Hasta ${p.pickupUntil}"
            ivCategory.setImageResource(iconFor(p.category))

             
            ivPhoto.isVisible = true
            ivCategory.isVisible = false
            ivPhoto.load(AppImages.pack(p)) { crossfade(250) }

            tvExpires.text = p.expiresLabel
            val urgent = p.expiresInDays == 0
            tvExpires.setBackgroundResource(if (urgent) R.drawable.bg_badge_red else R.drawable.bg_badge_amber)
            tvExpires.setTextColor(ContextCompat.getColor(ctx, if (urgent) R.color.red else R.color.amber))

            if (p.stock > 0) {
                tvStock.text = if (p.stock == 1) "¡Último pack!" else "${p.stock} packs"
                tvStock.setTextColor(ContextCompat.getColor(ctx, if (p.stock == 1) R.color.amber else R.color.green_700))
                root.alpha = 1f
            } else {
                tvStock.text = "Agotado"
                tvStock.setTextColor(ContextCompat.getColor(ctx, R.color.ink_soft))
                root.alpha = 0.6f
            }

            bindFavorite(this, p.id in favorites)
            btnFavorite.setOnClickListener {
                val fav = onFavorite(p)
                favorites = if (fav) favorites + p.id else favorites - p.id
                bindFavorite(this, fav)
                btnFavorite.pop()
            }
            root.setOnClickListener { onClick(p) }
        }
    }

    private fun bindFavorite(b: ItemPackBinding, fav: Boolean) {
        b.btnFavorite.setImageResource(if (fav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        b.btnFavorite.imageTintList = ContextCompat.getColorStateList(b.root.context, if (fav) R.color.red else R.color.ink_soft)
    }

    companion object {
        fun iconFor(c: Category) = when (c) {
            Category.PANADERIA -> R.drawable.ic_shopping_bag
            Category.ABARROTES -> R.drawable.ic_store
            Category.PREPARADOS -> R.drawable.ic_gift
        }
    }

    private object Diff : DiffUtil.ItemCallback<Pack>() {
        override fun areItemsTheSame(a: Pack, b: Pack) = a.id == b.id
        override fun areContentsTheSame(a: Pack, b: Pack) = a == b
    }
}
