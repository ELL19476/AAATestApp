package com.example.aaatestapp.markerlist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyController
import com.example.aaatestapp.MapsActivity
import com.example.aaatestapp.R
import kotlinx.android.synthetic.main.activity_marker_list.*
import kotlin.math.absoluteValue


class ListActivity: AppCompatActivity(){

    private lateinit var markers: Array<MarkerData>
    private val gps: MarkerData? = SavedMarkers.gpsMarker

    private var isSorted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_list)

        markers = SavedMarkers.markers?: arrayOf()
        println("markers count received: ${markers.count()}")

        initRecycler()

        fab.setOnClickListener {
            sortMarkers()
        }
    }

    private fun sortMarkers() {
        if(isSorted)
        {
            markers.sortBy{ it.title }
            Toast.makeText(this, "sort by name", Toast.LENGTH_SHORT).show()
        }
        else if(gps != null)
        {
            markers.sortBy {m ->
                ((gps.lat- m.lat) * (gps.lon - m.lon)).absoluteValue
            }
            Toast.makeText(this, "sort by distance", Toast.LENGTH_SHORT).show()
        }

        isSorted = !isSorted

        buildModel()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MapsActivity::class.java))
        finish()
    }

    private fun initRecycler() {
        rV_marker_list.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        buildModel()
    }

    private fun buildModel() {
        rV_marker_list.withModels{

            if(gps != null)
                singleMarker {
                    id(gps.hashCode())
                    markerId(-1)
                    resIcon(gps.resIcon)
                    title(gps.title)
                    location(gps.location)
                    onDetailClick(::detailActivity)
                }

            markers.forEachIndexed { index, markerData ->
                singleMarker {
                    id(markerData.hashCode())
                    markerId(index)
                    resIcon(markerData.resIcon)
                    title(markerData.title)
                    location(markerData.location)
                    onDetailClick(::detailActivity)
                }
            }
        }
    }

    private fun detailActivity(id: Int){
        startActivity(
            Intent(this, MarkerDetailActivity::class.java)
                .apply { putExtra("id", id) })
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }
}