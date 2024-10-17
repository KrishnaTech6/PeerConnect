package com.example.peerconnect.repository

import com.example.peerconnect.firebaseClient.FirebaseClient
import com.example.peerconnect.utils.DataModel
import com.example.peerconnect.utils.DataModelType.StartAudioCall
import com.example.peerconnect.utils.DataModelType.StartVideoCall
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient
) {
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

    interface Listener{
        fun onLatestEventReceived(data: DataModel)
    }
}