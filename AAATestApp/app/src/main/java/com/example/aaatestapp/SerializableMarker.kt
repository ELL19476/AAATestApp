package com.example.aaatestapp

import java.io.Serializable

class SerializableMarker(val tile: String, val latitude: Double, val longitude: Double, val tag: Int):
    Serializable
