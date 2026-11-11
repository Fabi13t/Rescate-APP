package com.rescate.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rescate.app.R
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.model.AppNotification
import com.rescate.app.data.model.NotificationType
import com.rescate.app.databinding.FragmentListScreenBinding
import com.rescate.app.databinding.ItemNotificationBinding

 
class NotificationsFragment : Fragment(R.layout.fragment_list_screen) {

    private var _binding: FragmentListScreenBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentListScreenBinding.bind(view)
        binding.toolbar.title = "Notificaciones"
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.inflateMenu(R.menu.menu_notifications)
        binding.toolbar.setOnMenuItemClickListener {
            DemoData.notifications.forEach { n -> n.read = true }
            binding.recycler.adapter?.notifyItemRangeChanged(0, DemoData.notifications.size)
            true
        }

        binding.recycler.adapter = Adapter(DemoData.notifications)
        binding.empty.root.isVisible = DemoData.notifications.isEmpty()
        binding.empty.ivEmpty.setImageResource(R.drawable.empty_notificaciones)
        binding.empty.tvEmptyTitle.text = "Sin notificaciones"
        binding.empty.tvEmptyBody.text = "Te avisaremos cuando haya packs cerca de ti."
        binding.empty.btnEmptyAction.isVisible = false
    }

    private class Adapter(val items: List<AppNotification>) : RecyclerView.Adapter<Adapter.VH>() {
        class VH(val b: ItemNotificationBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val n = items[position]
            val ctx = holder.itemView.context
            holder.b.tvTitle.text = n.title
            holder.b.tvBody.text = n.body
            holder.b.tvTime.text = n.time
            holder.b.unreadDot.isVisible = !n.read
            val (icon, bg, tint) = when (n.type) {
                NotificationType.PACK -> Triple(R.drawable.ic_shopping_bag, R.drawable.bg_icon_box, R.color.green_800)
                NotificationType.ALERTA -> Triple(R.drawable.ic_notifications, R.drawable.bg_icon_box_amber, R.color.amber)
                NotificationType.RECORDATORIO -> Triple(R.drawable.ic_schedule, R.drawable.bg_icon_box_yape, R.color.yape)
                NotificationType.OFERTA -> Triple(R.drawable.ic_star, R.drawable.bg_icon_box_amber, R.color.amber)
                NotificationType.IMPACTO -> Triple(R.drawable.ic_eco, R.drawable.bg_icon_box, R.color.green_600)
            }
            holder.b.ivIcon.setImageResource(icon)
            holder.b.ivIcon.setBackgroundResource(bg)
            holder.b.ivIcon.imageTintList = ContextCompat.getColorStateList(ctx, tint)
            holder.itemView.setOnClickListener {
                n.read = true
                notifyItemChanged(holder.bindingAdapterPosition)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
