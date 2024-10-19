package com.example.peerconnect.webrtc

import android.content.Context
import com.example.peerconnect.utils.DataModel
import com.google.gson.Gson
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import javax.inject.Inject

class WebRTCClient @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {
    //class variables
    var listener: Listener? = null
    private lateinit var username: String


    //webrtc variables
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private var peerConnection:PeerConnection? = null
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
            .setUsername("83eebabf8b4cce9d5dbcb649")
            .setPassword("2D7JvfkOQtBdYW3R").createIceServer()
    )
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints())}
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private val videoCapturer = getVideoCapturer(context)

    //call variables
    private lateinit var localSurfaceView: SurfaceViewRenderer
    private lateinit var remoteSurfaceView: SurfaceViewRenderer
    private var localStream: MediaStream? = null
    private var localTrackId = ""
    private var localStreamId = ""
    private var localAudioTrack: AudioTrack?=null
    private var localVideoTrack: VideoTrack?=null

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true).setFieldTrials("webRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory():PeerConnectionFactory{
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory( eglBaseContext,true, true))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption= false
                disableNetworkMonitor = false
            }).createPeerConnectionFactory()

    }

    private fun createWebRTCClient(username: String, observer: PeerConnection.Observer){
        this.username = username
        localTrackId= "${username}_track"
        localStreamId= "${username}_stream"
        peerConnection = createPeerConnection(observer)
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServer, observer)
    }

    private fun initSurfaceView(view: SurfaceViewRenderer){
        view.run{
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext, null)

        }
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer){
        this.remoteSurfaceView = view
        initSurfaceView(view)
    }

    fun initLocalSerfaceView(localView: SurfaceViewRenderer, isVideoCall: Boolean){
        this.localSurfaceView = localView
        initSurfaceView(localView)
        startLocalStreaming(localView, isVideoCall)
    }

    private fun startLocalStreaming(localView: SurfaceViewRenderer, isVideoCall: Boolean) {
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        if(isVideoCall){
            startCapturingCamera(localView)
        }
        localAudioTrack = peerConnectionFactory.createAudioTrack(localTrackId+"_audio", localAudioSource)
        localStream?.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }

    private fun startCapturingCamera(localView: SurfaceViewRenderer) {
        surfaceTextureHelper= SurfaceTextureHelper.create(
            Thread.currentThread().name, eglBaseContext
        )
        videoCapturer.initialize(
            surfaceTextureHelper, context, localVideoSource.capturerObserver
        )
        videoCapturer.startCapture(720, 480, 20)
        localVideoTrack = peerConnectionFactory.createVideoTrack(localTrackId+"_video", localVideoSource)
        localVideoTrack?.addSink(localView)
        localStream?.addTrack(localVideoTrack)
    }
    private fun stopCapturingCamera(){
        videoCapturer.dispose()
        localVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localVideoTrack)
        localVideoTrack?.dispose()
    }
    private fun getVideoCapturer(context:Context): CameraVideoCapturer =
        Camera2Enumerator(context).run {
            deviceNames.find { isFrontFacing(it) }?.let {
                createCapturer(it, null)
            }?: throw IllegalStateException()
        }


    interface Listener{
        fun onTransferEventToSocket(data: DataModel)
    }
}