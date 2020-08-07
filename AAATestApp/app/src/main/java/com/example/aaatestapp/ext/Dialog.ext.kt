package com.example.aaatestapp.ext

import android.content.Context
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.example.aaatestapp.R
import io.reactivex.Single
import io.reactivex.disposables.Disposable

fun Context.asyncDialog(
    title: String? = null,
    items: Single<Array<String>?>? = null,
    dataFilter: ((String) -> String)? = null,
    loadingMessage: String? = null,
    failMessage: String? = null,
    @StringRes negativeButton: Int = R.string.global_button_cancel,
    positiveAction: (String?) -> Unit = {},
    negativeAction: () -> Unit = {},
    cancelable: Boolean = true
) {
    val builder = AlertDialog.Builder(this)

    var disposable: Disposable? = null
    var itemValues: Array<String>? = null
    val adapter: ArrayAdapter<String> = ArrayAdapter(this, R.layout.simple_spinner_item, R.id.tV_spinner_item, arrayListOf(loadingMessage?:""))

    builder.setCancelable(cancelable)
    if (title != null) builder.setTitle(title)
    if (negativeButton != 0) builder.setNegativeButton(negativeButton) { _, _ -> negativeAction(); disposable?.dispose() }
    builder.setAdapter(adapter){
        _, index -> positiveAction(itemValues?.getOrNull(index)); disposable?.dispose()
    }

    val dialog = builder.show()
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

    disposable = items?.subscribe { data ->
        if(data == null) dialog.setMessage(failMessage)
        else {
            adapter.clear()
            itemValues = data
            adapter.addAll(data.map(dataFilter?:{ it }).toMutableList())
            (dialog.listView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        }
    }
}