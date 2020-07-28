package com.example.aaatestapp.markerlist

import android.view.View
import com.airbnb.epoxy.*
import com.example.aaatestapp.R
import kotlinx.android.synthetic.main.item_text_switch.view.*

@EpoxyModelClass
abstract class TextSwitchModel : EpoxyModelWithHolder<TextSwitchModel.Holder>() {

    @EpoxyAttribute
    lateinit var text: String

    @EpoxyAttribute
    var checked = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onStateChanged: (Boolean) -> Unit = {}

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.itemView.tvItemTextSwitch.text = text
        holder.itemView.swItemTextSwitch.isChecked = checked
        holder.itemView.swItemTextSwitch.setOnCheckedChangeListener { _, isChecked -> changeCheck(isChecked) }
        holder.itemView.setOnClickListener { changeCheck(); holder.itemView.swItemTextSwitch.isChecked = checked }
    }

    private fun changeCheck(isChecked: Boolean? = null) {
        checked = isChecked?:!checked
        onStateChanged.invoke(checked)
    }

    override fun unbind(holder: Holder) {
        holder.itemView.swItemTextSwitch.setOnCheckedChangeListener(null)
        super.unbind(holder)
    }

    inner class Holder: EpoxyHolder() {
        lateinit var itemView: View

        override fun bindView(itemView: View) {
            this.itemView = itemView
        }
    }

    override fun getDefaultLayout() = R.layout.item_text_switch
}