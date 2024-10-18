package com.example.peerconnect.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.peerconnect.R
import com.example.peerconnect.databinding.ActivityCallBinding

class CallActivity : AppCompatActivity() {
    private var isCaller:Boolean = true
    private var isVideoCall: Boolean = true
    private var target:String? = null
    private lateinit var binding: ActivityCallBinding
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
            if(!isVideoCall){
                toggleCameraButton.isVisible= false
                screenShareButton.isVisible= false
                switchCameraButton.isVisible= false
            }
        }
    }
}