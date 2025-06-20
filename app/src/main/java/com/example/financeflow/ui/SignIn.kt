package com.example.financeflow.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflow.databinding.ActivitySignInBinding

class SignIn : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    // credentials for now
    private val validEmail = "user@gmail.com"
    private val validPassword = "123456"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.signIn.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (!validateEmail(email) || !validatePassword(password)) {
                return@setOnClickListener
            }

            if (email == validEmail && password == validPassword) {
                // Go to dashboard
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                binding.passwordEditText.text?.clear()
            }
        }

        binding.createAccountButton.setOnClickListener {
            Toast.makeText(this, "Redirect to registration screen", Toast.LENGTH_SHORT).show()

        }

        binding.forgotPasswordLink.setOnClickListener {
            Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show()
            // Handle password reset logic
        }
    }

    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            binding.emailEditText.error = "Email cannot be empty"
            binding.emailEditText.requestFocus()
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Please enter a valid email"
            binding.emailEditText.requestFocus()
            false
        } else {
            binding.emailEditText.error = null
            true
        }
    }

    private fun validatePassword(password: String): Boolean {
        return if (password.isEmpty()) {
            binding.passwordEditText.error = "Password cannot be empty"
            binding.passwordEditText.requestFocus()
            false
        } else if (password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters"
            binding.passwordEditText.requestFocus()
            false
        } else {
            binding.passwordEditText.error = null
            true
        }
    }
}
