package com.rescate.app.ui.merchant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rescate.app.R
import com.rescate.app.data.model.Pack
import com.rescate.app.data.model.PackStatus
import com.rescate.app.data.model.Payment
import com.rescate.app.data.model.PaymentStatus
import com.rescate.app.databinding.ItemMerchantPackBinding
import com.rescate.app.databinding.ItemPaymentBinding
import com.rescate.app.util.AppImages
import com.rescate.app.util.soles

class MerchantPackAdapter(private val items: List<Pack>) : RecyclerView.Adapter<MerchantPackAdapter.VH>() {
    class VH(val b: ItemMerchantPackBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMerchantPackBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val ctx = holder.itemView.context
        holder.b.tvTitle.text = p.title
        holder.b.tvPrice.text = p.rescuePrice.soles()
        holder.b.tvInfo.text = "Inicial: ${p.initialStock} · Disponible: ${p.stock} · ${p.publishedAt}"
        val sold = p.initialStock - p.stock
        holder.b.soldBar.setProgressCompat(sold * 100 / p.initialStock.coerceAtLeast(1), true)
        val finished = p.status == PackStatus.FINALIZADO || p.stock == 0
        holder.b.tvStatus.text = if (finished) "Finalizado" else "En proceso"
        holder.b.tvStatus.setBackgroundResource(if (finished) R.drawable.bg_badge_red else R.drawable.bg_badge_amber)
        holder.b.tvStatus.setTextColor(ContextCompat.getColor(ctx, if (finished) R.color.red else R.color.amber))
        (holder.b.ivPhoto.parent as android.view.View).clipToOutline = true
        holder.b.ivPhoto.isVisible = true
        holder.b.ivPhoto.load(AppImages.pack(p)) { crossfade(250) }
    }
}

class PaymentAdapter(
    private val items: List<Payment>,
    private val onClick: (Payment) -> Unit
) : RecyclerView.Adapter<PaymentAdapter.VH>() {
    class VH(val b: ItemPaymentBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val ctx = holder.itemView.context
        holder.b.tvName.text = p.customerName
        holder.b.tvInfo.text = "${p.amount.soles()} · ${p.product} · ${p.time}"
        holder.b.tvStatus.text = p.status.label
        val (bg, color) = when (p.status) {
            PaymentStatus.PENDIENTE -> R.drawable.bg_badge_amber to R.color.amber
            PaymentStatus.CONFIRMADO -> R.drawable.bg_badge_green to R.color.green_800
            PaymentStatus.RECHAZADO -> R.drawable.bg_badge_red to R.color.red
        }
        holder.b.tvStatus.setBackgroundResource(bg)
        holder.b.tvStatus.setTextColor(ContextCompat.getColor(ctx, color))
        holder.itemView.setOnClickListener { onClick(p) }
    }
}
