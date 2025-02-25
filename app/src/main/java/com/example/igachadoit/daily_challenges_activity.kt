package com.example.igachadoit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView

data class Challenge(val description: String, val reward: String, var completed: Boolean = false)

class DailyChallengesActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var challenges: MutableList<Challenge> // Mutable list for dynamic challenges
    private lateinit var adapter: DailyChallengesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.daily_challenges_activity)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
        }

        val challengesRecyclerView: RecyclerView = findViewById(R.id.challengesRecyclerView)
        challengesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Generate random challenges
        challenges = generateRandomChallenges()

        adapter = DailyChallengesAdapter(challenges) { position ->
            claimReward(position)
        }
        challengesRecyclerView.adapter = adapter
    }

    private fun generateRandomChallenges(): MutableList<Challenge> {
        val allChallenges = listOf(
            Challenge("Complete 3 sessions today.", "5 Pulls"),
            Challenge("Study for 2 hours.", "3 Pulls"),
            Challenge("Maintain your streak.", "1 Pull"),
            Challenge("Complete a hard session.", "2 pulls"),
            Challenge("Complete 5 easy sessions.", "6 pulls"),
            Challenge("Study for 30 minutes.", "1 pull")
        )
        val randomChallenges = allChallenges.shuffled().take(3).toMutableList() // Select 3 random challenges
        return randomChallenges
    }

    private fun claimReward(position: Int) {
        val challenge = challenges[position]
        if (challenge.completed) {
            // Add pull reward logic here
            val rewardAmount = challenge.reward.split(" ")[0].toInt() // Extract the number of pulls
            addPulls(rewardAmount)
            Toast.makeText(this, "Reward claimed: ${challenge.reward}", Toast.LENGTH_SHORT).show()
            challenge.completed = false // Reset completion
            adapter.notifyItemChanged(position) // Update the adapter
        } else {
            Toast.makeText(this, "Challenge not completed yet.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addPulls(amount: Int) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val currentPulls = sharedPreferences.getInt("pulls", 0)
        val editor = sharedPreferences.edit()
        editor.putInt("pulls", currentPulls + amount)
        editor.apply()
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
}

class DailyChallengesAdapter(
    private val challenges: List<Challenge>,
    private val onItemClick: (Int) -> Unit // Callback for item click
) : RecyclerView.Adapter<DailyChallengesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val challengeDescriptionTextView: TextView = itemView.findViewById(R.id.challengeDescriptionTextView)
        val challengeRewardTextView: TextView = itemView.findViewById(R.id.challengeRewardTextView)
        val claimButton: Button = itemView.findViewById(R.id.claimButton) // Added claim button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.challenge_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val challenge = challenges[position]
        holder.challengeDescriptionTextView.text = challenge.description
        holder.challengeRewardTextView.text = "Reward: ${challenge.reward}"
        holder.claimButton.isEnabled = challenge.completed
        holder.claimButton.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int = challenges.size
}