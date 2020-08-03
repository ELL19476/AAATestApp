package com.example.aaatestapp.markerlist

import SavedMarkers
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.aaatestapp.R
import com.example.aaatestapp.networking.MarkerDataHandler
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_marker_list.*
import kotlin.math.absoluteValue


class ListActivity: AppCompatActivity(){

    private lateinit var markers: Array<MarkerData>
    private val gps: MarkerData? = SavedMarkers.gpsMarker

    private var isSorted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_list)

        markers = SavedMarkers.markers?.copyOf()?: arrayOf()
        println("markers count received: ${markers.count()}")

        initRecycler()

        fab.setOnClickListener {
            sortMarkers()
        }
    }

    // create an action bar button
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.list_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // handle button activities
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == R.id.shareButton) {
            uploadMarkers()
        }
        return super.onOptionsItemSelected(item)
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

    private fun uploadMarkers() {
        val savedMarkers = SavedMarkers.markers
        if(savedMarkers != null && savedMarkers.count() > 0)
        {
            Toast.makeText(this, getString(R.string.upload_start), Toast.LENGTH_LONG).show()

            val json = Gson().toJson(savedMarkers);
            MarkerDataHandler(contentResolver).saveMarkers(json = json ) {
                // todo show more user-friendly message
                Toast.makeText(this, if(it)R.string.upload_success else R.string.upload_fail, Toast.LENGTH_LONG).show()
            }
        }
        else{
            Toast.makeText(this, getString(R.string.upload_reject), Toast.LENGTH_LONG).show()
        }
    }
}