package com.example.aaatestapp.networking

import android.util.Log.VERBOSE
import com.example.aaatestapp.markerlist.MarkerData
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface MarkerApiService {
    @GET("aaa.php")
    fun getMarkerData(
        @Query("user") user: String
    ): Single<Array<MarkerData>>

    @GET("aaa.php")
    fun saveMarkerData(
        @Query("user") user: String,
        @Query("markers") markers: String
    ): Single<Int>

    @GET("aaa.php?getUsers=true")
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
                .baseUrl("http://samuel.ellmauer.eu/")
                .addCallAdapterFactory(
                    RxJava2CallAdapterFactory.createAsync())
                .addConverterFactory(
                    GsonConverterFactory.create())
                .client(client)
                .build()

            return retrofit.create(MarkerApiService::class.java)
}
}
}