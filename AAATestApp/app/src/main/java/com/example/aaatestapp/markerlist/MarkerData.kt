package com.example.aaatestapp.markerlist

import java.io.Serializable

data class MarkerData(
    val lat: Double,
    val lon: Double,
    val resIcon:Int =-1,
    val title:String = "",
    val location: String = ""
): Serializable