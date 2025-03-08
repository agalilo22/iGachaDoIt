package com.example.igachadoit

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth


class RewardGalleryActivity : AppCompatActivity() {

    private lateinit var rewardGalleryRecyclerView: RecyclerView // Corrected variable name - but will be reassigned to findViewById
    private lateinit var rewardCategorySpinner: Spinner
    private var rewardItems: MutableList<RewardItem> = mutableListOf()
    private lateinit var adapter: RewardGalleryAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var firestore: FirebaseFirestore
    private var googleAccount: GoogleSignInAccount? = null
    private lateinit var sharedPreferences: SharedPreferences

    private val prizeDrawables = mapOf(
        "AM Album Vinyl" to R.drawable.am_album_vinyl,
        "Fyukai Desu" to R.drawable.fyukai_desu,
        "Blade Runner 2047 Movie Poster" to R.drawable.blade_runner_poster,
        "U-U-U-I-A" to R.drawable.u_u_u_i_a
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reward_gallery_activity)

        rewardGalleryRecyclerView = findViewById(R.id.rewardRecyclerView) // Corrected ID here to rewardRecyclerView
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

        rewardGalleryRecyclerView = findViewById(R.id.rewardRecyclerView) // Corrected ID here to rewardRecyclerView
        rewardGalleryRecyclerView.layoutManager = GridLayoutManager(this, 3) // 3 columns

        firestore = FirebaseFirestore.getInstance()
        googleAccount = GoogleSignIn.getLastSignedInAccount(this)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)


        // Handle item passed from DailyChallengesActivity or SessionActivity
        intent.getStringExtra("rewardName")?.let { rewardName ->
            intent.getIntExtra("rewardImageResId", 0).let { rewardImageResId ->
                if (rewardImageResId != 0) {
                    // handled in passRewardedItemToGallery function in SessionActivity and DailyChallengesActivity
                }
            }
        }
        loadRewards()
    }

    private fun loadRewards() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            loadRewardsFromFirestore()
        } else {
            loadRewardsFromSharedPreferences()
        }
    }


    private fun loadRewardsFromFirestore() {
        googleAccount?.let { account ->
            val userId = account.id ?: return
            firestore.collection("users").document(userId)
                .collection("rewards")
                .get()
                .addOnSuccessListener { documents ->
                    // Use a Set to store unique reward names
                    val uniqueRewardNames = HashSet<String>()
                    for (document in documents) {
                        document.getString("prizeName")?.let { prizeName ->
                            uniqueRewardNames.add(prizeName)
                        }
                    }
                    // Convert Set to List before updating RecyclerView
                    updateRecyclerView(uniqueRewardNames.toList())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to load rewards from cloud: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {
            Toast.makeText(this, "User not signed in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRewardsFromSharedPreferences() {
        val rewardsJson = sharedPreferences.getString("reward_gallery", "[]")
        val type = object : TypeToken<List<String>>() {}.type
        val rewardNames: List<String> = Gson().fromJson(rewardsJson, type) ?: emptyList()

        // Use a Set to ensure uniqueness even when loading from SharedPreferences
        val uniqueRewardNamesSet = HashSet<String>(rewardNames)
        // Convert Set to List for RecyclerView
        updateRecyclerView(uniqueRewardNamesSet.toList())

        Toast.makeText(this, "Rewards loaded locally.", Toast.LENGTH_SHORT).show()
    }


    private fun updateRecyclerView(rewardNames: List<String>) {
        rewardItems.clear()
        rewardItems.addAll(rewardNames.mapNotNull { name ->
            prizeDrawables[name]?.let { drawableId ->
                RewardItem(name, drawableId)
            }
        })
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        } else {
            adapter = RewardGalleryAdapter(rewardItems)
            rewardGalleryRecyclerView.adapter = adapter
        }
        setupSpinner() // Re-setup spinner to reflect potentially new categories
    }


    data class RewardItem(val name: String, val imageResId: Int)

    class RewardGalleryAdapter(private val rewardItems: List<RewardItem>) :
        RecyclerView.Adapter<RewardGalleryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val rewardImageView: ImageView = view.findViewById(R.id.rewardImage) // Corrected ID to rewardImage
            val rewardNameTextView: TextView = view.findViewById(R.id.rewardName) // Corrected ID to rewardName
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.reward_item_gallery, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = rewardItems[position]
            holder.rewardImageView.setImageResource(item.imageResId)
            holder.rewardNameTextView.text = item.name
        }

        override fun getItemCount(): Int = rewardItems.size
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

    private fun setupSpinner() {
        val categories = mutableListOf("All")
        categories.addAll(rewardItems.distinctBy { getCategoryFromRewardName(it.name) }.map { getCategoryFromRewardName(it.name) }.sorted())
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        rewardCategorySpinner.adapter = spinnerAdapter

        rewardCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCategory = categories[position]
                filterRewards(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                filterRewards("All")
            }
        }
    }


    private fun filterRewards(category: String) {
        val filteredRewards = if (category == "All") {
            rewardItems
        } else {
            rewardItems.filter { getCategoryFromRewardName(it.name) == category }
        }
        adapter = RewardGalleryAdapter(filteredRewards)
        rewardGalleryRecyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }


    private fun getCategoryFromRewardName(rewardName: String): String {
        return when (rewardName) {
            "AM Album Vinyl",  -> "Music"
            "Fyukai Desu", -> "Anime"
            "U-U-U-I-A", -> "Pets"
            "Blade Runner 2047 Movie Poster" -> "Movies"
            else -> "Uncategorized" // Default category
        }
    }

    private fun getImageResIdFromName(prizeName: String): Int {
        return prizeDrawables[prizeName] ?: 0
    }
}