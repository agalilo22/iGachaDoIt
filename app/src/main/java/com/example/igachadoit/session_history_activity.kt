package com.example.igachadoit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class SessionHistoryActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sessionHistoryRecyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore

    private val SESSION_HISTORY_KEY = "session_history"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.session_history_activity)

        firestore = FirebaseFirestore.getInstance()

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        sessionHistoryRecyclerView = findViewById(R.id.sessionHistoryRecyclerView)

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
        }

        // Setup RecyclerView
        sessionHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        loadSessionHistory()
    }

    private fun loadSessionHistory() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            // Load from Firestore for authenticated users
            val googleAccount = GoogleSignIn.getLastSignedInAccount(this)
            googleAccount?.let { account ->
                val userId = account.id ?: return
                firestore.collection("users").document(userId)
                    .collection("sessionHistory")
                    .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { result ->
                        val sessions = result.map { doc ->
                            val startTime = doc.getTimestamp("startTime") ?: Timestamp.now()
                            val durationSeconds = doc.getLong("durationSeconds")?.toLong() ?: 0L
                            val completed = doc.getBoolean("completed") ?: false
                            Session(startTime, durationSeconds, completed)
                        }
                        sessionHistoryRecyclerView.adapter = SessionHistoryAdapter(sessions)
                    }
                    .addOnFailureListener { e ->
                        // Fallback to local data if Firestore fails
                        loadLocalSessionHistory()
                    }
            }
        } else {
            // Load from SharedPreferences for guest users
            loadLocalSessionHistory()
        }
    }

    private fun loadLocalSessionHistory() {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val currentHistoryJson = sharedPreferences.getString(SESSION_HISTORY_KEY, "[]")
        val type = object : TypeToken<MutableList<Session>>() {}.type
        val sessionHistoryList: List<Session> = Gson().fromJson(currentHistoryJson, type) ?: emptyList()
        sessionHistoryRecyclerView.adapter = SessionHistoryAdapter(sessionHistoryList)
    }

    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_home -> startActivity(Intent(this, SessionActivity::class.java))
            R.id.nav_progress -> startActivity(Intent(this, ProgressActivity::class.java))
            R.id.nav_session_history -> {} // Already here
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

    // Data class for session (already exists in SessionActivity, reused here)
    data class Session(val startTime: Timestamp?, val durationSeconds: Long, val completed: Boolean)

    // RecyclerView Adapter
    inner class SessionHistoryAdapter(private val sessions: List<Session>) :
        RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dateTextView: TextView = view.findViewById(R.id.sessionDate)
            val timeTextView: TextView = view.findViewById(R.id.sessionTime)
            val durationTextView: TextView = view.findViewById(R.id.sessionDuration)
            val statusTextView: TextView = view.findViewById(R.id.sessionStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.session_history_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val session = sessions[position]
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            // Date (MM/DD/YYYY)
            holder.dateTextView.text = session.startTime?.toDate()?.let { dateFormat.format(it) } ?: "N/A"

            // Time (HH:MM:SS)
            holder.timeTextView.text = session.startTime?.toDate()?.let { timeFormat.format(it) } ?: "N/A"

            // Duration (formatted as MM:SS)
            val minutes = session.durationSeconds / 60
            val seconds = session.durationSeconds % 60
            holder.durationTextView.text = String.format("%02d:%02d", minutes, seconds)

            // Status
            holder.statusTextView.text = if (session.completed) "Completed" else "Canceled"
        }

        override fun getItemCount(): Int = sessions.size
    }
}