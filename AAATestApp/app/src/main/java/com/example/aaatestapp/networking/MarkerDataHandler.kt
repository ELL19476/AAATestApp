package com.example.aaatestapp.networking

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import com.example.aaatestapp.markerlist.MarkerData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody


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
                { response ->
                    if(response == 0)
                        onResponse(false)
                    else
                        onResponse(true)
                },
                { onResponse(false) }
            )
        )
    }

    // save icons to image urls e.g.: /aaa.php/micon-username-0.jpg
    fun saveIcons(images: Array<ByteArray?>, user: String = deviceName, onResponse: (Boolean) -> Unit)
    {
        fun uploadIcon(index: Int)
        {
            val bytes = images.getOrElse(index){ return@uploadIcon}
            if(bytes == null)
            {
                uploadIcon(index + 1)
                return
            }
            val file: RequestBody =
                bytes.toRequestBody("image".toMediaTypeOrNull(), 0, bytes.size)

            val image = MultipartBody.Part.createFormData(
                "image",
                "$ICON_PREFIX-$user-$index$EXT",
                file
            )

            disposables.add(
                markerApiService.saveMarkerIcon(user, index, image)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe (
                        { response ->
                            if(response == 0)
                                onResponse(false)
                            else {
                                if(index == images.lastIndex)
                                    onResponse(true)
                                else
                                    uploadIcon(index + 1)
                            }
                        },
                        { onResponse(false) }
                    )
            )
        }
        // start upload
        uploadIcon(0)
    }

    fun loadUsers() = markerApiService.getUsers()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError { println(it) }

    companion object{
        const val ICON_PREFIX = "micon"
        const val EXT = ".jpeg"
        const val BITMAP_SIZE = 128
    }
}
