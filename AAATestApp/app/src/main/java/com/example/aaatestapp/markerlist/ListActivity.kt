package com.example.aaatestapp.markerlist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.aaatestapp.R
import kotlinx.android.synthetic.main.activity_marker_list.*


class ListActivity: AppCompatActivity(){

    private lateinit var markers: Array<MarkerData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_list)

        markers = intent.getSerializableExtra("markers") as Array<MarkerData>
        println("markers count recived: ${markers.count()}")

        initRecycler()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish();
    }

    private fun initRecycler() {
        rV_marker_list.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        rV_marker_list.withModels{
            markers.forEach {
                singleMarker {
                    id(it.hashCode())
                    resIcon(it.resIcon)
                    title(it.title)
                    location(it.location)
                }
            }
        }
    }
}