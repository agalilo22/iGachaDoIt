package com.example.igachadoit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient // Declare GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth // Declare Firebase Auth instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        // Configure Google Sign-In options for Firebase Authentication
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id)) // Required for Firebase Auth with Google
            .requestEmail() // Request user's email
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso) // Initialize GoogleSignInClient

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance()

        val googleSignInButton: Button = findViewById(R.id.googleSignInButton)
        val guestLoginButton: Button = findViewById(R.id.guestLoginButton)

        // Check if user is already signed in with Firebase Authentication
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            navigateToSessionActivity() // If user is signed in, go to SessionActivity
        }

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
        startActivityForResult(signInIntent, 101) // Request code 101 for Google Sign-In
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) { // Check if the result is from Google Sign-In
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java)
            // Google Sign-In was successful, authenticate with Firebase using the Google ID Token
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // Google Sign-In failed
            Log.w("GoogleSignIn", "Google sign-in failed", e)
            Toast.makeText(this, "Google Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Firebase Authentication successful
                    val user: FirebaseUser? = firebaseAuth.currentUser
                    updateUI(user) // Update UI for successful sign-in
                } else {
                    // Firebase Authentication failed
                    Log.w("FirebaseGoogleAuth", "Firebase sign-in failed", task.exception)
                    Toast.makeText(this, "Firebase sign-in failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null) // Update UI for failed sign-in
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "Firebase Google Sign-in successful!", Toast.LENGTH_SHORT).show()
            navigateToSessionActivity()
        } else {
            // Sign-in failed, handle UI updates if needed
        }
    }

    private fun navigateToSessionActivity() {
        val intent = Intent(this, SessionActivity::class.java)
        startActivity(intent)
        finish() // Optional: Finish LoginActivity so user can't go back easily
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