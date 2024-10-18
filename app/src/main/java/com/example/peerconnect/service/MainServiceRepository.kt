package com.example.peerconnect.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.peerconnect.ui.CallActivity
import javax.inject.Inject

class MainServiceRepository @Inject constructor(
    private val context: Context,
) {

    fun startService(username: String) {
        Thread {
            val intent = Intent(context, MainService::class.java)
            intent.putExtra("username", username)
            intent.action = MainServiceActions.START_SERVICE.name
            startServiceIntent(intent)
        }.start()
    }

    private fun startServiceIntent(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun setupViews(videoCall: Boolean, caller: Boolean, target: String) {
        val intent = Intent(context, CallActivity::class.java)
            .apply {
                action = MainServiceActions.SETUP_VIEWS.name
                putExtra("isVideoCall", videoCall)
                putExtra("isCaller", caller)
                putExtra("target", target)
            }
        startServiceIntent(intent)
    }
}