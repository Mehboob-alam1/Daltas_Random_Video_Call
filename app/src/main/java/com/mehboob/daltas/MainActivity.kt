package com.mehboob.daltas

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceView
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mehboob.daltas.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val appId = "533552e5ee3c4d8caf9d3bc3f22392f5"
    private val channelName = "daltas"
    private val token =
        "007eJxTYNDgTtdauOD59iBp1buekoFZJbvttdP3H9J0nDY9cE2f2SkFBlNjY1NTo1TT1FTjZJMUi+TENMsU46Rk4zQjI2NLozTTGWryKQ2BjAzODr8YGRkgEMRnY0hJzClJLGZgAADY8h3v"
    private val uid = 0
    private var isJoined = false
    private var agoraEngine: RtcEngine? = null

    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private val PERMISSION_ID = 12
    private val REQUESTED_PERMISSIONS =
        arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupVideoSdkEngine() {
        try {

            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler

            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(
                this, REQUESTED_PERMISSIONS,
                PERMISSION_ID
            )
        }
        setupVideoSdkEngine()
        binding.btnJoin.setOnClickListener {
            joinCall()
        }

        binding.btnLeave.setOnClickListener {
            leaveCall()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    private fun leaveCall() {

        if (!isJoined) {
            showMessage("Join a channel first ")
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            if (remoteSurfaceView != null)
                remoteSurfaceView!!.visibility = GONE
            if (localSurfaceView != null)
                localSurfaceView!!.visibility = GONE

            isJoined = false
        }
    }

    private fun joinCall() {

        if (checkSelfPermission()) {
            val option = ChannelMediaOptions()
            option.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            option.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setUpLocalVideo()
            localSurfaceView!!.visibility = VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, channelName, uid, option)

        } else {
            showMessage("Permission not granted")
        }

    }

    private fun setUpRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        binding.remoteUser.addView(remoteSurfaceView)

        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
    }

    private fun setUpLocalVideo() {
        localSurfaceView = SurfaceView(baseContext)
        binding.localUser.addView(localSurfaceView)

        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                0
            )
        )
    }

    private val mRtcEventHandler: IRtcEngineEventHandler =
        object : IRtcEngineEventHandler() {
            override fun onUserJoined(uid: Int, elapsed: Int) {
                showMessage("Remote user joined $uid")
                runOnUiThread {
                    setUpRemoteVideo(uid)
                }
            }

            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                isJoined = true
                showMessage("Joined channel $channel")
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                showMessage("User offline")
                runOnUiThread {
                    remoteSurfaceView!!.visibility = GONE
                }
            }
        }
}