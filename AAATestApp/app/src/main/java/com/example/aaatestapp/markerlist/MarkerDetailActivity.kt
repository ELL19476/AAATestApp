package com.example.aaatestapp.markerlist

import SavedMarkers
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aaatestapp.R
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.marker_item_detail.*
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.math.roundToInt

// TODO: SHOW CUSTOM BITMAP if it exists

class MarkerDetailActivity: AppCompatActivity(){

    private var markerIndex: Int = -1

    private var icon: Int? = null
    private var draggable: Boolean? = null

    private var marker: MarkerData? = null

    companion object{
        const val RESULT_LOAD_IMAGE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.marker_item_detail)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        println("Hello from ${this::class.simpleName}")

        markerIndex = intent.getSerializableExtra("id") as Int
        marker = SavedMarkers.markers?.getOrNull(markerIndex)?:SavedMarkers.gpsMarker

        rV_settings.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        buildModel()

        save.setOnClickListener {
            if(draggable != null) marker?.draggable = draggable?:true
            if(icon != null) marker?.resIcon = icon?:-1
            saveTitle()

            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            setResult(Activity.RESULT_OK, null);
            finish()
        }

        imagePicker.setOnClickListener {
            val picker = Intent(Intent.ACTION_GET_CONTENT)
            picker.type = "image/*"
            startActivityForResult(picker, RESULT_LOAD_IMAGE)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED, null);
        finish()
    }

    private fun buildModel() {
        val src = icon?: marker?.resIcon?: -1
        preview_image.setImageResource(src)
        val isGpsMarker = src == R.drawable.ic_gps_marker
        rV_settings.withModels {
            textSwitch {
                id(this.hashCode())
                text(getString(R.string.is_location_marker))
                checked(isGpsMarker)
                onStateChanged { checked ->
                    icon = if(checked) R.drawable.ic_gps_marker else R.drawable.ic_default_marker
                    preview_image.setImageDrawable(getDrawable(icon?:-1))
                }
            }
            if(marker != SavedMarkers.gpsMarker)
                textSwitch {
                    id(this.hashCode())
                    text(getString(R.string.is_draggable))
                    checked(marker?.draggable?:true)
                    onStateChanged { checked ->
                        draggable = checked
                        preview_image.imageAlpha = if(checked) (255 * MarkerData.ENABLED_ALPHA).roundToInt()
                        else (255 * MarkerData.DISABLED_ALPHA).roundToInt()
                    }
                }
        }

        title_text.text = marker?.title
        description.text = marker?.location
        title_input.hint = getString(R.string.edit_title_hint)

        title_holder.setOnClickListener {
            title_input.isVisible = true
            requestFocus(title_input)
            title_holder.visibility = View.INVISIBLE
            updateSaveButton(title_input.editText?.text)
        }
        title_input.editText?.apply {
            addTextChangedListener {
                updateSaveButton(it)
            }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    saveTitle()
                    defocus(title_input)
                }
                true
            }
            this.imeOptions = this.imeOptions or EditorInfo.IME_ACTION_DONE
        }
    }

    private fun updateSaveButton(text: Editable?) {
        save.isEnabled = text?.isNotBlank()?:false
        save.alpha = if(save.isEnabled) 1f else .8f
    }

    private fun requestFocus(inputLayout: TextInputLayout?) {
        inputLayout?.editText?.requestFocus()
        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(inputLayout?.editText, InputMethodManager.SHOW_IMPLICIT)
    }
    private fun defocus(inputLayout: TextInputLayout?){
        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(inputLayout?.editText?.windowToken, 0)
    }

    private fun saveTitle() {
        title_holder.visibility = View.VISIBLE
        title_input.isVisible = false

        val input = title_input.editText?.text
        if(input?.isNotBlank() == true)
        {
            title_text.text = input
            marker?.title = input.toString()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                val imageUri: Uri = data.data
                val imageStream: InputStream = contentResolver.openInputStream(imageUri)
                val selectedImage = BitmapFactory.decodeStream(imageStream)

                ivUpload.setImageBitmap(selectedImage)
                marker?.bitmap = selectedImage

                ivUpload.isVisible = true
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show()
        }
    }
}