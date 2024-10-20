package com.example.peerconnect.repository

import com.example.peerconnect.firebaseClient.FirebaseClient
import com.example.peerconnect.utils.DataModel
import com.example.peerconnect.utils.DataModelType.EndCall
import com.example.peerconnect.utils.DataModelType.StartAudioCall
import com.example.peerconnect.utils.DataModelType.StartVideoCall
import com.example.peerconnect.utils.UserStatus
import com.example.peerconnect.webrtc.MyPeerObserver
import com.example.peerconnect.webrtc.WebRTCClient
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: WebRTCClient
) : WebRTCClient.Listener {
    private var target: String? = null
    var listener: Listener? = null

    fun login(username: String, password: String, isDone: (Boolean, String ?)-> Unit) {
        firebaseClient.login(username, password, isDone)
    }

    fun observeUserStatus(status: (List<Pair<String, String>>) -> Unit) {
        firebaseClient.observeUserStatus(status)
    }

    fun initFirebase(){
        firebaseClient.subscribeForLatestEvents(object: FirebaseClient.Listener{
            override fun onLatestEventReceived(event: DataModel) {
                listener?.onLatestEventReceived(event)
                when(event.type){

                    else -> Unit

                }
            }
        })
    }

    fun sendConnectionRequest(target: String, isVideoCall: Boolean, success: (Boolean) -> Unit){
        val message = DataModel(
            target = target,
            type = if(isVideoCall) StartVideoCall else StartAudioCall
        )
        firebaseClient.sendMessageToOtherClient(message, success)
    }

    fun setTarget(target: String) {
        this.target = target
    }

    interface Listener{
        fun onLatestEventReceived(data: DataModel)
    }


    fun initWebRTCClient(username: String){
        webRTCClient.listener = this
        webRTCClient.initializeWebRTCClient(username, object: MyPeerObserver(){
            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                //notify the creator of this class that there is a new stream available
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    webRTCClient.sendIceCandidate(target!!, it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                if(newState==PeerConnection.PeerConnectionState.CONNECTED){
                    //1. change my status to in call
                    changeMyStatus(UserStatus.IN_CALL)
                    //2. clear latest event inside my user section in firebase database
                    firebaseClient.clearLatestEvent()
                }
            }
        })
    }

    fun initLocalSurfaceView(view:SurfaceViewRenderer,isVideoCall: Boolean){
        webRTCClient.initLocalSurfaceView(view,isVideoCall)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer){
        webRTCClient.initRemoteSurfaceView(view)
    }

    fun startCall(){
        webRTCClient.call(target!!)
    }
    fun endCall(){
        webRTCClient.closeConnection()
        changeMyStatus(UserStatus.ONLINE)
    }
    fun sendEndCall(){
        onTransferEventToSocket(
            DataModel(type= EndCall, target = target!!)
        )
    }

    private fun changeMyStatus(status: UserStatus) {
        firebaseClient.changeMyStatus(status)
    }

    fun toggleAudio(shouldBeMuted : Boolean){
        webRTCClient.toggleAudio(shouldBeMuted)
    }
    fun toggleVideo(shouldBeMuted : Boolean){
        webRTCClient.toggleVideo(shouldBeMuted)
    }

    fun switchCamera(){
        webRTCClient.switchCamera()
    }

    override fun onTransferEventToSocket(data: DataModel) {
        firebaseClient.sendMessageToOtherClient(data){}
    }
}