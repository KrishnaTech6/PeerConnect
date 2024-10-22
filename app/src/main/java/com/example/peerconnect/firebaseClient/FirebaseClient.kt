package com.example.peerconnect.firebaseClient

import com.example.peerconnect.utils.DataModel
import com.example.peerconnect.utils.FirebaseFieldName.LATEST_EVENT
import com.example.peerconnect.utils.FirebaseFieldName.PASSWORD
import com.example.peerconnect.utils.FirebaseFieldName.STATUS
import com.example.peerconnect.utils.MyEventListener
import com.example.peerconnect.utils.UserStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor(
    private val dbRef: DatabaseReference,
    private val gson: Gson
){
    private var currentUserName: String? = null
    private fun setUserName(username: String){
        this.currentUserName = username
    }
    fun login(username: String, password: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object: MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                //If the current user exists
                if(snapshot.hasChild(username)){
                    val dbPassword = snapshot.child(username).child(PASSWORD).value
                    if(password== dbPassword){
                        //password correct and sign in
                        dbRef.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                            .addOnCompleteListener{
                                setUserName(username)
                                done(true, null)
                            }
                            .addOnFailureListener{
                                done(false, it.message)
                            }
                    }else{
                        //password incorrect notify user
                        done(false, "Incorrect Password")
                    }

                }else{
                    dbRef.child(username).child(PASSWORD).setValue(password).addOnCompleteListener{
                        dbRef.child(username).child(STATUS).setValue(UserStatus.ONLINE).addOnCompleteListener{
                            setUserName(username)
                            done(true, null)
                        }.addOnFailureListener{
                            done(false, it.message)
                        }
                    }.addOnFailureListener {
                        done(false, it.message)
                    }
                }
            }
        })
    }

    fun observeUserStatus(status: (List<Pair<String, String>>) -> Unit) {
        dbRef.addValueEventListener(object: MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.filter { it.key != currentUserName }.map {
                    it.key!! to it.child(STATUS).value.toString()
                }
                status(list)
            }

        })


    }

    fun subscribeForLatestEvents(listener: Listener){
        try{
            dbRef.child(currentUserName!!).child(LATEST_EVENT).addValueEventListener(object: MyEventListener(){
                override fun onDataChange(snapshot: DataSnapshot) {
                    super.onDataChange(snapshot)
                    val event = try {
                        gson.fromJson(snapshot.value.toString(), DataModel::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                    event?.let {
                        //Data flows from FirebaseClient → MainRepository → MainService.
                        listener.onLatestEventReceived(it)
                    }
                }
            })
        }catch (e: Exception){
            e.printStackTrace()
        }

    }
    fun sendMessageToOtherClient(message: DataModel, success:(Boolean)->Unit){
        val convertedMessage = gson.toJson(message.copy(sender = currentUserName))
        dbRef.child(message.target).child(LATEST_EVENT).setValue(convertedMessage)
            .addOnCompleteListener{
                success(true)
            }
            .addOnFailureListener{
                success(false)
            }
    }

    fun changeMyStatus(status: UserStatus) {
        dbRef.child(currentUserName!!).child(STATUS).setValue(status)
    }

    fun clearLatestEvent() {
        dbRef.child(currentUserName!!).child(LATEST_EVENT).setValue(null)
    }

    fun logOff(function: () -> Unit) {
        dbRef.child(currentUserName!!).child(STATUS).setValue(UserStatus.OFFLINE)
        .addOnCompleteListener{
            function()
        }

    }

    interface Listener{
        fun onLatestEventReceived(event: DataModel)
    }
}