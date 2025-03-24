package com.example.igachadoit

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Challenge(
    val description: String,
    val reward: String,
    var completed: Boolean = false,
    var claimed: Boolean = false,
    val imageResId: Int
)

class DailyChallengesActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var challenges: MutableList<Challenge>
    private lateinit var adapter: DailyChallengesAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firestore: FirebaseFirestore
    private var googleAccount: GoogleSignInAccount? = null
    private var pullsToGive = 0

    private val prizes = listOf(
        Pair("AM Album Vinyl", R.drawable.am_album_vinyl),
        Pair("Fyukai Desu", R.drawable.fyukai_desu),
        Pair("Blade Runner 2047 Movie Poster", R.drawable.blade_runner_poster),
        Pair("U-U-U-I-A", R.drawable.u_u_u_i_a)
    )

    private val prizeDrawables = mapOf(
        "AM Album Vinyl" to R.drawable.am_album_vinyl,
        "Fyukai Desu" to R.drawable.fyukai_desu,
        "Blade Runner 2047 Movie Poster" to R.drawable.blade_runner_poster,
        "U-U-U-I-A" to R.drawable.u_u_u_i_a
    )

    private val REWARD_GALLERY_KEY = "reward_gallery"
    private val TAG = "DailyChallengesActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.daily_challenges_activity)

        toolbar = findViewById(R.id.toolbar)

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

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        firestore = FirebaseFirestore.getInstance()
        googleAccount = GoogleSignIn.getLastSignedInAccount(this)

        challenges = getOrGenerateDailyChallenges().toMutableList()
        Log.d(TAG, "Initial Challenges: ${Gson().toJson(challenges)}")

        adapter = DailyChallengesAdapter(challenges) { position ->
            claimReward(position)
        }
        challengesRecyclerView.adapter = adapter

        checkChallengeCompletion()
        loadPulls()
    }

    override fun onResume() {
        super.onResume()
        checkChallengeCompletion() // Refresh challenge status when activity resumes
    }

    private fun getOrGenerateDailyChallenges(): List<Challenge> {
        val currentDate = getCurrentDate()
        val storedDate = sharedPreferences.getString("challengeDate", "")
        val challengeJson = sharedPreferences.getString("challenges", null)

        if (storedDate == currentDate && challengeJson != null) {
            val type = object : TypeToken<List<Challenge>>() {}.type
            val loadedChallenges = Gson().fromJson<List<Challenge>>(challengeJson, type)
            Log.d(TAG, "Loaded Challenges: ${Gson().toJson(loadedChallenges)}")

            // Check if any loaded challenge has an invalid imageResId (0)
            if (loadedChallenges.any { it.imageResId == 0 }) {
                Log.w(TAG, "Loaded challenges have invalid imageResId. Regenerating...")
                return generateRandomChallenges()
            }
            return loadedChallenges
        } else {
            val newChallenges = generateRandomChallenges()
            val editor = sharedPreferences.edit()
            editor.putString("challengeDate", currentDate)
            editor.putString("challenges", Gson().toJson(newChallenges))
            editor.apply()
            Log.d(TAG, "Generated New Challenges: ${Gson().toJson(newChallenges)}")
            return newChallenges
        }
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun generateRandomChallenges(): List<Challenge> {
        val allChallenges = listOf(
            Challenge("Complete 3 sessions today.", "5 Pulls", imageResId = R.drawable.ic_session),
//            Challenge("Study for 2 hours.", "3 Pulls", imageResId = R.drawable.ic_clock), // commented out for testing purposes
//            Challenge("Maintain your streak.", "1 Pull", imageResId = R.drawable.ic_streak), // commented out for testing purposes
            Challenge("Complete a hard session.", "2 Pulls", imageResId = R.drawable.ic_hard),
            Challenge("Complete 5 easy sessions.", "6 Pulls", imageResId = R.drawable.ic_easy),
//            Challenge("Study for 30 minutes.", "1 Pull", imageResId = R.drawable.ic_study) // commented out for testing purposes
        )
        val shuffledChallenges = allChallenges.shuffled().take(3)
        Log.d(TAG, "Randomly Generated Challenges: ${Gson().toJson(shuffledChallenges)}")
        return shuffledChallenges
    }

    // Updated challenge completion logic
    private fun checkChallengeCompletion() {
        val sessionsCompleted = sharedPreferences.getInt("sessionsCompletedToday", 0)
        val studyTimeMinutes = sharedPreferences.getInt("studyTimeMinutes", 0)
        val streakMaintained = sharedPreferences.getBoolean("streakMaintained", false)
        val hardSessionCompleted = sharedPreferences.getBoolean("hardSessionCompleted", false)
        val easySessionsCompleted = sharedPreferences.getInt("easySessionsCompleted", 0)

        // Log SharedPreferences values for debugging
        Log.d(TAG, "SharedPrefs - sessionsCompleted: $sessionsCompleted, studyTimeMinutes: $studyTimeMinutes, " +
                "streakMaintained: $streakMaintained, hardSessionCompleted: $hardSessionCompleted, " +
                "easySessionsCompleted: $easySessionsCompleted")

        for (challenge in challenges) {
            when (challenge.description) {
                "Complete 3 sessions today." -> challenge.completed = sessionsCompleted >= 3
//                "Study for 2 hours." -> challenge.completed = studyTimeMinutes >= 120 // commented out for testing purposes
//                "Maintain your streak." -> challenge.completed = streakMaintained // commented out for testing purposes
                "Complete a hard session." -> challenge.completed = hardSessionCompleted
                "Complete 5 easy sessions." -> challenge.completed = easySessionsCompleted >= 5
//                "Study for 30 minutes." -> challenge.completed = studyTimeMinutes >= 30 // commented out for testing purposes
            }
            Log.d(TAG, "Challenge: ${challenge.description}, Completed: ${challenge.completed}")
        }
        adapter.notifyDataSetChanged()
    }

    private fun claimReward(position: Int) {
        val challenge = challenges[position]
        if (challenge.completed && !challenge.claimed) {
            val rewardAmount = challenge.reward.split(" ")[0].toInt()
            addPulls(rewardAmount)
            Toast.makeText(this, "Reward claimed: ${challenge.reward}", Toast.LENGTH_SHORT).show()
            challenge.claimed = true
            updateStoredChallenges()
            adapter.notifyItemChanged(position)
            showPullDialog(rewardAmount)
        } else if (challenge.claimed) {
            Toast.makeText(this, "Reward already claimed.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Challenge not completed yet.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPulls() {
        pullsToGive = sharedPreferences.getInt("pulls", 0)
    }

    private fun addPulls(amount: Int) {
        val currentPulls = sharedPreferences.getInt("pulls", 0)
        val editor = sharedPreferences.edit()
        editor.putInt("pulls", currentPulls + amount)
        editor.apply()
        pullsToGive += amount
    }

    private fun showPullDialog(rewardAmount: Int) {
        pullsToGive = rewardAmount
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pull_reward, null)
        val pullAnimationImageView: ImageView = dialogView.findViewById(R.id.pullAnimationImageView)
        val prizeImageView: ImageView = dialogView.findViewById(R.id.prizeImageView)
        val pullButton: Button = dialogView.findViewById(R.id.pullButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        pullButton.setOnClickListener {
            rewardUser(pullAnimationImageView, prizeImageView, dialog)
        }

        dialog.show()
    }

    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_home -> startActivity(Intent(this, SessionActivity::class.java))
            R.id.nav_progress -> startActivity(Intent(this, ProgressActivity::class.java))
            R.id.nav_session_history -> startActivity(Intent(this, SessionHistoryActivity::class.java))
            R.id.nav_daily_challenges -> {} // Already here
            R.id.nav_reward_gallery -> startActivity(Intent(this, RewardGalleryActivity::class.java))
            R.id.nav_logout -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        drawerLayout.closeDrawer(navigationView)
        return true
    }

    private fun rewardUser(pullAnimationImageView: ImageView, prizeImageView: ImageView, dialog: AlertDialog) {
        if (pullsToGive > 0) {
            val randomPrize = prizes.random()
            prizeImageView.visibility = View.GONE
            pullAnimationImageView.visibility = View.VISIBLE
            Glide.with(this)
                .asGif()
                .load(R.raw.pull_animation)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(pullAnimationImageView)

            pullAnimationImageView.postDelayed({
                pullAnimationImageView.visibility = View.GONE
                prizeImageView.visibility = View.VISIBLE
                val prizeDrawable = prizeDrawables[randomPrize.first]
                if (prizeDrawable != null) {
                    prizeImageView.setImageResource(prizeDrawable)
                    Toast.makeText(this, "You got: ${randomPrize.first}!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error loading prize image", Toast.LENGTH_SHORT).show()
                }
                recordReward(randomPrize.first)
                pullsToGive--
                sharedPreferences.edit().putInt("pulls", sharedPreferences.getInt("pulls", 0) - 1).apply()
                if (pullsToGive == 0) {
                    Toast.makeText(this, "You've used all your pulls!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    showPostRewardsDialog(randomPrize)
                }
            }, 1200)
        } else {
            Toast.makeText(this, "No pulls left!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    private fun showPostRewardsDialog(lastPrize: Pair<String, Int>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_post_rewards, null)
        val viewGalleryButton = dialogView.findViewById<Button>(R.id.viewGalleryButton)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("No thanks!") { _, _ -> }
            .create()
            .show()

        viewGalleryButton.setOnClickListener {
            passRewardedItemToGallery(lastPrize)
        }
    }

    private fun recordReward(prizeName: String) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            googleAccount?.let { account ->
                val userId = account.id ?: return
                val rewardData = hashMapOf(
                    "prizeName" to prizeName,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("users").document(userId).collection("rewards").add(rewardData)
            }
        } else {
            saveRewardOffline(prizeName)
        }
    }

    private fun saveRewardOffline(prizeName: String) {
        val currentRewardsJson = sharedPreferences.getString(REWARD_GALLERY_KEY, "")
        val type = object : TypeToken<MutableList<String>>() {}.type
        val rewardList: MutableList<String> = Gson().fromJson(currentRewardsJson, type) ?: mutableListOf()
        rewardList.add(prizeName)
        sharedPreferences.edit().putString(REWARD_GALLERY_KEY, Gson().toJson(rewardList)).apply()
    }

    private fun passRewardedItemToGallery(rewardedItem: Pair<String, Int>) {
        val intent = Intent(this, RewardGalleryActivity::class.java)
        intent.putExtra("rewardName", rewardedItem.first)
        intent.putExtra("rewardImageResId", rewardedItem.second)
        startActivity(intent)
    }

    private fun updateStoredChallenges() {
        sharedPreferences.edit().putString("challenges", Gson().toJson(challenges)).apply()
    }
}

class DailyChallengesAdapter(
    private val challenges: List<Challenge>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<DailyChallengesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val challengeImage: ImageView = itemView.findViewById(R.id.challengeImage)
        val challengeDescriptionTextView: TextView = itemView.findViewById(R.id.challengeDescriptionTextView)
        val challengeRewardTextView: TextView = itemView.findViewById(R.id.challengeRewardTextView)
        val claimButton: Button = itemView.findViewById(R.id.claimButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.challenge_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val challenge = challenges[position]

        Log.d("DailyChallengesAdapter", "Attempting to load image for ${challenge.description}. Resource ID: ${challenge.imageResId}")
        holder.challengeImage.setImageResource(challenge.imageResId)

        if (holder.challengeImage.drawable == null) {
            Log.w("DailyChallengesAdapter", "Image not rendered for ${challenge.description}, using fallback")
            holder.challengeImage.setImageResource(android.R.drawable.ic_menu_help)
            Toast.makeText(holder.itemView.context, "Image missing for ${challenge.description}", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("DailyChallengesAdapter", "Image successfully loaded for ${challenge.description}")
        }

        holder.challengeDescriptionTextView.text = challenge.description
        holder.challengeRewardTextView.text = "Reward: ${challenge.reward}"

        if (challenge.completed && !challenge.claimed) {
            holder.claimButton.visibility = View.VISIBLE
            holder.challengeDescriptionTextView.paintFlags = holder.challengeDescriptionTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        } else {
            if (challenge.claimed) {
                holder.claimButton.visibility = View.GONE
                holder.challengeDescriptionTextView.paintFlags = holder.challengeDescriptionTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.claimButton.visibility = View.GONE
                holder.challengeDescriptionTextView.paintFlags = holder.challengeDescriptionTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }

        holder.claimButton.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int = challenges.size
}