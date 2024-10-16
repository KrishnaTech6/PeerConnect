package com.example.peerconnect.firebaseClient

import com.example.peerconnect.utils.FirebaseFieldName.PASSWORD
import com.example.peerconnect.utils.FirebaseFieldName.STATUS
import com.example.peerconnect.utils.UserStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import javax.inject.Inject

class FirebaseClient @Inject constructor(
    private val dbRef: DatabaseReference,
    private val gson: Gson
){
    private var currentUserName: String? = null
    private fun setUserName(username: String){
        this.currentUserName = username
    }
    fun login(username: String, password: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object: ValueEventListener{
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

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
}