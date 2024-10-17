package com.example.peerconnect.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.peerconnect.R
import com.example.peerconnect.databinding.ActivityLoginBinding
import com.example.peerconnect.repository.MainRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    @Inject lateinit var mainRepository: MainRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
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
        binding.apply {
            btn.setOnClickListener{
                mainRepository.login(usernameEt.text.toString(), passwordEt.text.toString()) { isDone, reason ->
                    if(!isDone)
                        Toast.makeText(this@LoginActivity, reason, Toast.LENGTH_SHORT).show()
                    else {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("username", usernameEt.text.toString())
                        })
                    }
                }
        }
    }
    }
}