package com.example.peerconnect.webrtc

import android.content.Context
import com.example.peerconnect.utils.DataModel
import com.example.peerconnect.utils.DataModelType
import com.google.gson.Gson
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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
    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }

    //call variables
    private lateinit var localSurfaceView: SurfaceViewRenderer
    private lateinit var remoteSurfaceView: SurfaceViewRenderer
    private var localStream: MediaStream? = null
    private var localTrackId = ""
    private var localStreamId = ""
    private var localAudioTrack: AudioTrack?=null
    private var localVideoTrack: VideoTrack?=null


    //Installing requirement section
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
    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServer, observer)
    }
     fun initializeWebRTCClient(username: String, observer: PeerConnection.Observer){
        this.username = username
        localTrackId= "${username}_track"
        localStreamId= "${username}_stream"
        peerConnection = createPeerConnection(observer)
    }

    //Negotiation Section
    fun call(target: String){
        peerConnection?.createOffer(object : MySdpObserver(){
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object: MySdpObserver(){
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            DataModel(type = DataModelType.Offer,
                                target = target,
                                data = desc?.description,
                                sender = username)
                        )
                    }
                }, desc)
            }
        }, mediaConstraint)
    }
    fun answer(target:String){
        peerConnection?.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            DataModel(type = DataModelType.Answer,
                                sender = username,
                                target = target,
                                data = desc?.description)
                        )
                    }
                },desc)
            }
        },mediaConstraint)
    }
    fun onRemoteSessionReceived(sessionDescription: SessionDescription){
        peerConnection?.setRemoteDescription(MySdpObserver(),sessionDescription)
    }
    fun addIceCandidateToPeer(iceCandidate: IceCandidate){
        peerConnection?.addIceCandidate(iceCandidate)
    }
    fun sendIceCandidate(target: String,iceCandidate: IceCandidate){
        addIceCandidateToPeer(iceCandidate)
        listener?.onTransferEventToSocket(
            DataModel(
                type = DataModelType.IceCandidate,
                sender = username,
                target = target,
                data = gson.toJson(iceCandidate)
            )
        )
    }
    fun closeConnection(){
        try {
            videoCapturer.dispose()
            localStream?.dispose()
            peerConnection?.close()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
    fun switchCamera(){
        videoCapturer.switchCamera(null)
    }
    fun toggleAudio(shouldBeMuted:Boolean){
        if (shouldBeMuted){
            localStream?.removeTrack(localAudioTrack)
        }else{
            localStream?.addTrack(localAudioTrack)
        }
    }
    fun toggleVideo(shouldBeMuted: Boolean){
        try {
            if (shouldBeMuted){
                stopCapturingCamera()
            }else{
                startCapturingCamera(localSurfaceView)
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }


    //Streaming Section
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
    fun initLocalSurfaceView(localView: SurfaceViewRenderer, isVideoCall: Boolean){
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
    private fun getVideoCapturer(context:Context): CameraVideoCapturer =
        Camera2Enumerator(context).run {
            deviceNames.find { isFrontFacing(it) }?.let {
                createCapturer(it, null)
            }?: throw IllegalStateException()
        }
    private fun stopCapturingCamera(){
        videoCapturer.dispose()
        localVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localVideoTrack)
        localVideoTrack?.dispose()
    }


    interface Listener{
        fun onTransferEventToSocket(data: DataModel)
    }
}