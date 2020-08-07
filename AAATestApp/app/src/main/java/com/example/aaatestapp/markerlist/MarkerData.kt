package com.example.aaatestapp.markerlist

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.google.android.gms.maps.model.BitmapDescriptorFactory

data class MarkerData(
    var lat: Double,
    var lon: Double,
    var resIcon:Int =-1,
    var bitmap: Bitmap? = null,
    var title:String = "",
    var location: String = "",
    var draggable: Boolean = true
){

    companion object{
        const val DEFAULT_ICON_SIZE: Int = 24
        const val DISABLED_ALPHA = 0.65f
        const val ENABLED_ALPHA = 1f
    }
}