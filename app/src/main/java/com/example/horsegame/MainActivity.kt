package com.example.horsegame

import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initScreenGame()
    }

    private fun initScreenGame(){
        setSizeBoard()
        hideMessage()
    }

    private fun setSizeBoard(){
        var iv: ImageView
        //save display
        val display = windowManager.defaultDisplay
        //catch screen width
        val size = Point()
        display.getSize(size)
        val width = size.x
        //transform width in dp
        var width_dp = (width/getResources().getDisplayMetrics().density)
        //margins are zero because it is the total width
        var lateralMarginDP = 0
        val width_cell = (width_dp-lateralMarginDP)/8
        val height_cell = width_cell

        for(i in 0..7) {
            for (j in 0..7) {
                iv = findViewById(resources.getIdentifier("c$i$j","id",packageName))
                //establish width & height transformed in the dimension I have to apply
                //transform to dp height_cell/width_cell taking into account the screen, and send that value to the parent layout
                var height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height_cell,getResources().getDisplayMetrics()).toInt()
                var widthF = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width_cell,getResources().getDisplayMetrics()).toInt()
                iv.setLayoutParams(TableRow.LayoutParams(widthF,height))
            }
        }
    }

    private fun hideMessage(){
        var lyMessage = findViewById<LinearLayout>(R.id.lyMessage)
        lyMessage.visibility = View.INVISIBLE
    }
}