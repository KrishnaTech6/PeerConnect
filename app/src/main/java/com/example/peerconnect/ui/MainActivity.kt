package com.example.peerconnect.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.peerconnect.R
import com.example.peerconnect.databinding.ActivityMainBinding
import com.example.peerconnect.repository.MainRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var username : String? = null
    private val TAG = "MainActivity"

    @Inject lateinit var mainRepository: MainRepository

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

    private fun init(){

        username = intent.getStringExtra("username")
        if(username==null) finish()

        //observe other user status
        subscribeObservers()

        //start foreground service to listen for incoming calls
        startMyService()

    }

    private fun subscribeObservers() {
        mainRepository.observeUserStatus{
            Log.d(TAG, "subscribeObservers: $it")
        }
    }

    private fun startMyService() {

    }
}