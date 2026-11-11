package com.rescate.app.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rescate.app.BuildConfig
import com.rescate.app.R
import com.rescate.app.data.model.Store
import com.rescate.app.data.repository.PackRepository
import com.rescate.app.databinding.FragmentMapBinding
import com.rescate.app.databinding.SheetStorePacksBinding
import com.rescate.app.ui.home.PackAdapter
import com.rescate.app.util.AppImages
import com.rescate.app.util.AppImages.showPhoto
import com.rescate.app.util.LocationHelper
import com.rescate.app.util.distanceLabel
import com.rescate.app.util.openDirections
import com.rescate.app.util.snack
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker

 
class MapFragment : Fragment(R.layout.fragment_map) {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: PackRepository
    private val markers = mutableMapOf<Long, Marker>()
    private var selected: Store? = null
    private var meMarker: Marker? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapBinding.bind(view)
        repo = PackRepository(requireContext())

        binding.map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(15.5)
            controller.setCenter(GeoPoint(PackRepository.DEFAULT_LAT, PackRepository.DEFAULT_LON))
        }
        addStoreMarkers(repo.stores())

        binding.fabLocate.setOnClickListener { locate(animate = true) }
        binding.btnRoute.setOnClickListener {
            selected?.let { requireContext().openDirections(it.lat, it.lon, it.name) }
        }
        binding.btnPacks.setOnClickListener { selected?.let { showStorePacks(it) } }
        binding.etSearch.doAfterTextChanged { text ->
            val q = text?.toString()?.trim().orEmpty()
            val visible = repo.stores().filter { q.isBlank() || it.name.contains(q, true) || it.category.label.contains(q, true) }
            markers.forEach { (id, m) -> m.isEnabled = visible.any { it.id == id } }
            binding.map.invalidate()
            binding.tvCount.text = if (visible.isEmpty()) "Sin resultados cercanos" else "${visible.size} comercios con packs"
            if (visible.size == 1) select(visible.first())
        }
        binding.tvCount.text = "${repo.stores().size} comercios con packs"
        locate(animate = false)
    }

    private fun addStoreMarkers(stores: List<Store>) {
        stores.forEach { store ->
            val marker = Marker(binding.map).apply {
                position = GeoPoint(store.lat, store.lon)
                title = store.name
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.marker_store)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ -> select(store); true }
            }
            markers[store.id] = marker
            binding.map.overlays.add(marker)
        }
        binding.map.invalidate()
    }

    private fun select(store: Store) {
        val b = _binding ?: return
         
        selected?.let { prev -> markers[prev.id]?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.marker_store) }
        markers[store.id]?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.marker_store_selected)
        selected = store
        b.map.controller.animateTo(GeoPoint(store.lat, store.lon))
        b.map.invalidate()

        val packs = repo.packsOfStore(store.id)
        val me = LocationHelper.lastLocation
        val dist = if (me != null && !BuildConfig.DEMO_MODE) PackRepository.distance(me.latitude, me.longitude, store)
        else PackRepository.distance(PackRepository.DEFAULT_LAT, PackRepository.DEFAULT_LON, store)
        b.tvStoreName.text = store.name
        b.tvStoreRating.text = store.rating.toString()
        b.tvStoreInfo.text = "${packs.sumOf { it.stock }} packs · ${dist.distanceLabel()} · Abierto hasta ${store.closesAt}"
        val photo = AppImages.store(store.name)
        if (photo != null) b.ivStoreIcon.showPhoto(photo) else b.ivStoreIcon.setImageResource(PackAdapter.iconFor(store.category))

         
        b.storeCard.isVisible = true
        b.storeCard.translationY = 300f
        b.storeCard.alpha = 0f
        b.storeCard.animate().translationY(0f).alpha(1f).setDuration(320).start()
        b.fabLocate.animate().translationY(-(b.storeCard.height.takeIf { it > 0 } ?: 400).toFloat() - 16f).setDuration(320).start()
    }

    private fun showStorePacks(store: Store) {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = SheetStorePacksBinding.inflate(LayoutInflater.from(requireContext()))
        sheet.tvSheetTitle.text = store.name
        val packs = repo.packsOfStore(store.id)
        val adapter = PackAdapter(
            onClick = {
                dialog.dismiss()
                findNavController().navigate(R.id.toDetail, bundleOf("packId" to it.id))
            },
            onFavorite = { repo.toggleFavorite(it.id) }
        )
        sheet.rvSheet.adapter = adapter
        adapter.submitList(packs)
        sheet.tvSheetEmpty.isVisible = packs.isEmpty()
        dialog.setContentView(sheet.root)
        dialog.show()
    }

    private fun locate(animate: Boolean) {
        LocationHelper.current(requireContext()) { loc ->
            val b = _binding ?: return@current
            if (loc == null) {
                if (animate) snack("Activa la ubicación para ver tu posición.")
                return@current
            }
            val point = GeoPoint(loc.latitude, loc.longitude)
            if (meMarker == null) {
                meMarker = Marker(b.map).apply {
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.marker_me)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Tú estás aquí"
                }
                b.map.overlays.add(meMarker)
            }
            meMarker?.position = point
            if (animate) b.map.controller.animateTo(point)
            b.map.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        binding.map.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.map.onDetach()
        markers.clear()
        meMarker = null
        _binding = null
        super.onDestroyView()
    }
}
