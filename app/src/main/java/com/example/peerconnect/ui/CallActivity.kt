package com.example.peerconnect.ui

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.peerconnect.R
import com.example.peerconnect.databinding.ActivityCallBinding
import com.example.peerconnect.service.MainService
import com.example.peerconnect.service.MainServiceRepository
import com.example.peerconnect.utils.convertToHumanTime
import com.example.peerconnect.webrtc.RTCAudioManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class CallActivity : AppCompatActivity(), MainService.EndCallListener {
    private var target:String? = null
    private var isCaller:Boolean = true
    private var isVideoCall: Boolean = true
    private lateinit var binding: ActivityCallBinding

    private var isMicrophoneMuted = false
    private var isCameraMuted = false
    private var isSpeakerMode = true
    var isScreenCasting = false

    @Inject lateinit var serviceRepository: MainServiceRepository
    private lateinit var requestScreenCaptureLauncher: ActivityResultLauncher<Intent>

    override fun onStart() {
        super.onStart()
        requestScreenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == RESULT_OK){
                val intent= result.data
                //give result to service and it passes to webrtc client
                MainService.screenPermissionIntent = intent
                isScreenCasting= true
                updateUiToScreenCaptureIsOn()
                serviceRepository.toggleScreenShare(true)
            }else {

            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        init()
    }

    private fun init() {
        intent.getStringExtra("target")?.let {
            this.target = it
        } ?: kotlin.run { finish() }
        isCaller = intent.getBooleanExtra("isCaller", true)
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)

        binding.apply {
            callTitleTv.text = "In call with $target"
            CoroutineScope(Dispatchers.IO).launch {
                for (i in 0..3600){
                    delay(1000)
                    withContext(Dispatchers.Main){
                        //convert this int to human readable time
                        callTimerTv.text = i.convertToHumanTime()
                    }
                }
            }
            if(!isVideoCall){
                toggleCameraButton.isVisible= false
                screenShareButton.isVisible= false
                switchCameraButton.isVisible= false
            }
            MainService.remoteSurfaceView = remoteView
            MainService.localSurfaceView = localView
            serviceRepository.setupViews(isVideoCall, isCaller, target!!)

            endCallButton.setOnClickListener{
                serviceRepository.sendEndCall()
            }

            switchCameraButton.setOnClickListener {
                serviceRepository.switchCamera()
            }
        }
        MainService.endCallListener = this
        setupMicToggleClicked()
        setupScreenCasting()
        setCameraToggleClicked()
        setUpToggleAudioDevice()
    }

    private fun setupScreenCasting() {
        binding.apply {
            screenShareButton.setOnClickListener {
                if(!isScreenCasting){
                    AlertDialog.Builder(this@CallActivity)
                        .setTitle("Screen Casting")
                        .setMessage("Are you sure to cast your screen?")
                        .setNegativeButton("No"){dialog, _->
                            startScreenCasting()
                            dialog.dismiss()
                        }
                        .setPositiveButton("Yes"){dialog, _->
                            dialog.dismiss()
                        }.create().show()

                }else{
                    isScreenCasting = false
                    updateUiToScreenCaptureIsOff()
                    serviceRepository.toggleScreenShare(false)
                }
            }
        }


    }

    private fun startScreenCasting() {
        val mediaProjectionManager = application.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        requestScreenCaptureLauncher.launch(captureIntent)

    }

    private fun updateUiToScreenCaptureIsOn(){
        binding.apply {
            localView.isVisible= false
            toggleCameraButton.isVisible = false
            switchCameraButton.isVisible = false
            screenShareButton.setImageResource(R.drawable.ic_stop_screen_share)
        }
    }

    private fun updateUiToScreenCaptureIsOff(){
        binding.apply {
            localView.isVisible= true
            toggleCameraButton.isVisible = true
            switchCameraButton.isVisible = true
            screenShareButton.setImageResource(R.drawable.ic_screen_share)
        }
    }

    private fun setupMicToggleClicked(){
        binding.apply {
            toggleMicrophoneButton.setOnClickListener {
                if(!isMicrophoneMuted){
                    serviceRepository.toggleAudio(true)
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_on)
                }else{
                    serviceRepository.toggleAudio(false)
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_off)
                }
                isMicrophoneMuted =!isMicrophoneMuted
            }
        }
    }

    private fun setCameraToggleClicked(){
        binding.apply {
            toggleCameraButton.setOnClickListener {
                if(!isCameraMuted){
                    serviceRepository.toggleVideo(true)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_on)
                }else{
                    serviceRepository.toggleVideo(false)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_off)
                }
                isCameraMuted = !isCameraMuted
            }
        }
    }

    private fun setUpToggleAudioDevice(){
        binding.apply {
            toggleAudioDevice.setOnClickListener {
                if(!isSpeakerMode){
                    toggleAudioDevice.setImageResource(R.drawable.ic_speaker)
                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.EARPIECE.name)
                }else{
                    toggleAudioDevice.setImageResource(R.drawable.ic_ear)
                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE.name)
                }
                isSpeakerMode = !isSpeakerMode
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        serviceRepository.sendEndCall()
    }

    override fun onCallEnded() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        MainService.remoteSurfaceView?.release()
        MainService.remoteSurfaceView = null

        MainService.localSurfaceView?.release()
        MainService.localSurfaceView = null

    }
}