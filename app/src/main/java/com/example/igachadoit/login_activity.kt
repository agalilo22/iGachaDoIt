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

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()

        val googleSignInButton: Button = findViewById(R.id.googleSignInButton)
        val guestLoginButton: Button = findViewById(R.id.guestLoginButton)

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            navigateToSessionActivity()
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        guestLoginButton.setOnClickListener {
            showGuestWarningDialog()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, 101)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "Google sign-in failed", e)
            Toast.makeText(this, "Google Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = firebaseAuth.currentUser
                    updateUI(user)
                } else {
                    Log.w("FirebaseGoogleAuth", "Firebase sign-in failed", task.exception)
                    Toast.makeText(this, "Firebase sign-in failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
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
        finish()
    }

    private fun showGuestWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle("Continue as Guest")
            .setMessage("Your progress will be saved locally on this device. If you uninstall the app or clear data, your progress will be lost. Do you want to proceed?")
            .setPositiveButton("Continue") { _, _ ->
                val intent = Intent(this, SessionActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Logged in as Guest", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
}