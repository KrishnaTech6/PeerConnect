package com.example.peerconnect.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.peerconnect.R
import com.example.peerconnect.databinding.ActivityCallBinding
import com.example.peerconnect.service.MainService
import com.example.peerconnect.service.MainServiceRepository
import com.example.peerconnect.webrtc.RTCAudioManager
import dagger.hilt.android.AndroidEntryPoint
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

    @Inject lateinit var serviceRepository: MainServiceRepository
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
        setCameraToggleClicked()
        setUpToggleAudioDevice()
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