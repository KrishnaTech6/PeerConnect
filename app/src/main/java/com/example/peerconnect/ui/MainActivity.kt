package com.example.peerconnect.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.peerconnect.R
import com.example.peerconnect.adapters.MainRecyclerViewAdapter
import com.example.peerconnect.databinding.ActivityMainBinding
import com.example.peerconnect.repository.MainRepository
import com.example.peerconnect.service.MainService
import com.example.peerconnect.service.MainServiceRepository
import com.example.peerconnect.utils.DataModel
import com.example.peerconnect.utils.DataModelType
import com.example.peerconnect.utils.getCameraAndMicPermissions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainRecyclerViewAdapter.Listener, MainService.Listener {
    private lateinit var binding: ActivityMainBinding
    private var username : String? = null
    private val TAG = "MainActivity"
    private var mainAdapter: MainRecyclerViewAdapter?= null

    @Inject lateinit var mainRepository: MainRepository
    @Inject lateinit var mainServiceRepository: MainServiceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mainServiceRepository.stopService()
    }

    private fun init(){

        username = intent.getStringExtra("username")
        if(username==null) finish()

        //observe other user status
        subscribeObservers()

        //start foreground service to listen for incoming calls
        startMyService()

    }

    private fun subscribeObservers() {
        setupRecyclerView()
        MainService.listener = this
        mainRepository.observeUserStatus{
            Log.d(TAG, "subscribeObservers: $it")
            mainAdapter?.updateList(it)
        }
    }

    private fun setupRecyclerView() {
        mainAdapter= MainRecyclerViewAdapter(this)
        val linearLayoutManager = LinearLayoutManager(this)
        binding.mainRecyclerView.apply {
            layoutManager = linearLayoutManager
            adapter = mainAdapter
        }
    }

    override fun videoCallClicked(username: String) {
        //Check if permission of camera and microphone is granted
        getCameraAndMicPermissions{
            mainRepository.sendConnectionRequest(username, true){
                //We have to start video call
                // move to call activity
                startActivity(Intent(this@MainActivity, CallActivity::class.java).apply {
                    putExtra("isCaller", true)
                    putExtra("isVideoCall", true)
                    putExtra("target", username)
                })
            }
        }
    }

    override fun audioCallClicked(username: String) {
        getCameraAndMicPermissions {
            mainRepository.sendConnectionRequest(username, false){
                if(it){
                    //We have to start audio call
                    // move to call activity
                    startActivity(Intent(this@MainActivity, CallActivity::class.java).apply {
                        putExtra("isCaller", true)
                        putExtra("isVideoCall", false)
                        putExtra("target", username)
                    })
                }
            }
        }
    }

    private fun startMyService() {
        mainServiceRepository.startService(username!!)
    }

    override fun onCallReceived(model: DataModel) {
        runOnUiThread{
            binding.apply {
                val isVideoCall = model.type == DataModelType.StartVideoCall
                val isVideoCallText = if(isVideoCall) "Video" else "Audio"
                incomingCallTitleTv.text = " ${model.sender} Incoming $isVideoCallText Call"
                incomingCallLayout.isVisible = true
                acceptButton.setOnClickListener {
                    getCameraAndMicPermissions {
                        incomingCallLayout.isVisible= false
                        //We have to start video call
                        // move to call activity
                        startActivity(Intent(this@MainActivity, CallActivity::class.java).apply {
                            putExtra("isCaller", false)
                            putExtra("isVideoCall", isVideoCall)
                            putExtra("target", model.sender)
                        })
                    }
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                }
            }
        }
    }
}