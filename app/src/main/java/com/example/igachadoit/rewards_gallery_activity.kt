package com.example.igachadoit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore

data class Reward(val name: String, val category: String, val imageResId: Int)

class RewardGalleryActivity : AppCompatActivity() {

    private lateinit var rewardRecyclerView: RecyclerView
    private lateinit var rewardCategorySpinner: Spinner
    private var rewards: MutableList<Reward> = mutableListOf()
    private lateinit var adapter: RewardAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var firestore: FirebaseFirestore
    private var googleAccount: GoogleSignInAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reward_gallery_activity)

        rewardRecyclerView = findViewById(R.id.rewardRecyclerView)
        rewardCategorySpinner = findViewById(R.id.rewardCategorySpinner)

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

        firestore = FirebaseFirestore.getInstance()
        googleAccount = GoogleSignIn.getLastSignedInAccount(this)

        loadRewardsFromFirestore()

        rewardRecyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = RewardAdapter(rewards)
        rewardRecyclerView.adapter = adapter

        setupSpinner()
    }

    private fun setupSpinner() {
        val categories = mutableListOf("All")
        categories.addAll(rewards.distinctBy { it.category }.map { it.category })
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        rewardCategorySpinner.adapter = spinnerAdapter

        rewardCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                filterRewards(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                filterRewards("All")
            }
        }
    }

    private fun loadRewardsFromFirestore() {
        googleAccount?.let { account ->
            val userId = account.id ?: return
            val rewardsCollection = firestore.collection("users").document(userId).collection("rewards")

            rewardsCollection.get()
                .addOnSuccessListener { documents ->
                    rewards.clear()
                    val uniqueRewards = HashSet<String>() // Use HashSet to track unique names
                    for (document in documents) {
                        val prizeName = document.getString("prizeName") ?: ""
                        val imageResId = getImageResIdFromName(prizeName)
                        val category = getCategoryFromRewardName(prizeName)
                        if (prizeName.isNotEmpty() && imageResId != 0 && !uniqueRewards.contains(prizeName)) {
                            rewards.add(Reward(prizeName, category, imageResId))
                            uniqueRewards.add(prizeName) // Add name to HashSet
                        }
                    }
                    adapter.updateRewards(rewards)
                    setupSpinner()
                }
                .addOnFailureListener { e ->
                    // Handle failure
                }
        } ?: run {
            // Handle user not signed in
        }
    }

    private fun getImageResIdFromName(rewardName: String): Int {
        return when (rewardName) {
            "AM Album Vinyl" -> R.drawable.am_album_vinyl
            "Blade Runner 2047 Movie Poster" -> R.drawable.blade_runner_poster
            "Fyukai Desu" -> R.drawable.fyukai_desu
            "U-U-U-I-A" -> R.drawable.u_u_u_i_a
            else -> 0
        }
    }

    private fun getCategoryFromRewardName(rewardName: String): String {
        return when (rewardName) {
            "AM Album Vinyl" -> "Music"
            "Blade Runner 2047 Movie Poster" -> "Movies"
            "Fyukai Desu" -> "Anime"
            "U-U-U-I-A" -> "Pets"
            else -> "Other"
        }
    }

    private fun filterRewards(category: String) {
        val filteredRewards = if (category == "All") {
            rewards
        } else {
            rewards.filter { it.category == category }
        }
        adapter.updateRewards(filteredRewards)
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

    class RewardAdapter(private var rewards: List<Reward>) : RecyclerView.Adapter<RewardAdapter.RewardViewHolder>() {

        class RewardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val rewardIcon: ImageView = itemView.findViewById(R.id.rewardIcon)
            val rewardName: TextView = itemView.findViewById(R.id.rewardName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.reward_item, parent, false)
            return RewardViewHolder(view)
        }

        override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
            val reward = rewards[position]
            holder.rewardIcon.setImageResource(reward.imageResId)
            holder.rewardName.text = reward.name
        }

        override fun getItemCount(): Int = rewards.size

        fun updateRewards(newRewards: List<Reward>) {
            this.rewards = newRewards
            notifyDataSetChanged()
        }
    }
}