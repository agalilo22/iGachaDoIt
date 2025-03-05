package com.example.igachadoit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Session(val startTime: Timestamp?, val durationSeconds: Long, val completed: Boolean)

class SessionHistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sessionHistoryRecyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private var googleAccount: GoogleSignInAccount? = null

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

        sessionHistoryRecyclerView = findViewById(R.id.sessionHistoryRecyclerView)
        sessionHistoryRecyclerView.layoutManager = LinearLayoutManager(this)

        firestore = FirebaseFirestore.getInstance()
        googleAccount = GoogleSignIn.getLastSignedInAccount(this)

        loadSessionHistory()
    }

    private fun loadSessionHistory() {
        googleAccount?.let { account ->
            val userId = account.id ?: return
            val sessionHistoryCollection = firestore.collection("users").document(userId).collection("sessionHistory")

            sessionHistoryCollection.get()
                .addOnSuccessListener { documents ->
                    val sessions = mutableListOf<Session>()
                    for (document in documents) {
                        val startTime = document.getTimestamp("startTime")
                        val durationSeconds = document.getLong("durationSeconds") ?: 0L
                        val completed = document.getBoolean("completed") ?: false
                        sessions.add(Session(startTime, durationSeconds, completed))
                    }
                    val adapter = SessionHistoryAdapter(sessions)
                    sessionHistoryRecyclerView.adapter = adapter
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading session history: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(this, "User not signed in.", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle navigation item selection
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

    class SessionHistoryAdapter(private val sessions: List<Session>) :
        RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val sessionDateTextView: TextView = itemView.findViewById(R.id.sessionDateTextView)
            val sessionDurationTextView: TextView = itemView.findViewById(R.id.sessionDurationTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.session_history_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val session = sessions[position]
            holder.sessionDateTextView.text = formatDate(session.startTime?.toDate())
            holder.sessionDurationTextView.text = formatDuration(session.durationSeconds)
        }

        override fun getItemCount(): Int = sessions.size

        private fun formatDate(date: Date?): String {
            return if (date != null) {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())
                format.format(date)
            } else {
                "N/A"
            }
        }

        private fun formatDuration(durationSeconds: Long): String {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format(Locale.getDefault(), "Duration: %02d:%02d", minutes, seconds)
        }
    }
}