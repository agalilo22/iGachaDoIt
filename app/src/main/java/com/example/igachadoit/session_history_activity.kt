package com.example.igachadoit
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Session(val date: Date, val durationMinutes: Int)

class SessionHistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.session_history_activity)

        // Initialize Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Handle navigation item selection
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
        }


        val sessionHistoryRecyclerView: RecyclerView = findViewById(R.id.sessionHistoryRecyclerView)
        sessionHistoryRecyclerView.layoutManager = LinearLayoutManager(this)

        // Sample session data (replace with your actual data)
        val sessions = listOf(
            Session(Date(), 60),
            Session(Date(System.currentTimeMillis() - 86400000), 90), // Yesterday
            Session(Date(System.currentTimeMillis() - 172800000), 120) // 2 days ago
            // ... more sessions
        )

        val adapter = SessionHistoryAdapter(sessions)
        sessionHistoryRecyclerView.adapter = adapter
    }
    // Handle navigation item selection
    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_home -> startActivity(Intent(this, SessionActivity::class.java))  // Launch SessionActivity on home press
            R.id.nav_progress -> startActivity(Intent(this, ProgressActivity::class.java))
            R.id.nav_session_history -> startActivity(Intent(this, SessionHistoryActivity::class.java))
            R.id.nav_daily_challenges -> startActivity(Intent(this, DailyChallengesActivity::class.java))
            R.id.nav_reward_gallery -> startActivity(Intent(this, RewardGalleryActivity::class.java))
        }
        drawerLayout.closeDrawer(navigationView)
        return true
}

class SessionHistoryAdapter(private val sessions: List<Session>) :
    RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val sessionDateTextView: TextView = itemView.findViewById(R.id.sessionDateTextView)
        val sessionDurationTextView: TextView = itemView.findViewById(R.id.sessionDurationTextView)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.session_history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())
        holder.sessionDateTextView.text = dateFormat.format(session.date)
        holder.sessionDurationTextView.text = "Duration: ${session.durationMinutes} minutes"
    }

    override fun getItemCount(): Int = sessions.size
}
}