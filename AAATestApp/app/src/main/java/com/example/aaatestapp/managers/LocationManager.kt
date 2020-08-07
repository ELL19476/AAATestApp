package com.example.aaatestapp.managers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class LocationManager (val activity: Activity) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    var lastLocation: Location? = null

    // only call this when permission granted
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(): Observable<Location> {

        val locationRequest = LocationRequest()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 3000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

        // create GPS marker
        return BehaviorSubject.create<Location>{
                emitter ->
            // initial location
            fusedLocationClient.lastLocation.addOnSuccessListener{
                println("onNext location: $it")
                if(it != null)
                    emitter.onNext(it)
            }
            // location updates
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    emitter.onNext(locationResult.lastLocation)
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }.doOnNext{ lastLocation = it }
    }
}