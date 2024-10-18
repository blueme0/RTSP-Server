package com.pedro.sample

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.decoder.AudioDecoderInterface
import com.pedro.encoder.input.decoder.VideoDecoderInterface
import com.pedro.rtspserver.RtspServerFromFile
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class FileDemoActivity : AppCompatActivity(), ConnectChecker, VideoDecoderInterface, AudioDecoderInterface, SeekBar.OnSeekBarChangeListener {

    private lateinit var rtspServerFromFile: RtspServerFromFile
    private var filePath  = ""

    private var samplePath = "android.resource://${packageName}/raw/sample"

    private var currentDateAndTime = ""
    private lateinit var folder: File
    private var touching: Boolean = false

    private var btnStartStop: Button? = null
    private var btnSelectFile: Button? = null
    private var btnReSync: Button? = null
    private var btnRecord: Button? = null
    //    private var etUrl: EditText? = null
    private var seekBar: SeekBar? = null
    private var tvFile: TextView? = null
    private var tvUrl: TextView? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_file_demo)
        folder = PathUtils.getRecordPath()

        initView()
        initListener()

        rtspServerFromFile = RtspServerFromFile(this, this, 8554, this, this)
        tvUrl?.text = rtspServerFromFile.streamClient.getEndPointConnection()
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onPause() {
        super.onPause()
        if (rtspServerFromFile.isRecording) {
            rtspServerFromFile.stopRecord()
            PathUtils.updateGallery(this, folder.absolutePath + "/" + currentDateAndTime + ".mp4")
            btnRecord?.text = "Start Record"
        }
        if (rtspServerFromFile.isStreaming) {
            rtspServerFromFile.stopStream()
            btnStartStop?.text = "Start Stream"
        }
    }

    override fun onConnectionStarted(url: String) {

    }

    override fun onConnectionSuccess() {
        runOnUiThread {
            Toast.makeText(
                this@FileDemoActivity,
                "Connection Success",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            Toast.makeText(this@FileDemoActivity, "Connection Failed. $reason", Toast.LENGTH_SHORT).show()
            rtspServerFromFile.stopStream()
            btnStartStop?.text = "Start Stream"
        }
    }

    override fun onNewBitrate(bitrate: Long) {
    }

    override fun onDisconnect() {
        runOnUiThread {
            Toast.makeText(this@FileDemoActivity, "Disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthError() {
        runOnUiThread {
            Toast.makeText(this@FileDemoActivity, "Auth error", Toast.LENGTH_SHORT).show()

        }
    }

    override fun onAuthSuccess() {
        runOnUiThread {
            Toast.makeText(this@FileDemoActivity, "Auth success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5 && data != null) {
//            filePath = PathUtils.getRecordPath().toString()
            filePath = PathUtils.getPath(this@FileDemoActivity, data.data!!).toString()
            Toast.makeText(this@FileDemoActivity, filePath, Toast.LENGTH_SHORT).show()
            tvFile?.text = filePath
        }
    }

    private fun initView() {
        btnStartStop = findViewById(R.id.btn_start_stop)
        btnSelectFile = findViewById(R.id.btn_select_file)
        btnReSync = findViewById(R.id.btn_re_sync)
        btnRecord = findViewById(R.id.btn_record)
//        etUrl = findViewById(R.id.et_url)
//        etUrl?.hint = "HINT"
        seekBar = findViewById(R.id.seek_bar)
        seekBar?.progressDrawable?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
        tvFile = findViewById(R.id.tv_file)
        tvUrl = findViewById(R.id.tv_url)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initListener() {
        seekBar?.setOnSeekBarChangeListener(this)

        btnStartStop?.setOnClickListener {
            if (!rtspServerFromFile.isStreaming) {
                try {
                    if (!rtspServerFromFile.isRecording) {
                        if (prepare()) {
                            btnStartStop?.text = "Stop Stream"
//                            etUrl?.let {  }
                            rtspServerFromFile.startStream()
                            seekBar?.max = rtspServerFromFile.videoDuration.toInt()
                                .coerceAtLeast(rtspServerFromFile.audioDuration.toInt())
                            updateProgress()
                            Log.d("FILE_DEMO", "recording 10")

                        } else {
                            btnStartStop?.text = "Start Stream"
                            rtspServerFromFile.stopStream()
                            Toast.makeText(this@FileDemoActivity, "Error: unsupported file", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        btnStartStop?.text = "Stop Stream"
                        rtspServerFromFile.startStream()
//                        etUrl?.let { rtspServerFromFile.startStream(it.text.toString()) }
                    }
                } catch (e: IOException) {
                    Toast.makeText(this@FileDemoActivity, "Erorr: file not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("FILE_DEMO", "recording 11")

                btnStartStop?.text = "Start Stream"
                rtspServerFromFile.stopStream()
            }
        }

        btnSelectFile?.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("*/*")
            startActivityForResult(intent, 5)
        }

        btnReSync?.setOnClickListener {
            rtspServerFromFile.reSyncFile()
        }

        btnRecord?.setOnClickListener {
            if (!rtspServerFromFile.isRecording) {
                try {
                    if (!folder.exists()) {
                        folder.mkdir()
                    }
                    val sdf: SimpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    currentDateAndTime = sdf.format(Date())
                    if (!rtspServerFromFile.isStreaming) {
                        if (prepare()) {
                            rtspServerFromFile.startRecord(
                                folder.absolutePath + "/" + currentDateAndTime + ".mp4"
                            )
                            seekBar?.max = rtspServerFromFile.videoDuration.toInt().coerceAtLeast(rtspServerFromFile.audioDuration.toInt())
                            updateProgress()
                            btnRecord?.text = "Stop Record"
                            Log.d("FILE_DEMO", "recording 1")
                            Toast.makeText(this@FileDemoActivity, "Recording... ", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.d("FILE_DEMO", "recording 2")
                            Toast.makeText(this@FileDemoActivity, "Error preparing stream, This device can't do it", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        rtspServerFromFile.startRecord(
                            folder.absolutePath + "/" + currentDateAndTime + ".mp4"
                        )
                        btnRecord?.text = "Stop Record"
                        Toast.makeText(this@FileDemoActivity, "Recording... ", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    Log.d("FILE_DEMO", "recording 3")
                    rtspServerFromFile.stopRecord()
                    PathUtils.updateGallery(this@FileDemoActivity, folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                    Log.d("FILE_DEMO", "recording 4")
                    btnRecord?.text = "Start Record"
                    Toast.makeText(this@FileDemoActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("FILE_DEMO", "recording 5")
                rtspServerFromFile.stopRecord()
                PathUtils.updateGallery(this@FileDemoActivity, folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                btnRecord?.text = "Start Record"
                Toast.makeText(this@FileDemoActivity, "file $currentDateAndTime.mp4 saved in ${folder.absolutePath}", Toast.LENGTH_SHORT).show()
                currentDateAndTime = ""
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun prepare(): Boolean {
        var result = rtspServerFromFile.prepareVideo(filePath)
        result = result or rtspServerFromFile.prepareAudio(filePath)
        return result
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun updateProgress() {
        Log.d("FILE_DEMO", "recording 6")
        Thread {
            while (rtspServerFromFile.isStreaming || rtspServerFromFile.isRecording) {
                try {
                    Thread.sleep(1000)
                    if (!touching) {
                        runOnUiThread {
                            seekBar?.let {
                                setProgress(
                                    rtspServerFromFile.videoTime.toInt()
                                        .coerceAtLeast(rtspServerFromFile.audioTime.toInt())
                                )
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
        Log.d("FILE_DEMO", "recording 7")
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onVideoDecoderFinished() {
        runOnUiThread {
            if (rtspServerFromFile.isRecording) {
                rtspServerFromFile.stopRecord()
                PathUtils.updateGallery(applicationContext, folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                btnRecord?.text = "Start Record"
                Toast.makeText(this@FileDemoActivity, "file $currentDateAndTime.mp4 saved in ${folder.absolutePath}", Toast.LENGTH_SHORT).show()
                currentDateAndTime = ""
            }
            if (rtspServerFromFile.isStreaming) {
                btnStartStop?.text = "Start Stream"
                Toast.makeText(this@FileDemoActivity, "Video stream finished", Toast.LENGTH_SHORT).show()
                rtspServerFromFile.stopStream()
            }
        }
    }

    override fun onAudioDecoderFinished() {
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
        touching = true
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onStopTrackingTouch(p0: SeekBar?) {
        if (rtspServerFromFile.isStreaming || rtspServerFromFile.isRecording) {
            p0?.let { rtspServerFromFile.moveTo(it.progress.toDouble()) }
            Handler().postDelayed({ rtspServerFromFile.reSyncFile() }, 500)
        }
        touching = false
    }
}