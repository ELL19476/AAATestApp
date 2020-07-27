package com.example.aaatestapp.markerlist

import java.io.Serializable

data class MarkerData(
    val resIcon:Int =-1,
    val title:String = "",
    val location: String = ""
): Serializable