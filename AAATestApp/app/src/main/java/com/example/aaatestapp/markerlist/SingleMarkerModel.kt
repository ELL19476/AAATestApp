package com.example.aaatestapp.markerlist

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.example.aaatestapp.R
import kotlinx.android.synthetic.main.marker_list_single.view.*

@EpoxyModelClass
abstract class SingleMarkerModel(): EpoxyModelWithHolder<SingleMarkerModel.MarkerHolder>() {

    @EpoxyAttribute
    var markerId: Int = 0

    @EpoxyAttribute
    var resIcon: Int = 0

    @EpoxyAttribute
    var bitmap: Bitmap? = null

    @EpoxyAttribute
    var alpha: Float = 1f

    @EpoxyAttribute
    var title: String = ""

    @EpoxyAttribute
    var location: String = ""

    @EpoxyAttribute
    var onDetailClick: ((Int) -> Unit)? = null

    override fun bind(holder: MarkerHolder) {
        if(bitmap != null)
            holder.imageView.setImageBitmap(bitmap)
        else
            holder.imageView.setImageResource(resIcon)
        holder.titleView.text = title
        holder.locationView.text = location
    }

    inner class MarkerHolder: EpoxyHolder() {

        lateinit var imageView: ImageView
        lateinit var titleView: TextView
        lateinit var locationView: TextView

        override fun bindView(itemView: View) {
            imageView = itemView.ivMarker
            imageView.alpha = alpha
            titleView = itemView.tvTitle
            locationView = itemView.tvLocation

            itemView.setOnClickListener {
                onDetailClick?.invoke(markerId)
            }
        }
    }

    override fun getDefaultLayout() = R.layout.marker_list_single
}