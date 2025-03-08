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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth


data class Session(val startTime: Timestamp?, val durationSeconds: Long, val completed: Boolean)

class SessionHistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sessionHistoryRecyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private var googleAccount: GoogleSignInAccount? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var isGuestUser = false // Flag to track guest user


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

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
        }

        sessionHistoryRecyclerView = findViewById(R.id.sessionHistoryRecyclerView)
        sessionHistoryRecyclerView.layoutManager = LinearLayoutManager(this)

        firestore = FirebaseFirestore.getInstance()
        googleAccount = GoogleSignIn.getLastSignedInAccount(this)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        isGuestUser = sharedPreferences.getBoolean(Constants.PREF_IS_GUEST_USER, false) // Retrieve guest user status


        fetchSessionHistory()
    }


    private fun fetchSessionHistory() {
        if (isGuestUser) {
            loadSessionHistoryFromSharedPreferences()
        } else {
            fetchSessionHistoryFromFirestore()
        }
    }


    private fun fetchSessionHistoryFromFirestore() {
        googleAccount?.let { account ->
            val userId = account.id ?: return
            firestore.collection("users").document(userId)
                .collection("sessionHistory")
                .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val sessions = documents.map { document ->
                        val startTime = document.getTimestamp("startTime")
                        val durationSeconds = document.getLong("durationSeconds") ?: 0
                        val completed = document.getBoolean("completed") ?: false
                        Session(startTime, durationSeconds, completed)
                    }.toMutableList()
                    updateRecyclerView(sessions)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to load session history from cloud: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {
            Toast.makeText(this, "User not signed in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSessionHistoryFromSharedPreferences() {
        val sessionHistoryJson = sharedPreferences.getString(Constants.PREF_GUEST_SESSION_HISTORY, "[]")
        val type = object : TypeToken<MutableList<Session>>() {}.type
        val sessionsList: MutableList<Session> = Gson().fromJson(sessionHistoryJson, type) ?: mutableListOf()
        updateRecyclerView(sessionsList)
        Toast.makeText(this, "Session history loaded locally.", Toast.LENGTH_SHORT).show()
    }

    private fun saveSessionHistoryToSharedPreferences(session: Session) {
        val sessionHistoryJson = sharedPreferences.getString(Constants.PREF_GUEST_SESSION_HISTORY, "[]")
        val type = object : TypeToken<MutableList<Session>>() {}.type
        val sessionsList: MutableList<Session> = Gson().fromJson(sessionHistoryJson, type) ?: mutableListOf()
        sessionsList.add(session)
        val editor = sharedPreferences.edit()
        editor.putString(Constants.PREF_GUEST_SESSION_HISTORY, Gson().toJson(sessionsList))
        editor.apply()
    }


    private fun updateRecyclerView(sessions: List<Session>) {
        sessionHistoryRecyclerView.adapter = SessionHistoryAdapter(sessions)
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


    class SessionHistoryAdapter(private val sessions: List<Session>) :
        RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sessionDateTextView: TextView = view.findViewById(R.id.sessionDateTextView) // Corrected ID to sessionDateTextView
            val sessionDurationTextView: TextView = view.findViewById(R.id.sessionDurationTextView) // Corrected ID to sessionDurationTextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.session_history_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val session = sessions[position]
            val formattedDate = session.startTime?.toDate()?.let { formatDate(it) } ?: "N/A"
            val durationMinutes = session.durationSeconds / 60
            val completionStatus = if (session.completed) "Completed" else "Canceled"
            holder.sessionDateTextView.text = "Date: $formattedDate" // Set Date to sessionDateTextView
            holder.sessionDurationTextView.text = "Duration: ${durationMinutes} min, Status: $completionStatus" // Set Duration and Status to sessionDurationTextView
        }

        private fun formatDate(date: Date): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return dateFormat.format(date)
        }

        override fun getItemCount(): Int = sessions.size
    }

    companion object {
        // Static method to save session history, can be called from SessionActivity or wherever session ends
        fun saveSession(context: Context, session: Session) {
            val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val isGuestUser = sharedPreferences.getBoolean(Constants.PREF_IS_GUEST_USER, false)
            val firestore = FirebaseFirestore.getInstance()
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)

            if (isGuestUser) {
                val sessionHistoryJson = sharedPreferences.getString(Constants.PREF_GUEST_SESSION_HISTORY, "[]")
                val type = object : TypeToken<MutableList<Session>>() {}.type
                val sessionsList: MutableList<Session> = Gson().fromJson(sessionHistoryJson, type) ?: mutableListOf()

                // Add the new session to the list
                sessionsList.add(session)

                // Save the updated list back to SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString(Constants.PREF_GUEST_SESSION_HISTORY, Gson().toJson(sessionsList))
                editor.apply()
            } else {
                // Save session history to Firestore for logged-in users
                googleAccount?.let { account ->
                    val userId = account.id ?: return // Return if no user ID
                    val sessionData = hashMapOf(
                        "startTime" to session.startTime,
                        "durationSeconds" to session.durationSeconds,
                        "completed" to session.completed
                    )

                    firestore.collection("users").document(userId)
                        .collection("sessionHistory")
                        .add(sessionData)
                        .addOnSuccessListener { documentReference ->
                            //Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.id)
                        }
                        .addOnFailureListener { e ->
                            //Log.w(TAG, "Error adding document", e)
                        }
                }
            }
        }
    }
}