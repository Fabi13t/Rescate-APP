package com.rescate.app.util

import android.net.Uri
import android.view.View
import android.widget.ImageView
import coil.load
import com.rescate.app.R
import com.rescate.app.data.model.Category
import com.rescate.app.data.model.Pack





object AppImages {

     
    fun pack(p: Pack): Any {
        p.photoUri?.let { return Uri.parse(it) }
        val t = p.title.lowercase()
        return when {
            "verdura" in t -> R.drawable.pack_verduras
            "postre" in t || "torta" in t -> R.drawable.pack_postres
            "fruta" in t -> R.drawable.pack_frutas
            "dulce" in t -> R.drawable.pack_panes_dulces
            "almuerzo" in t || "menú" in t || "menu" in t -> R.drawable.pack_almuerzo
            "abarrote" in t || "arroz" in t || "menestra" in t -> R.drawable.pack_abarrotes
            "pan" in t && "sorpresa" !in t -> R.drawable.pack_panes
            else -> when (p.category) {
                Category.PANADERIA -> R.drawable.pack_sorpresa_panaderia
                Category.ABARROTES -> R.drawable.pack_abarrotes
                Category.PREPARADOS -> R.drawable.pack_almuerzo
            }
        }
    }

     
    fun store(name: String): Int? = when {
        "Don Pedro" in name -> R.drawable.store_don_pedro
        "Frugal" in name -> R.drawable.store_mercado_frugal
        "Aroma" in name -> R.drawable.store_cafeteria_aroma
        "Esquina" in name -> R.drawable.store_fruteria_esquina
        "El Sol" in name -> R.drawable.store_minimarket_el_sol
        else -> null
    }

    



    fun ImageView.showPhoto(data: Any, clipTarget: View = this) {
        imageTintList = null
        setPadding(0, 0, 0, 0)
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipTarget.clipToOutline = true
        load(data) { crossfade(250) }
    }
}
