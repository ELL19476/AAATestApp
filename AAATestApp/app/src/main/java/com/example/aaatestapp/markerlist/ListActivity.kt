package com.example.aaatestapp.markerlist

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyController
import com.example.aaatestapp.R
import kotlinx.android.synthetic.main.activity_marker_list.*
import kotlin.math.absoluteValue


class ListActivity: AppCompatActivity(){

    private lateinit var markers: Array<MarkerData>
    private lateinit var gps: MarkerData

    private var isSorted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_list)

        markers = intent.getSerializableExtra("markers") as Array<MarkerData>
        println("markers count recived: ${markers.count()}")
        gps = markers.last()

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
        else
        {
            markers.sortByDescending {m ->
                ((gps.lat - m.lat) * (gps.lon - m.lon)).absoluteValue
            }
            Toast.makeText(this, "sort by distance", Toast.LENGTH_SHORT).show()
        }

        isSorted = !isSorted

        buildModel()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish();
    }

    private fun initRecycler() {
        rV_marker_list.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        buildModel()
    }

    private fun buildModel() {
        rV_marker_list.withModels{
            markers.forEach {
                singleMarker {
                    id(it.hashCode())
                    resIcon(it.resIcon)
                    title(it.title)
                    location(it.location)
                }
            }
        }
    }
}