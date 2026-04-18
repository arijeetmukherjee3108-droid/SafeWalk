package com.example.safeher.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.safeher.MainActivity
import com.example.safeher.R
import com.example.safeher.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupListeners()
    }

    private fun setupTabs() {
        binding.loginTab.setOnClickListener {
            updateTabUI(isLogin = true)
        }
        binding.registerTab.setOnClickListener {
            updateTabUI(isLogin = false)
        }
        // Default to login
        updateTabUI(isLogin = true)
    }

    private fun updateTabUI(isLogin: Boolean) {
        if (isLogin) {
            binding.loginTab.setBackgroundResource(R.drawable.bg_tab_active)
            binding.loginTab.setTextColor(getColor(R.color.white))
            binding.registerTab.setBackgroundResource(android.R.color.transparent)
            binding.registerTab.setTextColor(getColor(R.color.sub))
            
            binding.registerFields.visibility = View.GONE
            binding.confirmPasswordContainer.visibility = View.GONE
            binding.loginButton.text = "Login"
        } else {
            binding.registerTab.setBackgroundResource(R.drawable.bg_tab_active)
            binding.registerTab.setTextColor(getColor(R.color.white))
            binding.loginTab.setBackgroundResource(android.R.color.transparent)
            binding.loginTab.setTextColor(getColor(R.color.sub))
            
            binding.registerFields.visibility = View.VISIBLE
            binding.confirmPasswordContainer.visibility = View.VISIBLE
            binding.loginButton.text = "Register"
        }
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            // For now, just navigate to MainActivity to test UI
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
