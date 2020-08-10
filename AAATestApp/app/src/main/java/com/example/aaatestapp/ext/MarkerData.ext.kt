package com.example.aaatestapp.ext

import SavedMarkers
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
import kotlin.math.roundToInt

const val ICON_SIZE = 24
const val SCALE_FACTOR = 1.2f

fun MarkerData?.getIcon(context: Context): BitmapDescriptor? {
    return if(this?.bitmap != null)
        BitmapDescriptorFactory.fromBitmap(this.bitmap?.scaleToIcon(context))
    else
        bitmapDescriptorFromVector(
            context,
            this?.resIcon ?: R.drawable.ic_gps_marker
        )
}

fun MarkerData?.getIconLarger(context: Context): BitmapDescriptor? {
    return if(this?.bitmap != null)
        BitmapDescriptorFactory.fromBitmap(this.bitmap?.scaleToIcon(context, SCALE_FACTOR))
    else
        bitmapDescriptorFromVector(
            context,
            this?.resIcon ?: R.drawable.ic_gps_marker,
            SCALE_FACTOR
        )
}

fun bitmapDescriptorFromVector(context: Context, vectorResId: Int, factor: Float = 1f): BitmapDescriptor? {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)?:return null
    val width = (vectorDrawable.intrinsicWidth * factor).roundToInt()
    val height = (vectorDrawable.intrinsicHeight * factor).roundToInt()
    vectorDrawable.setBounds(0, 0, width, height)
    val bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
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

fun List<Marker>.toSerializableArray(): Array<MarkerData> {
    val list: MutableList<MarkerData> = mutableListOf()
    val defaultM = R.drawable.ic_default_marker
    val gpsM = R.drawable.ic_gps_marker

    forEachIndexed { i, it ->
        list.add(
            it.toMarkerData(
                if(it.tag == MarkerType.DEFAULT) defaultM else gpsM,
                SavedMarkers.markers?.getOrNull(i)?.bitmap
            )
        )
    }
    return list.toTypedArray()
}

fun Bitmap?.scaleToIcon(context: Context): Bitmap?
{
    val size = ICON_SIZE * context.resources.displayMetrics.density.toInt()
    return this?.scale(size, size)
}

fun Bitmap?.scaleToIcon(context: Context, factor: Float): Bitmap?
{
    val size = (ICON_SIZE * factor * context.resources.displayMetrics.density).roundToInt()
    return this?.scale(size, size)
}