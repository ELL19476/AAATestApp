package com.example.aaatestapp.ext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import com.example.aaatestapp.MarkerType
import com.example.aaatestapp.R
import com.example.aaatestapp.markerlist.MarkerData
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker

fun MarkerData?.getIcon(context: Context): BitmapDescriptor? {
    return if(this?.bitmap != null)
        BitmapDescriptorFactory.fromBitmap(this.bitmap?.scale(MarkerData.DEFAULT_ICON_SIZE, MarkerData.DEFAULT_ICON_SIZE))
    else
        bitmapDescriptorFromVector(
            context,
            this?.resIcon ?: R.drawable.ic_gps_marker
        )
}

fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
    vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
    val bitmap =
        Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun Marker.toMarkerData(iconId: Int, bitmap: Bitmap? = null): MarkerData = MarkerData(
    lat = this.position.latitude,
    lon = this.position.longitude,
    resIcon = iconId,
    bitmap = bitmap,
    title =  this.title,
    location = this.position.format(),
    draggable = this.isDraggable
)

fun <E: Marker> List<E>.toSerializableArrayWith(markerBitmaps: List<Bitmap?>): Array<MarkerData> {
    val list: MutableList<MarkerData> = mutableListOf()
    val defaultM = R.drawable.ic_default_marker
    val gpsM = R.drawable.ic_gps_marker

    forEachIndexed { i, it ->
        list.add(
            it.toMarkerData(
                if(it.tag == MarkerType.DEFAULT) defaultM else gpsM,
                markerBitmaps.getOrNull(i)
            )
        )
    }
    return list.toTypedArray()
}