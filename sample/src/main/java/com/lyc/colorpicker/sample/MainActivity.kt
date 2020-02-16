package com.lyc.colorpicker.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lyc.colorpicker.HueBarView
import com.lyc.colorpicker.SVColorPickerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pv.setColorPickerViewListener(object : SVColorPickerView.ColorPickerViewListener {
            override fun onColorChange(newColor: Int, colorHSV: FloatArray) {
                view_color.setBackgroundColor(newColor)
            }
        })
        hbv.currentHue = pv.hue
        hbv.setOnHueBarChangeListener(object : HueBarView.OnHueBarChangeListener {
            override fun onHueChange(newHue: Float) {
                pv.hue = newHue
            }
        })
        view_color.setBackgroundColor(pv.getCurrentColor())
        if (savedInstanceState == null) {
            val initColor = 0xFFE64A19.toInt()
            pv.setCurrentColor(initColor)
        }
    }
}
