package com.example.aaatestapp.markerlist

data class MarkerData(
    var lat: Double,
    var lon: Double,
    var resIcon:Int =-1,
    var title:String = "",
    var location: String = "",
    var draggable: Boolean = true
)