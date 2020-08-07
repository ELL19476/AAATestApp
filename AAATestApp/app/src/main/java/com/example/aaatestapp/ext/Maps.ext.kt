package com.example.aaatestapp.ext

import android.content.Context
import android.graphics.Color
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.aaatestapp.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlin.math.roundToInt

data class Vector(val x: Double, val y: Double)
{
    constructor(data: LatLng) : this(data.latitude, data.longitude)
    constructor(data: Location) : this(data.latitude, data.longitude)

    operator fun minus(other: Vector) =
        Vector(x - other.x, y - other.y)
    fun modifiedCross(other: Vector) = x * other.y - y * other.x
}
fun Polygon.style(context: Context) {
    this.fillColor = ContextCompat.getColor(context,
        R.color.fillColor
    )
    this.strokeColor = Color.WHITE
    this.strokeWidth = 10f
    this.isClickable = true
}

class ObservableList<T> {
    val list: MutableList<T> = mutableListOf()
    private val onChange: PublishSubject<T> = PublishSubject.create()

    fun update(value: T) {
        if(!list.contains(value))
            list.add(value)
        onChange.onNext(value)
    }
    fun remove(value: T) {
        list.remove(value)
        onChange.onNext(value)
        println("list count ${list.count()}")
    }

    val observable: Observable<T>
        get() = onChange
}

fun LatLng.format(): String {
    val degrees = latitude.toInt() to longitude.toInt()
    var rem =  (latitude - degrees.first) * 60 to (longitude - degrees.second) * 60
    val minutes = rem.first.toInt() to rem.second.toInt()
    rem = (rem.first - minutes.first) * 60 to (rem.second - minutes.second) * 60
    val seconds = rem.first.roundToInt() to rem.second.roundToInt()

    return "${degrees.first}° ${minutes.first}' ${seconds.first}'' N ${degrees.second}° ${minutes.second}' ${seconds.second}'' E"
}