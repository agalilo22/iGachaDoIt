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
import com.google.android.material.navigation.NavigationView

data class Reward(val name: String, val category: String, val imageResId: Int)

class RewardGalleryActivity : AppCompatActivity() {

    private lateinit var rewardRecyclerView: RecyclerView
    private lateinit var rewardCategorySpinner: Spinner
    private lateinit var rewards: List<Reward>
    private lateinit var adapter: RewardAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var toggle: ActionBarDrawerToggle

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

        rewards = listOf(
            Reward("Vinyl", "Music", R.drawable.am_album_vinyl),
            Reward("Poster", "Movies", R.drawable.blade_runner_poster),
            Reward("Meme", "Anime", R.drawable.fyukai_desu),
            Reward("U-U-I-A", "Pets", R.drawable.u_u_u_i_a),
        )

        val categories = rewards.distinctBy { it.category }.map { it.category }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        rewardCategorySpinner.adapter = spinnerAdapter

        rewardRecyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = RewardAdapter(rewards)
        rewardRecyclerView.adapter = adapter

        rewardCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                filterRewards(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if (categories.isNotEmpty()) {
                    filterRewards(categories[0])
                }
            }
        }
    }


    private fun filterRewards(category: String) {
        val filteredRewards = rewards.filter { it.category == category }
        adapter.updateRewards(filteredRewards)
    }

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
