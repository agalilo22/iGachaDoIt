package com.example.igachadoit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient // Declare the variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        // Initialize Google Sign-In options (THIS IS THE KEY PART)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso) // Initialize the client

        val googleSignInButton: Button = findViewById(R.id.googleSignInButton)
        val guestLoginButton: Button = findViewById(R.id.guestLoginButton)

        // Google Sign-In Button action
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        // Guest Login Button action
        guestLoginButton.setOnClickListener {
            showGuestWarningDialog()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, 100) // You'll need to handle the result
    }

    private fun showGuestWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle("Continue as Guest")
            .setMessage("Your progress will not be saved to the cloud. Do you want to proceed?")
            .setPositiveButton("Continue") { _, _ ->
                showDifficultyInfoDialog()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun showDifficultyInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("Difficulty Information")
            .setMessage("Please note, difficulty durations are set to 5 seconds for testing purposes.")
            .setPositiveButton("OK") { dialog, _ ->
                val intent = Intent(this, SessionActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Logged in as Guest", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }
}