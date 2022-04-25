package com.example.horsegame

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.Screenshot.capture
import com.google.android.gms.ads.MobileAds
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var bitmap: Bitmap?= null

    private var mHandler: Handler? = null
    private var timeInSeconds: Long = 0
    private var playing = true
    private var string_share = ""

    private var width_bonus = 0

    private var cellSelected_x = 0
    private var cellSelected_y = 0

    private var nextLevel= false
    private var level = 1
    private var scoreLevel = 1
    private var levelMoves = 0
    private var movesRequired = 0
    private var moves = 0
    private var lives = 1
    private var score_lives = 1

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
        initAds()
        startGame()
    }

    private fun initAds(){
        MobileAds.initialize(this)
    }
    private fun initScreenGame(){
        setSizeBoard()
        hideMessage(false)
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
    private fun hideMessage(start: Boolean){
        var lyMessage = findViewById<LinearLayout>(R.id.lyMessage)
        lyMessage.visibility = View.INVISIBLE
        if(start) startGame()
    }

    fun launchShareGame(v: View){
        shareGame()
    }
    private fun shareGame(){
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        var scCapture: ScreenCapture = capture(this)
        bitmap = scCapture.bitmap

        if (bitmap != null){
            var idGame = SimpleDateFormat("yyyy/MM/dd").format(Date())
            idGame = idGame.replace(":", "")
            idGame = idGame.replace("/", "")
            //save image
            val path = saveImage(bitmap, "${idGame}.jpg")
            //get identify resource in order to manage in the OS
            //necessary if i handle files
            val bitmapURI = Uri.parse(path)

            //create an intent - type action send
            val shareIntent = Intent(Intent.ACTION_SEND)
            //create new task because send an image to another app is a new task
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            //say what i am sending
            shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapURI)
            //reference to a phrase we send the image
            shareIntent.putExtra(Intent.EXTRA_TEXT, string_share)
            shareIntent.type = "image/png"

            //intent to select the app where you want to share your results
            val finalShareIntent = Intent.createChooser(shareIntent, "Select the app you want to share the game to")
            finalShareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(finalShareIntent)
        }
    }

    private fun saveImage(bitmap: Bitmap?, fileName: String): String? {
        if (bitmap == null){
            return null
        }
        //check SDK in which we are and if we can save the folder
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Screenshots")
            }
            val uri = this.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null){
                this.contentResolver.openOutputStream(uri).use {
                    if (it == null)
                        return@use
                    //compress the file and set a quality
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, it)
                    it.flush()
                    it.close()

                    //add picture to gallery
                    MediaScannerConnection.scanFile(this, arrayOf(uri.toString()), null, null)
                }
            }
            return uri.toString()
        }

        //indicate the route
        val filePath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES + "/Screenshots"
        ).absolutePath

        val dir = File(filePath)
        if (!dir.exists())
            dir.mkdirs()
        val file = File(dir, fileName)
        val fOut = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
        fOut.flush()
        fOut.close()

        //add picture to gallery
        MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null, null)
        return filePath
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
        checkOptions(x,y)

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
        var firstPosition = false
        while (!firstPosition) {
            x = (0..7).random()
            y = (0..7).random()
            if (board[x][y] == 0)
                firstPosition = true
            checkOptions(x,y)
            if (options == 0)
                firstPosition = false
        }

        cellSelected_x = x
        cellSelected_y = y
        selectCell(x,y)
    }

    private fun setLevel() {
        if (nextLevel){
            level++
            setLives()
        }
        else {
            lives--
            if (lives < 1){
                level = 1
                lives = 1
            }
        }
    }
    private fun setLives(){
        when(lives){
            1->lives = 1
            2->lives = 4
            3->lives = 3
            4->lives = 3
            5->lives = 4
            6->lives = 3
            7->lives = 5
            8->lives = 5
            9->lives = 3
            10->lives = 4
            11->lives = 5
            12->lives = 5
            13->lives =3
        }
    }
    private fun setLevelParameters(){
        var tvLiveData = findViewById<TextView>(R.id.tvLiveData)
        tvLiveData.text = lives.toString()

        score_lives = lives

        var tvLevelNumber = findViewById<TextView>(R.id.tvLevelNumber)
        tvLevelNumber.text = level.toString()
        scoreLevel = level

        bonus = 0
        var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
        tvBonusData.text = ""

        setLevelMoves()
        moves = levelMoves

        movesRequired = setMovesRequired()
    }

    private fun setLevelMoves(){
        when (level){
            1-> levelMoves = 64
            2-> levelMoves = 56
            3-> levelMoves = 32
            4-> levelMoves = 16
            5-> levelMoves = 48
            6-> levelMoves = 36
            7-> levelMoves = 48
            8-> levelMoves = 49
            9-> levelMoves = 59
            10-> levelMoves = 48
            11-> levelMoves = 64
            12-> levelMoves = 48
            13-> levelMoves = 48
        }
    }
    private fun setMovesRequired():Int{
        var movesRequired = 0

        when(level){
            1-> movesRequired = 8
            2-> movesRequired = 10
            3-> movesRequired = 12
            4-> movesRequired = 10
            5-> movesRequired = 10
            6-> movesRequired = 12
            7-> movesRequired = 5
            8-> movesRequired = 7
            9-> movesRequired = 9
            10-> movesRequired = 8
            11-> movesRequired = 1000
            12-> movesRequired = 5
            13-> movesRequired = 5
        }
        return movesRequired
    }
    private fun setBoardLevel(){
        when(level){
            2-> paintLevel_2()
            3-> paintLevel_3()
            4-> paintLevel_4()
            5-> paintLevel_5()
            6-> paintLevel_6()
            7-> paintLevel_7()
            8-> paintLevel_8()
            9-> paintLevel_9()
            10-> paintLevel_10()
            11-> paintLevel_11()
            12-> paintLevel_12()
            13-> paintLevel_13()
        }
    }

    private fun paint_Column(column: Int){
        for (i in 0..7){
            board[column][i] = 1
            paintHorseCell(column, i, "previous_cell")
        }
    }
    private fun paintLevel_2() {
        paint_Column(6)
    }
    private fun paintLevel_3() {
        for (i in 0..7){
            for(j in 4..7){
                board[j][i] = 1
                paintHorseCell(j, i, "previous_cell")
            }
        }
    }
    private fun paintLevel_4() {
        paintLevel_3()
        paintLevel_5()
    }
    private fun paintLevel_5() {
        for (i in 0..3){
            for(j in 0..3){
                board[j][i] = 1
                paintHorseCell(j, i, "previous_cell")
            }
        }
    }
    private fun paintLevel_6() {
        paintLevel_5()
        paintLevel_2()
    }
    private fun paintLevel_7() {
        for (i in 0..4){
            for(j in 0..4){
                board[j][i] = 1
                paintHorseCell(j, i, "previous_cell")
            }
        }
    }
    private fun paintLevel_8() {
        paint_Column(4)
    }
    private fun paintLevel_9() {
        paintLevel_3()
        paintLevel_4()
    }
    private fun paintLevel_10() {
        paintLevel_5()
        paintLevel_6()
    }
    private fun paintLevel_11() {
        paint_Column(2)
    }
    private fun paintLevel_12() {
        paint_Column(4)
    }
    private fun paintLevel_13() {
        paintLevel_3()
        paintLevel_4()
        paintLevel_5()
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
        nextLevel = !gameOver
        var lyMessage = findViewById<LinearLayout>(R.id.lyMessage)
        lyMessage.visibility = View.VISIBLE
        var tvTitleLevel = findViewById<TextView>(R.id.tvTitleMessage)
        tvTitleLevel.text = title
        var score: String = ""
        var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
        if(gameOver){
            score = "Score: "+ (levelMoves - moves) + "/" + levelMoves
            string_share = "This game makes me crazy! " + score + ""

        }
        else{
            score = tvTimeData.text.toString()
            string_share = "Let's go! New challenge completed. Level: $level (" + score + ")"
        }
        var tvScoreMessage = findViewById<TextView>(R.id.tvScoreMessage)
        tvScoreMessage.text = score
        var tvAction = findViewById<TextView>(R.id.tvAction)
        tvAction.text = action
    }

    private fun checkOptions(x:Int, y:Int){
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
        setLevel()
        setLevelParameters()

        resetBoard()
        clearBoard()

        setBoardLevel()
        setFirstPosition()

        resetTime()
        startTime()
        playing = true
    }

    fun launchAction(view: View) {
        hideMessage(true)
    }
}