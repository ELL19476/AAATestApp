package com.example.aaatestapp.markerlist

import SavedMarkers
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.aaatestapp.R
import com.example.aaatestapp.ext.scaleToIcon
import com.example.aaatestapp.networking.MarkerDataHandler
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_marker_list.*
import java.io.ByteArrayOutputStream
import kotlin.math.absoluteValue


// DONE: SET IMAGE TO CUSTOM DRAWABLE if it exists - check
// DONE: SCALE CUSTOM BITMAP - check

class ListActivity: AppCompatActivity(){

    private lateinit var markers: Array<MarkerData>
    private val gps: MarkerData? = SavedMarkers.gpsMarker

    private var isSorted = false

    private lateinit var sortToast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_list)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        markers = SavedMarkers.markers?.copyOf()?: arrayOf()

        initRecycler()

        fab.setOnClickListener {
            sortMarkers()
        }

        sortToast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LAUNCH_MARKER_DETAIL) {
            // if saved markers, restart activity
            if (resultCode == Activity.RESULT_OK) {
                startActivity(Intent(this, ListActivity::class.java))
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // create an action bar button
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.list_activity_menu, menu)
        super.onCreateOptionsMenu(menu)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        return true
    }

    // handle button activities
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.shareButton -> uploadMarkers()
            android.R.id.home -> finish()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun sortMarkers() {
        if(isSorted)
        {
            markers.sortBy{ it.title }
            sortToast.run {
                setText(R.string.sort_by_name)
                show()
            }
        }
        else if(gps != null)
        {
            markers.sortBy {m ->
                ((gps.lat- m.lat) * (gps.lon - m.lon)).absoluteValue
            }
            sortToast.run {
                setText(getString(R.string.sort_by_distance))
                show()
            }
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
                    if(gps.bitmap != null) bitmap(gps.bitmap?.scaleToIcon(this@ListActivity))
                    else resIcon(gps.resIcon)
                    title(gps.title)
                    location(gps.location)
                    onDetailClick(::detailActivity)
                }

            markers.forEachIndexed { index, markerData ->
                singleMarker {
                    id(markerData.hashCode())
                    markerId(index)
                    if(markerData.bitmap != null) bitmap(markerData.bitmap?.scaleToIcon(this@ListActivity))
                    else resIcon(markerData.resIcon)
                    alpha(if(markerData.draggable) MarkerData.ENABLED_ALPHA else MarkerData.DISABLED_ALPHA)
                    title(markerData.title)
                    location(markerData.location)
                    onDetailClick(::detailActivity)
                }
            }
        }
    }

    private fun detailActivity(id: Int){
        startActivityForResult(
            Intent(this, MarkerDetailActivity::class.java)
                .apply { putExtra("id", id) }, LAUNCH_MARKER_DETAIL)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun uploadMarkers() {
        // deepcopy saved markers
        val savedMarkers = Gson().toJson(SavedMarkers.markers)
            .run { Gson().fromJson(this, Array<MarkerData>::class.java) }

        if(savedMarkers?.count()?:0 > 0)
        {
            val uploadHandler = MarkerDataHandler(contentResolver)

            val uploadMessage = Snackbar.make(clList_parent, R.string.upload_start, Snackbar.LENGTH_INDEFINITE)
            uploadMessage.show()

            // UPLOAD BITMAPS
            val images = mutableListOf<ByteArray?>()
            val stream = ByteArrayOutputStream()

            savedMarkers?.forEach {
                if(it.bitmap != null) {
                    it.bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    images.add(stream.toByteArray())
                }
                else {
                    images.add(null)
                }
            }
            uploadHandler.saveIcons(images.toTypedArray()) {
                println("SUCCESS? $it")
            }

            // REMOVE BITMAPS FOR MARKER DATA UPLOAD
            savedMarkers?.forEach { it.bitmap = null }

            // UPLOAD MARKER DATA WITHOUT BITMAP
            val json = Gson().toJson(savedMarkers)
            uploadHandler.saveMarkers(json = json ) {
                uploadMessage.dismiss()
                Snackbar.make(clList_parent,
                    if(it)R.string.upload_success else R.string.upload_fail,
                    Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(R.color.colorPrimary))
                    .show()
            }
        }
        else {
            Snackbar.make(clList_parent, R.string.upload_reject, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.colorAccent))
                .show()
        }
    }

    companion object{
        const val LAUNCH_MARKER_DETAIL: Int = 666
    }
}
