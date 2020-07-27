package com.example.aaatestapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aaatestapp.markerlist.ListActivity
import com.example.aaatestapp.markerlist.MarkerData
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToInt

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val REQUEST_CODE_LOCATION_PERMISSION = 1
        private const val THRESHOLD = 10000
    }

    private lateinit var mMap: GoogleMap

    private var markers = ObservableList<Marker>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val permission = android.Manifest.permission.ACCESS_FINE_LOCATION;

    private var polygon: Polygon? = null

    private var locationObserver: CompositeDisposable = CompositeDisposable()
    var lastLocation: Location? = null

    private var gpsMarker: Marker? = null

    private var wasInPolygon = false;

    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fab.setOnClickListener { startActivity(
            Intent(this, ListActivity::class.java).apply {
            putExtra("markers",
                if(gpsMarker != null)
                    (listOf(gpsMarker!!) + markers.list)?.toSerializableArray()
                else
                    markers.list.toSerializableArray()
            )
        })
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        /*
        // Initialize places
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Snackbar.make(maps_parent, "Place: ${place.name}, ID: ${place.id}", Snackbar.LENGTH_SHORT).show()
            }

            override fun onError(p0: Status) {
                Snackbar.make(maps_parent, "An error occurred: ${p0.statusCode}", Snackbar.LENGTH_INDEFINITE).show()
            }
        })
         */
    }

    override fun onPause() {
        super.onPause()
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // add click event to map
        mMap.setOnMapClickListener{ addNewMarker(it) }

        // find location of user
        if(ContextCompat.checkSelfPermission(applicationContext, permission) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(false)
        }
        else{
            bindLocationCheck()
        }

        // update polygon on drag
        mMap.setOnMarkerDragListener(object: GoogleMap.OnMarkerDragListener{
            override fun onMarkerDrag(p0: Marker?) {
                if(p0 != null)
                    updatePolygon(p0)
            }

            override fun onMarkerDragStart(p0: Marker?) {
                if(p0 != null)
                    updatePolygon(p0)            }

            override fun onMarkerDragEnd(p0: Marker?) {
                if(p0 != null)
                    updatePolygon(p0)
            }
        })

        mMap.setOnMarkerClickListener{
            fab.changeIcon(R.drawable.ic_edit)
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.isNotEmpty()){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                bindLocationCheck()
            }
            else if(lastLocation == null)
            {
                if(shouldShowRequestPermissionRationale(permission))
                    requestPermissions()
            }
        }
    }

    private fun bindLocationCheck() {
        // if disposable not null: stop observing
        locationObserver.clear()
        // CHANGE POPUP WHEN GOS MOVES
        locationObserver.add(getCurrentLocation()
            .filter {
                lastLocation == null ||
                // if last location provided isn't better
                !(lastLocation!!.accuracy > it.accuracy && (it.time - lastLocation!!.time) < THRESHOLD)
            }
            .subscribe (
                { updatePopUp(isInPolygon(Vector(it))); lastLocation = it },    // onNext
                { println("received an error $it") }    // onError
            )
        )

        // CHANGE POPUP WHEN MARKERS ARE UPDATED
        locationObserver.add(
            markers.observable
                .filter { lastLocation != null }
                .subscribe {
                updatePopUp(isInPolygon(Vector(lastLocation!!)))
            }
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
                ActivityCompat.requestPermissions(this@MapsActivity, arrayOf(permission), REQUEST_CODE_LOCATION_PERMISSION)
            }
            // don't have permission: show popUp and ask again
            Toast.makeText(this, "Pleeeeeeeease I need this permission. Don't be mean!", Toast.LENGTH_LONG).show()
        }
        else{
            // prompt user
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_LOCATION_PERMISSION)

        }
    }
    // only call this when permission granted
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(): Observable<Location> {

        val locationRequest = LocationRequest()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 3000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // create GPS marker
        val subject =  BehaviorSubject.create<Location>{
            emitter ->
            // initial location
            fusedLocationClient.lastLocation.addOnSuccessListener{
                println("onNext location: $it")
                if(it == null)
                    emitter.onError(NullPointerException())
                else
                    emitter.onNext(it)
            }
            // location updates
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    emitter.onNext(locationResult.lastLocation)
                    println("updated location!!!")
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }.doOnNext{
            gpsMarker = mMap.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude))
                .apply {icon(
                    bitmapDescriptorFromVector(this@MapsActivity, R.drawable.ic_gps_marker))}
                .apply {title("Your location")})
                .apply { tag = MarkerType.GPS }
        }.doOnError {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()   // onError
        }
        return subject
    }

    private fun addNewMarker(position: LatLng) {
        // Add a marker on click and move the camera
        markers.add(mMap.addMarker(MarkerOptions().position(position).draggable(true).
            apply { icon(bitmapDescriptorFromVector(this@MapsActivity, R.drawable.ic_default_marker)) }).
            apply { setAnchor(0.5f, 0.5f) }
            .apply { tag = MarkerType.DEFAULT }
            .apply { title = "Marker ${markers.list.count()}" }
        )

        mMap.moveCamera(CameraUpdateFactory.newLatLng(position))

        updatePolygon(markers.list.last())
    }

    private fun isInPolygon(point: Vector): Boolean
    {
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

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap =
            Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun updatePolygon(marker: Marker){
        // call marker observable
        markers.onAdd.onNext(marker)

        val polygonOptions = PolygonOptions()
        for(elem in markers.list)
        {
            polygonOptions.add(elem.position)
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
}

private fun <E: Marker> List<E>.toSerializableArray(): Array<MarkerData> {
    var list: MutableList<MarkerData> = mutableListOf()
    val defaultM = R.drawable.ic_default_marker
    val gpsM = R.drawable.ic_gps_marker

    forEach {
        list.add(
            MarkerData(
                if(it.tag == MarkerType.DEFAULT) defaultM else gpsM,
                it.title ?: "Default Marker",
                it.position.format()
            )
        )
    }
    return list.toTypedArray()
}

private data class Vector(val x: Double, val y: Double)
{
    constructor(data: LatLng) : this(data.latitude, data.longitude)
    constructor(data: Location) : this(data.latitude, data.longitude)

    operator fun minus(other: Vector) = Vector(x - other.x, y - other.y)
    fun modifiedCross(other: Vector) = x * other.y - y * other.x
}
private fun Polygon.style(context: Context) {
    this.fillColor = ContextCompat.getColor(context, R.color.fillColor)
    this.strokeColor = Color.WHITE
    this.strokeWidth = 10f
    this.isClickable = true
}

class ObservableList<T> {
    val list: MutableList<T> = mutableListOf()
    val onAdd: PublishSubject<T> = PublishSubject.create()
    fun add(value: T) {
        list.add(value)
        onAdd.onNext(value)
    }

    val observable: Observable<T>
        get() = onAdd
}

private fun LatLng.format(): String {
    val degrees = latitude.toInt() to longitude.toInt()
    var rem =  (latitude - degrees.first) * 60 to (longitude - degrees.second) * 60
    val minutes = rem.first.toInt() to rem.second.toInt()
    rem = (rem.first - minutes.first) * 60 to (rem.second - minutes.second) * 60
    val seconds = rem.first.roundToInt() to rem.second.roundToInt()

    return "${degrees.first}° ${minutes.first}' ${seconds.first}'' N ${degrees.second}° ${minutes.second}' ${seconds.second}'' E"
}

