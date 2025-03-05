package com.example.igachadoit

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class ProgressActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var sharedPreferences: SharedPreferences

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

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        updateProgressDisplay()
    }

    private fun updateProgressDisplay() {
        val dailyStreak = sharedPreferences.getInt("dailyStreak", 0)
        val weeklyStreak = sharedPreferences.getInt("weeklyStreak", 0)
        val totalPulls = sharedPreferences.getInt("pulls", 0)

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
            R.id.nav_session_history -> startActivity(Intent(this, SessionHistoryActivity::class.java))
            R.id.nav_daily_challenges -> startActivity(Intent(this, DailyChallengesActivity::class.java))
            R.id.nav_reward_gallery -> startActivity(Intent(this, RewardGalleryActivity::class.java))
        }
        drawerLayout.closeDrawer(navigationView)
        return true
    }

    override fun onResume() {
        super.onResume()
        updateProgressDisplay()
    }
}