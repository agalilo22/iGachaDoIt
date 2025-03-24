package com.example.igachadoit

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class ProgressActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var progressRecyclerView: RecyclerView
    private lateinit var progressContainer: MaterialCardView

    // Progress data
    private var dailyStreak = 0
    private var weeklyStreak = 0
    private var totalPulls = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progress_activity)

        toolbar = findViewById(R.id.toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        progressRecyclerView = findViewById(R.id.progressRecyclerView)
        progressContainer = findViewById(R.id.progressContainer)

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
        }

        // Load data from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        dailyStreak = sharedPreferences.getInt("dailyStreak", 0)
        weeklyStreak = sharedPreferences.getInt("weeklyStreak", 0)
        totalPulls = sharedPreferences.getInt("pulls", 0)

        // Setup RecyclerView
        progressRecyclerView.layoutManager = LinearLayoutManager(this)
        val progressItems = listOf(
            ProgressItem("Daily Streak", dailyStreak),
            ProgressItem("Weekly Streak", weeklyStreak),
            ProgressItem("Total Pulls", totalPulls)
        )
        progressRecyclerView.adapter = ProgressAdapter(progressItems)
    }

    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_progress -> {} // Already here

            R.id.nav_home -> startActivity(Intent(this, SessionActivity::class.java))
            R.id.nav_session_history -> startActivity(Intent(this, SessionHistoryActivity::class.java))
            R.id.nav_daily_challenges -> startActivity(Intent(this, DailyChallengesActivity::class.java))
            R.id.nav_reward_gallery -> startActivity(Intent(this, RewardGalleryActivity::class.java))
            R.id.nav_logout -> {
                // Sign out from Firebase Auth
                FirebaseAuth.getInstance().signOut()

                // Redirect the user to the login activity
                val intent = Intent(this, LoginActivity::class.java) // Replace LoginActivity with your actual login activity class
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
                startActivity(intent)
                finish() // Optional: Finish the current activity
            }
        }
        drawerLayout.closeDrawer(navigationView)
        return true
    }


    // Data class for progress items
    data class ProgressItem(val label: String, val value: Int)

    // RecyclerView Adapter
    inner class ProgressAdapter(private val items: List<ProgressItem>) :
        RecyclerView.Adapter<ProgressAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val labelTextView: TextView = view.findViewById(R.id.progressLabel)
            val valueTextView: TextView = view.findViewById(R.id.progressValue)
            val itemBackground: LinearLayout = view.findViewById(R.id.itemBackground)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_progress, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.labelTextView.text = item.label
            holder.valueTextView.text = item.value.toString()

            // Calculate intensity based on the item's value
            val intensity = when {
                item.value > 20 -> 0.9f // Very dark shade
                item.value > 15 -> 0.7f // Dark shade
                item.value > 10 -> 0.5f // Medium shade
                item.value > 5 -> 0.3f  // Light shade
                else -> 0.1f           // Very light shade
            }

            // Base color (green from your theme: #4CAF50)
            val baseColor = Color.parseColor("#AEE6B0")
            val hsv = FloatArray(3)
            Color.colorToHSV(baseColor, hsv)

            // Light color (left side)
            hsv[2] = hsv[2] * 0.9f // Slightly lighter than full brightness
            val lightColor = Color.HSVToColor(hsv)

            // Dark color (right side) based on intensity
            hsv[2] = hsv[2] * (1f - intensity) // Darken based on value
            val darkColor = Color.HSVToColor(hsv)

            // Apply gradient from left (light) to right (dark)
            val gradient = LinearGradient(
                0f, 0f, // Start X, Y (left)
                holder.itemBackground.width.toFloat(), 0f, // End X, Y (right)
                lightColor, // Start color
                darkColor, // End color
                Shader.TileMode.CLAMP
            )

            // Set the gradient as the background
            holder.itemBackground.background = null // Clear any existing background
            holder.itemBackground.post {
                // Ensure width is available after layout
                val updatedGradient = LinearGradient(
                    0f, 0f,
                    holder.itemBackground.width.toFloat(), 0f,
                    lightColor,
                    darkColor,
                    Shader.TileMode.CLAMP
                )
                holder.itemBackground.background = android.graphics.drawable.ShapeDrawable().apply {
                    paint.shader = updatedGradient
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}