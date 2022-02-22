package com.example.horsegame

import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var mHandler: Handler? = null
    private var timeInSeconds: Long = 0
    private var playing = true

    private var width_bonus = 0

    private var cellSelected_x = 0
    private var cellSelected_y = 0

    private var levelMoves = 64
    private var movesRequired = 4
    private var moves = 64
    private var options = 0
    private var bonus = 0

    private var checkMovement = true

    private var nameColorBlack = "black_cell"
    private var nameColorWhite = "white_cell"

    private lateinit var board: Array<IntArray>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initScreenGame()
        startGame()
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

        width_bonus = 2 * width_cell.toInt()

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

    fun checkCellClicked(v: View) {
        var name = v.tag.toString()
        var x = name.subSequence(1,2).toString().toInt()
        var y = name.subSequence(2,3).toString().toInt()
        checkCell(x,y)
    }
    private fun checkCell(x:Int, y:Int){
        var checkTrue = true
        if(checkMovement){
            var dif_x = x- cellSelected_x
            var dif_y = y - cellSelected_y

            checkTrue = false
            if (dif_x == 1 && dif_y == 2) checkTrue = true
            if (dif_x == 1 && dif_y == -2) checkTrue = true
            if (dif_x == 2 && dif_y == 1) checkTrue = true
            if (dif_x == 2 && dif_y == -1) checkTrue = true
            if (dif_x == -1 && dif_y == 2) checkTrue = true
            if (dif_x == -1 && dif_y == -2) checkTrue = true
            if (dif_x == -2 && dif_y == 1) checkTrue = true
            if (dif_x == -2 && dif_y == -1) checkTrue = true
        }
        else{
            if(board[x][y] != 1){
                bonus--
                var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
                tvBonusData.text = " + $bonus"
                if(bonus == 0)
                    tvBonusData.text = ""
            }
        }

        if(board[x][y] == 1) checkTrue = false

        if(checkTrue) selectCell(x,y)
    }
    private fun selectCell(x:Int, y:Int){
        moves--
        var tvMovesData = findViewById<TextView>(R.id.tvMovesData)
        tvMovesData.text = moves.toString()

        growProgressBonus()

        if(board[x][y] == 2) {
            bonus++
            var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
            tvBonusData.text = " + ${bonus}"
        }

        board[x][y] = 1
        paintHorseCell(cellSelected_x,cellSelected_y,"previous_cell")

        cellSelected_x = x
        cellSelected_y = y

        clearOptions()

        paintHorseCell(x,y,"selected_cell")
        checkMovement = true
        checkOption(x,y)

        if(moves > 0) {
            checkNewBonus()
            checkGameOver()
        }
        else
            showMessage ("You win", "Next level", false)
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
    private fun clearBoard(){
        var iv: ImageView
        var colorBlack = ContextCompat.getColor(this, resources.getIdentifier(nameColorBlack, "color", packageName))
        var colorWhite = ContextCompat.getColor(this, resources.getIdentifier(nameColorWhite, "color", packageName))
        for (i in 0..7){
            for (j in 0..7){
                iv = findViewById(resources.getIdentifier("c$i$j", "id", packageName))
                iv.setImageResource(R.drawable.ic_horse)
                iv.setImageResource(0)

                if (checkColorCell(i,j) == "black")
                    iv.setBackgroundColor(colorBlack)
                else
                    iv.setBackgroundColor(colorWhite)
            }
        }
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

    private fun checkNewBonus(){
        if(moves % movesRequired == 0) {
            var bonusCell_x = 0
            var bonusCell_y = 0

            var bonusCell = false
            while (!bonusCell){
                bonusCell_x = (0..7).random()
                bonusCell_y = (0..7).random()
                if(board[bonusCell_x][bonusCell_y] == 0) bonusCell = true
            }
            board[bonusCell_x][bonusCell_y] = 2
            paintBonusCell(bonusCell_x, bonusCell_y)
        }
    }
    private fun paintBonusCell(x:Int, y:Int){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y","id",packageName))
        iv.setImageResource(R.drawable.ic_bonus)
    }
    private fun growProgressBonus(){
        var moves_done = levelMoves - moves
        var bonus_done = moves_done / movesRequired
        var moves_reset = movesRequired * bonus_done
        var bonus_grow = moves_done - moves_reset

        var v = findViewById<View>(R.id.vNewBonus)
        var widthBonus = ((width_bonus/movesRequired) * bonus_grow).toFloat()

        var height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        var width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthBonus, resources.displayMetrics).toInt()
        v.setLayoutParams(TableRow.LayoutParams(width,height))
    }

    private fun clearOption(x:Int, y:Int){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y","id",packageName))
        //black cell
        if (checkColorCell(x,y) == "black")
            iv.setBackgroundColor(ContextCompat.getColor(this,resources.getIdentifier(nameColorBlack,"color", packageName)))
        //white cell
        else
            iv.setBackgroundColor(ContextCompat.getColor(this,resources.getIdentifier(nameColorWhite,"color", packageName)))
        //horse
        if(board[x][y] == 1)
            iv.setBackgroundColor(ContextCompat.getColor(this,resources.getIdentifier("previous_cell","color", packageName)))

    }
    private fun clearOptions(){
        for(i in 0..7){
            for(j in 0..7){
                if(board[i][j] == 9 || board[i][j] == 2){
                    if(board[i][j] == 9)
                        board[i][j] = 0
                    clearOption(i,j)
                }
            }
        }
    }
    private fun paintOption(x:Int, y:Int){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y","id",packageName))
        if(checkColorCell(x,y) == "black") iv.setBackgroundResource(R.drawable.option_black)
        else iv.setBackgroundResource(R.drawable.option_white)
    }
    private fun paintAllOptions(){
        for(i in 0..7){
            for(j in 0..7){
                if(board[i][j] != 1)
                    paintOption(i,j)
                if(board[i][j] == 0)
                    board[i][j] = 9
            }
        }
    }

    private fun checkGameOver(){
        if (options == 0){
            if (bonus > 0){
                checkMovement = false
                paintAllOptions()
            }
            else
                showMessage ("Game Over", "Try again", true)

        }
    }
    private fun showMessage(title:String, action:String, gameOver:Boolean){
        playing = false
        var lyMessage = findViewById<LinearLayout>(R.id.lyMessage)
        lyMessage.visibility = View.VISIBLE
        var tvTitleLevel = findViewById<TextView>(R.id.tvTitleMessage)
        tvTitleLevel.text = title
        var score: String = ""
        var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
        if(gameOver){
            score = "Score: "+ (levelMoves - moves) + "/" + levelMoves
        }
        else{
            score = tvTimeData.text.toString()
        }
        var tvScoreMessage = findViewById<TextView>(R.id.tvScoreMessage)
        tvScoreMessage.text = score
        var tvAction = findViewById<TextView>(R.id.tvAction)
        tvAction.text = action

    }

    private fun checkOption(x:Int, y:Int){
        options = 0
        checkMove(x,y,1,2)      //1 right, 2 up
        checkMove(x,y,2,1)      //2 right, 1 up
        checkMove(x,y,1,-2)     //1 right, 2 bottom
        checkMove(x,y,2,-1)     //2 right, 1 bottom
        checkMove(x,y,-1,2)     //1 left, 2 up
        checkMove(x,y,-2,1)     //2 left, 1 up
        checkMove(x,y,-1,-2)    //1 left, 2 bottom
        checkMove(x,y,-2,-1)    //2 left, 1 bottom

        var tvOptionsData = findViewById<TextView>(R.id.tvOptionsData)
        tvOptionsData.text = options.toString()
    }
    private fun checkMove(x:Int, y:Int, move_x:Int, move_y:Int){
        var option_x = x + move_x
        var option_y = y + move_y
        //check if the move is not out of the board
        if (option_x < 8 && option_y < 8 && option_x >= 0 && option_y >= 0)
            if(board[option_x][option_y] == 0 || board[option_x][option_y] == 2){
                options++
                paintOption(option_x,option_y)
                if (board[option_x][option_y] == 0)
                    board[option_x][option_y] = 9
            }

    }
    private fun checkColorCell(x:Int, y:Int):String{
        var color = "white"
        var blackColumn_x = arrayOf(0,2,4,6)
        var blackRow_x = arrayOf(1,3,5,7)
        if ((blackColumn_x.contains(x) && blackColumn_x.contains(y))
            || (blackRow_x.contains(x) && blackRow_x.contains(y)))
            color = "black"
        return color
    }

    private fun paintHorseCell(x:Int, y:Int, color:String){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y","id",packageName))
        iv.setBackgroundColor(ContextCompat.getColor(this,resources.getIdentifier(color,"color", packageName)))
        iv.setImageResource(R.drawable.ic_horse)
    }

    private fun resetTime() {
        mHandler?.removeCallbacks(chronometer)
        timeInSeconds = 0
    }
    private fun startTime(){
        mHandler = Handler(Looper.getMainLooper())
        chronometer.run()

        var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
        tvTimeData.text = "00:00"
    }
    private var chronometer: Runnable = object: Runnable{
        override fun run() {
            try {
                if(playing){
                    timeInSeconds++
                    updateStopWatchView(timeInSeconds)
                }

            }
            finally {
                mHandler!!.postDelayed(this, 1000L)
            }
        }
    }
    private fun updateStopWatchView(timeInSeconds: Long){
        val formattedTime = getFormattedStopWatch(timeInSeconds * 1000)
        var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
        tvTimeData.text = formattedTime
    }
    private fun getFormattedStopWatch(ms: Long): String{
        var milliseconds = ms
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
        return "${if (minutes < 10) "0" else ""}$minutes:"+
                "${if (seconds < 10) "0" else ""}$seconds"
    }
    private fun startGame(){
        playing = true
        resetBoard()
        clearBoard()
        setFirstPosition()

        resetTime()
        startTime()
    }
}