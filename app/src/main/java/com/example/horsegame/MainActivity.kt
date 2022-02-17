package com.example.horsegame

import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var cellSelected_x = 0
    private var cellSelected_y = 0

    private lateinit var board: Array<IntArray>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initScreenGame()
        resetBoard()
        setFirstPosition()
    }

    private fun initScreenGame(){
        setSizeBoard()
        hideMessage()
    }

    private  fun resetBoard(){
        // 0 está libre
        // 1 casilla marcada
        // 2 bonus
        // 9 opcion del movimieto actual
        board = arrayOf(
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0)
        )
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

    private fun setFirstPosition(){
        var x = 0
        var y = 0
        x = (0..7).random()
        y = (0..7).random()
        cellSelected_x = x
        cellSelected_y = y
        selectCell(x,y)
    }

    private fun selectCell(x:Int, y:Int){
        board[x][y] = 1
        paintHorseCell(cellSelected_x,cellSelected_y,"previous_cell")
        cellSelected_x = x
        cellSelected_y = y
        paintHorseCell(x,y,"selected_cell")
    }

    private fun paintHorseCell(x:Int, y:Int, color:String){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y","id",packageName))
        iv.setBackgroundColor(ContextCompat.getColor(this,resources.getIdentifier(color,"color", packageName)))
        iv.setImageResource(R.drawable.ic_horse)
    }
}