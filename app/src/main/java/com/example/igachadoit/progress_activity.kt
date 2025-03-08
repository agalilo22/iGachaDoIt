package com.example.igachadoit

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProgressActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences // Add SharedPreferences
    private var googleAccount: GoogleSignInAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progress_activity)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.open, R.string.close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
            drawerLayout.closeDrawers()
            true
        }

        firestore = FirebaseFirestore.getInstance()
        googleAccount = GoogleSignIn.getLastSignedInAccount(this)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) // Initialize SharedPreferences

        loadProgressData() // Use a single method to load progress based on user type
    }

    private fun loadProgressData() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            // Guest user - load from SharedPreferences
            loadProgressFromSharedPreferences()
        } else {
            // Logged-in user - load from Firestore
            loadProgressFromFirestore()
        }
    }


    private fun loadProgressFromFirestore() {
        googleAccount?.let { account ->
            val userId = account.id ?: return
            firestore.collection("user_data").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val dailyStreak = document.getLong("dailyStreak")?.toInt() ?: 0
                        val weeklyStreak = document.getLong("weeklyStreak")?.toInt() ?: 0
                        val totalPulls = document.getLong("totalPulls")?.toInt() ?: 0

                        updateProgressDisplay(dailyStreak, weeklyStreak, totalPulls)
                    } else {
                        Toast.makeText(this, "No progress data found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load progress: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        } ?: run {
            Toast.makeText(this, "User not signed in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProgressFromSharedPreferences() {
        val dailyStreak = sharedPreferences.getInt("dailyStreak", 0)
        val weeklyStreak = sharedPreferences.getInt("weeklyStreak", 0)
        val totalPulls = sharedPreferences.getInt("pulls", 0) // Guests pulls are stored under "pulls"

        updateProgressDisplay(dailyStreak, weeklyStreak, totalPulls)
        Toast.makeText(this, "Progress loaded locally.", Toast.LENGTH_SHORT).show() // Indicate local load
    }

    private fun updateProgressDisplay(dailyStreak: Int, weeklyStreak: Int, totalPulls: Int) {
        val dailyStreakTextView: TextView = findViewById(R.id.dailyStreakTextView)
        val weeklyStreakTextView: TextView = findViewById(R.id.weeklyStreakTextView)
        val totalPullsTextView: TextView = findViewById(R.id.totalPullsTextView)

        dailyStreakTextView.text = "Daily Streak: $dailyStreak Days"
        weeklyStreakTextView.text = "Weekly Streak: $weeklyStreak Weeks"
        totalPullsTextView.text = "Total Pulls: $totalPulls"
    }

    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_home -> startActivity(Intent(this, SessionActivity::class.java))
            R.id.nav_progress -> startActivity(Intent(this, ProgressActivity::class.java))
            R.id.nav_session_history -> startActivity(
                Intent(
                    this,
                    SessionHistoryActivity::class.java
                )
            )
            R.id.nav_daily_challenges -> startActivity(
                Intent(
                    this,
                    DailyChallengesActivity::class.java
                )
            )
            R.id.nav_reward_gallery -> startActivity(
                Intent(
                    this,
                    RewardGalleryActivity::class.java
                )
            )
        }
        drawerLayout.closeDrawer(navigationView)
        return true
    }
}