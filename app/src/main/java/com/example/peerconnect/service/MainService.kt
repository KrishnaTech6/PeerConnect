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
import com.example.peerconnect.service.MainServiceActions.START_SERVICE
import com.example.peerconnect.utils.DataModel
import com.example.peerconnect.utils.DataModelType
import com.example.peerconnect.utils.isValid
import dagger.hilt.android.AndroidEntryPoint
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
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{ incomingIntent ->
            when(incomingIntent.action){
                START_SERVICE.name -> handleStartService(incomingIntent)
                else -> Unit
            }
        }
        return START_STICKY
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
                DataModelType.StartVideoCall,
                -> {
                    listener?.onCallReceived(data)
                }

                else -> Unit
            }
        }
    }

    interface Listener{
        fun onCallReceived(model: DataModel)
    }

}