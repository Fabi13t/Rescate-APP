package com.rescate.app.ui.feedback

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.rescate.app.R
import com.rescate.app.data.model.Complaint
import com.rescate.app.data.model.Rating
import com.rescate.app.databinding.ItemComplaintBinding
import com.rescate.app.databinding.ItemReviewBinding
import com.rescate.app.util.starsText

 
class ComplaintAdapter(
    private val items: List<Complaint>,
    private val merchantMode: Boolean,
    private val onClick: (Complaint) -> Unit
) : RecyclerView.Adapter<ComplaintAdapter.VH>() {

    class VH(val b: ItemComplaintBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemComplaintBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        val ctx = holder.itemView.context
        with(holder.b) {
            tvCode.text = "${c.typeLabel} · ${c.code}"
            tvInfo.text = (if (merchantMode) c.customerName else c.storeName) + " · ${c.createdAt}"
            tvDetail.text = c.detail
            tvStatus.text = c.statusLabel
            val (bg, color) = when (c.status) {
                "PENDIENTE" -> R.drawable.bg_badge_amber to R.color.amber
                "EN_REVISION" -> R.drawable.bg_badge_yape to R.color.yape
                "RESPONDIDO" -> R.drawable.bg_badge_green to R.color.green_800
                else -> R.drawable.bg_badge_gray to R.color.ink_muted
            }
            tvStatus.setBackgroundResource(bg)
            tvStatus.setTextColor(ContextCompat.getColor(ctx, color))
            ivIcon.setBackgroundResource(if (c.type == "QUEJA") R.drawable.bg_icon_box_yape else R.drawable.bg_icon_box)
            ivIcon.imageTintList = ContextCompat.getColorStateList(ctx, if (c.type == "QUEJA") R.color.yape else R.color.green_800)
            tvDue.text = if (c.isOpen) "Responder hasta el ${c.dueDate}" else "Respondido el ${c.respondedAt ?: "—"}"
            tvDue.setTextColor(ContextCompat.getColor(ctx, if (c.isOpen && merchantMode) R.color.red else R.color.ink_muted))
            root.setOnClickListener { onClick(c) }
        }
    }
}

 
class ReviewAdapter(
    private val items: List<Rating>,
    private val onClick: ((Rating) -> Unit)? = null
) : RecyclerView.Adapter<ReviewAdapter.VH>() {

    class VH(val b: ItemReviewBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        with(holder.b) {
            tvAvatar.text = r.customerName.take(1).uppercase()
            tvName.text = r.customerName
            tvDate.text = r.createdAt
            tvStars.text = starsText(r.stars)
            tvComment.text = r.comment.ifBlank { "Sin comentario." }
            tvReply.isVisible = !r.merchantReply.isNullOrBlank()
            tvReply.text = "Respuesta del comercio: ${r.merchantReply}"
            root.isClickable = onClick != null
            root.setOnClickListener { onClick?.invoke(r) }
        }
    }
}
