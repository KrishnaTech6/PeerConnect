package com.example.peerconnect.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.peerconnect.R
import com.example.peerconnect.repository.MainRepository
import com.example.peerconnect.service.MainServiceActions.END_CALL
import com.example.peerconnect.service.MainServiceActions.SETUP_VIEWS
import com.example.peerconnect.service.MainServiceActions.START_SERVICE
import com.example.peerconnect.service.MainServiceActions.SWITCH_CAMERA
import com.example.peerconnect.service.MainServiceActions.TOGGLE_AUDIO
import com.example.peerconnect.service.MainServiceActions.TOGGLE_VIDEO
import com.example.peerconnect.utils.DataModel
import com.example.peerconnect.utils.DataModelType
import com.example.peerconnect.utils.isValid
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@AndroidEntryPoint
class MainService : Service(), MainRepository.Listener {
    private var isServiceRunning = false
    private var username: String? = null
    private  val TAG = "MainService"

    private lateinit var notificationManager: NotificationManager

    @Inject lateinit var mainRepository: MainRepository

    companion object{
        var listener: Listener? = null
        var endCallListener: EndCallListener? = null
        var localSurfaceView: SurfaceViewRenderer? = null
        var remoteSurfaceView: SurfaceViewRenderer? = null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{ incomingIntent ->
            when(incomingIntent.action){
                START_SERVICE.name -> handleStartService(incomingIntent)
                SETUP_VIEWS.name -> handleSetupViews(incomingIntent)
                END_CALL.name -> handleEndCall()
                SWITCH_CAMERA.name -> handleSwitchCamera()
                TOGGLE_AUDIO.name -> handleToggleAudio(incomingIntent)
                TOGGLE_VIDEO.name -> handleToggleVideo(incomingIntent)
                else -> Unit
            }
        }
        return START_STICKY
    }

    private fun handleToggleVideo(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted", false)
        mainRepository.toggleVideo(shouldBeMuted = shouldBeMuted)
    }

    private fun handleToggleAudio(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted", false)
        mainRepository.toggleAudio(shouldBeMuted = shouldBeMuted)
    }

    private fun handleSwitchCamera() {
        mainRepository.switchCamera()
    }

    private fun handleEndCall() {
        mainRepository.sendEndCall()

        endCallAndRestartRepository()
    }

    private fun endCallAndRestartRepository() {
        mainRepository.endCall()
        endCallListener?.onCallEnded()
        mainRepository.initWebRTCClient(username!!)
    }

    private fun handleSetupViews(incomingIntent: Intent) {
        val isCaller = incomingIntent.getBooleanExtra("isCaller", false)
        val isVideoCall = incomingIntent.getBooleanExtra("isVideoCall", true)
        val target = incomingIntent.getStringExtra("target")

        mainRepository.setTarget(target!!)
        //initialize our widgets , get audio and video call
        //get prepared for call

        mainRepository.initLocalSurfaceView(localSurfaceView!!, isVideoCall)
        mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)

        if(!isCaller){// is a Callee
            mainRepository.startCall()
        }
    }

    private fun handleStartService(incomingIntent: Intent) {
        //Start our foreground service
        if(!isServiceRunning){
            isServiceRunning = true
            username = incomingIntent.getStringExtra("username")
            startServiceWithNotification()

            //setup my clients
            mainRepository.listener = this
            mainRepository.initFirebase()
            mainRepository.initWebRTCClient(username!!)
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startServiceWithNotification() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel= NotificationChannel(
                "channel1", "foreground",NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(notificationChannel)
            val notification = NotificationCompat.Builder(
                this, "channel1"
            ).setSmallIcon(R.mipmap.ic_launcher)

            startForeground(1, notification.build())
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onLatestEventReceived(data: DataModel) {
        if(data.isValid()){
            when (data.type) {
                DataModelType.StartAudioCall,
                DataModelType.StartVideoCall -> {
                    listener?.onCallReceived(data)
                }

                else -> Unit
            }
        }
    }

    override fun endCall() {
        //We are receiving end call signal from remote peer
        endCallAndRestartRepository()
    }

    interface Listener{
        fun onCallReceived(model: DataModel)
    }

    interface EndCallListener {
        fun onCallEnded()
    }

}