package com.example.aaatestapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aaatestapp.ext.*
import com.example.aaatestapp.ext.Vector
import com.example.aaatestapp.managers.LocationManager
import com.example.aaatestapp.managers.MarkerManager
import com.example.aaatestapp.markerlist.ListActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*
import kotlin.concurrent.schedule

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val REQUEST_CODE_LOCATION_PERMISSION = 1
        private const val PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    }

    private lateinit var mMap: GoogleMap

    private lateinit var locationManager: LocationManager

    private var polygon: Polygon? = null

    private var observers: CompositeDisposable = CompositeDisposable()

    private var wasInPolygon = false

    private var markerManager: MarkerManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationManager = LocationManager(this)

        fab.setOnClickListener { transitionToList() }
    }

    // create an action bar button
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.maps_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // handle button activities
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == R.id.downloadButton) {
            this.asyncDialog(
                title = "Download Markers from",
                items = markerManager?.dataHandler?.loadUsers(),
                dataFilter = ::filterForUser,
                loadingMessage = "loading users...",
                failMessage = "check your internet connection",
                positiveAction = {
                    markerManager?.downloadFromUser(it);
                    polygon?.remove()
                    updatePolygon()
                }
            )
        }
        return super.onOptionsItemSelected(item)
    }

    private fun filterForUser(user: String): String =
        if(user == markerManager?.dataHandler?.deviceName) "$user (You)" else user

    override fun onResume() {
        super.onResume()
        // load saved markers
        markerManager?.load()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // find location of user
        if(ContextCompat.checkSelfPermission(applicationContext, PERMISSION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(false)
        else
            bindChanges()

        markerManager = MarkerManager(this, mMap)

        markerManager?.setOnFocusListener{
            if(markerManager?.focused?.isEmpty() == false) {
                fab.setImageDrawable(getDrawable(R.drawable.ic_delete))
                fab.setOnClickListener { markerManager?.deleteAll() }
            }
        }
        markerManager?.setOnUnfocusListener {
            fab.setImageDrawable(getDrawable(R.drawable.ic_list))
            fab.setOnClickListener { transitionToList() }
        }
        markerManager?.setOnUpdateMarkersListener{
            if(locationManager.lastLocation != null)
            {
                updatePolygon()
                updatePopUp(isInPolygon(Vector(locationManager.lastLocation!!)))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.isNotEmpty()){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                bindChanges()
            else if(locationManager.lastLocation == null)
            {
                if(shouldShowRequestPermissionRationale(PERMISSION))
                    requestPermissions()
            }
        }
    }

    private fun bindChanges() {
        // if disposable not null: stop observing
        observers.clear()
        // CHANGE POPUP WHEN GOS MOVES
        observers.add(
            locationManager.getCurrentLocation()
                .subscribe (
                    {
                        markerManager?.initGpsMarker(it)
                        updatePopUp(isInPolygon(Vector(it)))
                    },    // onNext
                    { println("error: ${it.message}") }    // onError
                )
        )


    }

    private fun updatePopUp(inPolygon: Boolean) {
        if(wasInPolygon == inPolygon)
            return
        wasInPolygon = inPolygon
        if(inPolygon)
        {
            Snackbar.make(maps_parent, R.string.intersection_success, Snackbar.LENGTH_SHORT).show()
        }
        else
        {
            Snackbar.make(maps_parent, R.string.intersection_fail, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissions(showError: Boolean = true) {
        if(showError)
        {
            Timer().schedule(2000) {
                // prompt user again
                ActivityCompat.requestPermissions(this@MapsActivity, arrayOf(PERMISSION), REQUEST_CODE_LOCATION_PERMISSION)
            }
            // don't have permission: show popUp and ask again
            Toast.makeText(this, "Pleeeeeeeease I need this permission. Don't be mean!", Toast.LENGTH_LONG).show()
        }
        else{
            // prompt user
            ActivityCompat.requestPermissions(this, arrayOf(PERMISSION), REQUEST_CODE_LOCATION_PERMISSION)

        }
    }

    private fun isInPolygon(point: Vector): Boolean
    {
        val markers = markerManager?.markers ?: return false
        if(markers.list.count() < 3) return false

        var count  = 0

        val positions = markers.list.map { it.position }
        positions.forEachIndexed { index, latLng ->
            var lastIndex = if(index == 0) positions.lastIndex else index - 1
            if(
                intersects(point, Vector(positions[lastIndex]) to Vector(latLng))
            ) count++
        }

        // is in polygon if ray intersected an uneven amount of times
        return count % 2 != 0
    }

    private fun updatePolygon(){
        val markers = markerManager?.markers ?: return
        if(markers.list.isEmpty()) {
            polygon = null
            return
        }

        val polygonOptions = PolygonOptions()
        markers.list.forEach { marker ->
            polygonOptions.add(marker.position)
        }

        polygon?.remove()
        polygon = mMap.addPolygon(polygonOptions).apply { style(this@MapsActivity) }
    }

    private fun intersects(point: Vector, line: Pair<Vector, Vector>): Boolean{
        // reference: https://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect/565282#565282

        // the point casts a ray upwards
        val ray1 = Vector(0.0, 1.0) // r
        // get the vector for the line
        val ray2: Vector = line.second - line.first // s

        val rayCross = ray1.modifiedCross(ray2) // r x s
        // if parallel they never intersect
        if(rayCross == 0.0) return false

        val pointDelta = line.first - point

        // get value u for function: Point = point + u * ray
        val u = pointDelta.modifiedCross(ray1) / rayCross // (q − p) × r / (r × s)

        // get value t
        val t = pointDelta.modifiedCross(ray2) / rayCross // (q − p) × s / (r × s)

        // u should be between 0 and 1, because it describes a line and not an endless ray; t should be positive
        return u in 0.0..1.0 && t > 0
    }

    private fun transitionToList() {
        markerManager?.save()
        startActivity(
            Intent(this, ListActivity::class.java)
        )
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}

