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

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        // Initialize Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
        startActivityForResult(signInIntent, 100)
    }

    private fun showGuestWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle("Continue as Guest")
            .setMessage("Your progress will not be saved to the cloud. Do you want to proceed?")
            .setPositiveButton("Continue") { _, _ ->
                // Navigate to the main app functionality
                val intent = Intent(this, SessionActivity::class.java) // Replace with your main activity
                startActivity(intent)
                Toast.makeText(this, "Logged in as Guest", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
}
