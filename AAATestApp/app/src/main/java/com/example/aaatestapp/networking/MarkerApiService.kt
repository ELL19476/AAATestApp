package com.example.aaatestapp.networking

import android.util.Log.VERBOSE
import com.example.aaatestapp.markerlist.MarkerData
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface MarkerApiService {
    @GET(UPLOAD_URL)
    fun getMarkerData(
        @Query("user") user: String
    ): Single<Array<MarkerData>?>

    @GET(UPLOAD_URL)
    fun saveMarkerData(
        @Query("user") user: String,
        @Query("markers") markers: String
    ): Single<Int>

    @Multipart
    @POST(IMAGE_UPLOAD_URL)
    fun saveMarkerIcon(
        @Part("user") user: String,
        @Part("markerIndex") index: Int,
        @Part image: MultipartBody.Part
    ): Observable<Int>

    @GET("$UPLOAD_URL?getUsers=true")
    fun getUsers(
    ): Single<Array<String>?>

    companion object{
        fun create(): MarkerApiService {
            val client = OkHttpClient
                .Builder()
                .addInterceptor(LoggingInterceptor.Builder()
                    .setLevel(Level.BASIC)
                    .log(VERBOSE)
                    .build())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(
                    RxJava2CallAdapterFactory.createAsync())
                .addConverterFactory(
                    GsonConverterFactory.create())
                .client(client)
                .build()

            return retrofit.create(MarkerApiService::class.java)
        }

        const val BASE_URL = "http://samuel.ellmauer.eu/"
        const val DIRECTORY = "aaa"
        private const val UPLOAD_URL = "$DIRECTORY/aaa.php"
        private const val IMAGE_UPLOAD_URL = "$DIRECTORY/saveIcon.php"
    }
}