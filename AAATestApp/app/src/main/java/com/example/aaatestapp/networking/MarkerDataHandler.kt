package com.example.aaatestapp.networking

import android.content.ContentResolver
import android.provider.Settings
import com.example.aaatestapp.markerlist.MarkerData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MarkerDataHandler(contentResolver: ContentResolver) {
    private val markerApiService by lazy {
        MarkerApiService.create()
    }
    private var disposables = CompositeDisposable()

    val deviceName = Settings.Secure.getString(contentResolver, "device_name")?:
    Settings.Secure.getString(contentResolver,
        Settings.Secure.ANDROID_ID)

    fun loadMarkers(user: String? = null, onLoad: (Array<MarkerData>?) -> Unit) {
        disposables.add(
            markerApiService.getMarkerData(user?:deviceName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe (
                { response -> onLoad(response) },
                { onLoad(null); println("Error ${it.message}") })
            )
    }

    fun saveMarkers(json: String, user: String = deviceName, onResponse: (Boolean) -> Unit) {
        disposables.add(
            markerApiService.saveMarkerData(user, json)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe (
                    { response -> if(response == 0) onResponse(false) else onResponse(true) },
                    { onResponse(false) }
                )
        )
    }

    fun loadUsers() = markerApiService.getUsers()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError { println(it) }
}