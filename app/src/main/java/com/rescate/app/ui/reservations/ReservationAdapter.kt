package com.rescate.app.ui.reservations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rescate.app.R
import com.rescate.app.data.model.Reservation
import com.rescate.app.data.model.ReservationStatus
import com.rescate.app.databinding.ItemReservationBinding
import com.rescate.app.util.soles

 
class ReservationAdapter(
    private val merchantMode: Boolean = false,
    private val onOpen: (Reservation) -> Unit,
    private val onCancel: (Reservation) -> Unit = {},
    private val onRate: (Reservation) -> Unit = {},
    private val onComplain: (Reservation) -> Unit = {}
) : ListAdapter<Reservation, ReservationAdapter.VH>(Diff) {

    inner class VH(val b: ItemReservationBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = getItem(position)
        val ctx = holder.itemView.context
        with(holder.b) {
            tvStore.text = if (merchantMode) r.customerName else r.pack.store.name
            tvInfo.text = "${r.quantity} × ${r.pack.title} · ${r.total.soles()}\n${r.createdAt} · ${r.code}"
            tvStatus.text = r.status.label
            val (bg, color, icon) = style(r.status)
            tvStatus.setBackgroundResource(bg)
            tvStatus.setTextColor(ContextCompat.getColor(ctx, color))
            ivIcon.setImageResource(icon)
            ivIcon.imageTintList = ContextCompat.getColorStateList(ctx, color)

            actions.isVisible = r.status.isActive
            btnCancel.isVisible = !merchantMode && r.status == ReservationStatus.CONFIRMADA
            btnVoucher.text = if (merchantMode) "Ver detalle" else "Ver QR"
            btnVoucher.setOnClickListener { onOpen(r) }
            btnCancel.setOnClickListener { onCancel(r) }

             
            val history = !merchantMode && r.status in setOf(
                ReservationStatus.RECOGIDA, ReservationStatus.NO_RECOGIDA, ReservationStatus.RECHAZADA
            )
            historyActions.isVisible = history
            btnRate.isVisible = r.status == ReservationStatus.RECOGIDA
            val rated = r.myRating
            btnRate.text = if (rated != null) "★ $rated calificado" else "Calificar"
            btnRate.isEnabled = rated == null
            btnRate.alpha = if (rated != null) 0.6f else 1f
            btnRate.setOnClickListener { onRate(r) }
            btnComplain.setOnClickListener { onComplain(r) }
            root.setOnClickListener { onOpen(r) }
        }
    }

    private fun style(s: ReservationStatus): Triple<Int, Int, Int> = when (s) {
        ReservationStatus.CONFIRMADA -> Triple(R.drawable.bg_badge_green, R.color.green_800, R.drawable.ic_event_note)
        ReservationStatus.RECOGIDA -> Triple(R.drawable.bg_badge_gray, R.color.green_600, R.drawable.ic_check)
        ReservationStatus.PAGO_EN_REVISION, ReservationStatus.RETENIDA -> Triple(R.drawable.bg_badge_yape, R.color.yape, R.drawable.ic_schedule)
        ReservationStatus.CANCELADA, ReservationStatus.RECHAZADA -> Triple(R.drawable.bg_badge_red, R.color.red, R.drawable.ic_close)
        ReservationStatus.NO_RECOGIDA -> Triple(R.drawable.bg_badge_amber, R.color.amber, R.drawable.ic_warning)
    }

    private object Diff : DiffUtil.ItemCallback<Reservation>() {
        override fun areItemsTheSame(a: Reservation, b: Reservation) = a.id == b.id
        override fun areContentsTheSame(a: Reservation, b: Reservation) = a == b
    }
}
