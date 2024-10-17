package com.example.peerconnect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.peerconnect.R
import com.example.peerconnect.service.MainServiceActions.START_SERVICE
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainService : Service(){
    private var isServiceRunning = false
    private var username: String? = null

    private lateinit var notificationManager: NotificationManager

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
        }
    }

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

}