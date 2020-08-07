package com.example.aaatestapp.managers

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.widget.Toast
import com.example.aaatestapp.MarkerType
import com.example.aaatestapp.R
import com.example.aaatestapp.ext.*
import com.example.aaatestapp.markerlist.MarkerData
import com.example.aaatestapp.networking.MarkerDataHandler
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.disposables.Disposable

// TODO: FOCUS AND DEFOCUS WITH DRAWABLE SCALE INSTEAD OF NEW DRAWABLE
// TODO: on download marker SET CUSTOM BITMAP if it exists

class MarkerManager(private val context: Context, private val mMap: GoogleMap) {
    val markerBitmaps = mutableListOf<Bitmap?>()
    var markers = ObservableList<Marker>()

    var focused = mutableListOf<Marker>()

    val dataHandler: MarkerDataHandler by lazy {
        MarkerDataHandler(context.contentResolver)
    }

    private var gps: Marker? = null

    private var onUnfocusMarkers: (() -> Unit)? = null
    private var onUpdateMarkers: (() -> Unit)? = null
    private var onFocusMarker: (() -> Unit)? = null

    private var disposable: Disposable

    init {
        // update polygon on drag
        mMap.setOnMarkerDragListener(object: GoogleMap.OnMarkerDragListener{
            override fun onMarkerDrag(p0: Marker?) {
                if(p0 != null)
                    markers.update(p0)
            }

            override fun onMarkerDragStart(p0: Marker?) {
                if(p0 != null)
                    markers.update(p0)
            }

            override fun onMarkerDragEnd(p0: Marker?) {
                if(p0 != null)
                    markers.update(p0)
            }
        })

        // add click event to map
        mMap.setOnMapClickListener{ position ->
            addNew(position)
            unfocus()
        }

        mMap.setOnMarkerClickListener {
            println("CLICK!")
            if(it != gps)
            {
                if(focused.contains(it)) {
                    it.setIcon(
                        bitmapDescriptorFromVector(
                            context,
                            if (it.tag == MarkerType.DEFAULT) R.drawable.ic_default_marker else R.drawable.ic_gps_marker
                        )
                    )
                    focused.remove(it)
                    if(focused.isEmpty()) unfocus()
                }
                else
                    focused.add(it.apply {
                        it.setIcon(
                            bitmapDescriptorFromVector(
                                context,
                                if (it.tag == MarkerType.DEFAULT) R.drawable.ic_default_marker_focused else R.drawable.ic_gps_marker_focused
                            )
                        )
                    })
            }
            onFocusMarker?.invoke()
            true
        }

        // call update when MARKERS ARE UPDATED
        disposable =
            markers.observable
                .subscribe{
                    onUpdateMarkers?.invoke()
                }
    }

    fun downloadFromUser(name: String?) {
        dataHandler.loadMarkers(name) { data ->
            SavedMarkers.markers = data
            markers.list.forEach {
                it.remove()
            }
            markers.list.clear()
            focused.clear()

            // load saved markers
            SavedMarkers.markers?.forEach {
                addNew(it)
            }
        }
        Toast.makeText(context, context.getString(R.string.downloading_message), Toast.LENGTH_LONG).show()
    }

    fun initGpsMarker(position: Location) {
        if(gps != null) return

        val savedGpsMarker = SavedMarkers.gpsMarker
        gps = mMap.addMarker(MarkerOptions().position(LatLng(position.latitude, position.longitude))
            .apply {icon(
                savedGpsMarker?.getIcon(context)?: bitmapDescriptorFromVector(context, R.drawable.ic_gps_marker)
            )
            })
            .apply {
                title = savedGpsMarker?.title?:"Your location"
                tag = MarkerType.GPS
                setAnchor(0.5f, 0.5f)
            }
        gps?.isDraggable = savedGpsMarker?.draggable?:false
    }

    fun load()
    {
        SavedMarkers.markers?.forEach {
            update(it)
        }
        SavedMarkers.gpsMarker.let{
            if(it != null)
                update(it)
        }
    }

    fun save()
    {
        SavedMarkers.markers = markers.list.toSerializableArrayWith(markerBitmaps)
        if(gps != null)
            SavedMarkers.gpsMarker = gps?.toMarkerData(
                SavedMarkers.gpsMarker?.resIcon?:R.drawable.ic_gps_marker,
                SavedMarkers.gpsMarker?.bitmap
            )
    }

    fun deleteAll(){
        focused.forEach {
            markerBitmaps.removeAt(markers.list.indexOf(it))
            markers.remove(it)
            it.remove()
        }
        focused.clear()
        unfocus()
    }

    fun setOnUnfocusListener(value: () -> Unit){
        onUnfocusMarkers = value
    }

    fun setOnUpdateMarkersListener(value: () -> Unit){
        onUpdateMarkers = value
    }

    fun setOnFocusListener(value: () -> Unit){
        onFocusMarker = value
    }

    private fun addNew(position: LatLng) {
        // Add a marker on click and move the camera
        addNew(
            MarkerData(
                lat = position.latitude,
                lon = position.longitude,
                resIcon = R.drawable.ic_default_marker,
                title = "Marker ${markers.list.count()}",
                draggable = true
            )
        )
    }
    private fun addNew(data: MarkerData)
    {
        val position = LatLng(data.lat, data.lon)
        // Add a marker to marker list
        markers.update(mMap.addMarker(
            MarkerOptions()
            .position(position)
            .anchor(0.5f, 0.5f)
        ))
        markerBitmaps.add(data.bitmap)

        update(data)
    }

    private fun update(data: MarkerData) {
        val position = LatLng(data.lat, data.lon)
        val marker = markers.list.find { it.position == position }?: gps
        marker ?: return
        marker.title = data.title
        marker.tag = if(data.resIcon == R.drawable.ic_gps_marker) MarkerType.GPS else MarkerType.DEFAULT
        marker.setIcon(
            data.getIcon(context)
        )
        marker.isDraggable = data.draggable
        if(!marker.isDraggable && marker != gps)
            marker.alpha = MarkerData.DISABLED_ALPHA
        else
            marker.alpha = MarkerData.ENABLED_ALPHA
    }

    private fun unfocus() {
        onUnfocusMarkers?.invoke()
        focused.forEach{it.setIcon(
            bitmapDescriptorFromVector(
                context,
                R.drawable.ic_default_marker
            )
        )}
        focused.clear()
    }
}