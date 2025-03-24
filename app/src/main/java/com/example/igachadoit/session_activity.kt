package com.example.igachadoit

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.timer

class SessionActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var timerProgressBar: ProgressBar // Circular progress indicator
    private lateinit var startSessionButton: Button
    private lateinit var cancelSessionButton: Button
    private lateinit var prizeImageView: ImageView
    private var timer: Timer? = null
    private var secondsElapsed = 0
    private var sessionDurationSeconds = 0 // Total session duration for progress calculation
    private var selectedDifficulty: String? = null
    private var remainingPulls = 0
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var pullAnimationImageView: ImageView
    private lateinit var pullButton: Button
    private var pullsToGive = 0
    private lateinit var sharedPreferences: SharedPreferences
    private val MECHANICS_PREF_KEY = "show_mechanics"
    private lateinit var firestore: FirebaseFirestore
    private var googleAccount: GoogleSignInAccount? = null
    private var sessionStartTime: Timestamp? = null
    private var sessionStartedToday = false
    private var appOpenedToday = false

    // Variables to be persisted locally for guest users
    private var dailyStreak = 0
    private var weeklyStreak = 0
    private var totalPulls = 0
    private var sessionsCompletedToday = 0
    private var studyTimeMinutes = 0
    private var streakMaintained = false
    private var hardSessionCompleted = false
    private var easySessionsCompleted = 0
    private var lastSessionDifficulty: String? = null
    private var dailyStreakIncrementedToday = false
    private var lastOpenedDate: String = ""

    // Keys for SharedPreferences
    private val SESSION_HISTORY_KEY = "session_history"
    private val REWARD_GALLERY_KEY = "reward_gallery"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.session_activity)

        firestore = FirebaseFirestore.getInstance()
        googleAccount = GoogleSignIn.getLastSignedInAccount(this)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        loadLocalData()
        loadPulls()

        val showMechanics = sharedPreferences.getBoolean(MECHANICS_PREF_KEY, true)
        if (showMechanics) {
            showGameMechanicsDialog()
        }

        checkDailyStreak()
        appOpenedToday = true

        if (FirebaseAuth.getInstance().currentUser != null) {
            syncDataFromFirestore()
        }

        val rewardRecyclerView: RecyclerView = findViewById(R.id.rewardsRecyclerView)
        rewardRecyclerView.layoutManager = LinearLayoutManager(this)
        rewardRecyclerView.adapter = RewardAdapter(prizes)

        toolbar = findViewById(R.id.toolbar)

        timerTextView = findViewById(R.id.timerTextView)
        timerProgressBar = findViewById(R.id.timerProgressBar) // Initialize ProgressBar
        startSessionButton = findViewById(R.id.startSessionButton)
        cancelSessionButton = findViewById(R.id.cancelSessionButton)
        pullAnimationImageView = findViewById(R.id.pullAnimationImageView)
        prizeImageView = findViewById(R.id.prizeImageView)
        pullButton = findViewById(R.id.pullButton)
        pullButton.visibility = View.GONE

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
        }

        startSessionButton.setOnClickListener {
            showDifficultyDialog()
        }

        cancelSessionButton.setOnClickListener {
            cancelSession()
        }
        pullButton.setOnClickListener {
            rewardUser()
        }
    }

    // Load pulls from SharedPreferences
    private fun loadPulls() {
        pullsToGive = sharedPreferences.getInt("pulls", 0)
    }

    private fun loadLocalData() {
        dailyStreak = sharedPreferences.getInt("dailyStreak", 0)
        weeklyStreak = sharedPreferences.getInt("weeklyStreak", 0)
        totalPulls = sharedPreferences.getInt("pulls", 0)
        sessionsCompletedToday = sharedPreferences.getInt("sessionsCompletedToday", 0)
        studyTimeMinutes = sharedPreferences.getInt("studyTimeMinutes", 0)
        streakMaintained = sharedPreferences.getBoolean("streakMaintained", false)
        hardSessionCompleted = sharedPreferences.getBoolean("hardSessionCompleted", false)
        easySessionsCompleted = sharedPreferences.getInt("easySessionsCompleted", 0)
        lastSessionDifficulty = sharedPreferences.getString("lastSessionDifficulty", null)
        dailyStreakIncrementedToday = sharedPreferences.getBoolean("dailyStreakIncrementedToday", false)
        lastOpenedDate = sharedPreferences.getString("lastOpenedDate", "") as String
    }

    private fun saveLocalData() {
        val editor = sharedPreferences.edit()
        editor.putInt("dailyStreak", dailyStreak)
        editor.putInt("weeklyStreak", weeklyStreak)
        editor.putInt("pulls", totalPulls)
        editor.putInt("sessionsCompletedToday", sessionsCompletedToday)
        editor.putInt("studyTimeMinutes", studyTimeMinutes)
        editor.putBoolean("streakMaintained", streakMaintained)
        editor.putBoolean("hardSessionCompleted", hardSessionCompleted)
        editor.putInt("easySessionsCompleted", easySessionsCompleted)
        editor.putString("lastSessionDifficulty", selectedDifficulty)
        editor.putBoolean("dailyStreakIncrementedToday", dailyStreakIncrementedToday)
        editor.putString("lastOpenedDate", lastOpenedDate)
        editor.apply()
    }

    private fun checkDailyStreak() {
        val currentDate = getCurrentDate()

        if (lastOpenedDate != currentDate) {
            sessionStartedToday = false
            dailyStreakIncrementedToday = false
        }

        lastOpenedDate = currentDate
        saveLocalData()
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    override fun onStop() {
        super.onStop()
        if (sessionStartedToday && appOpenedToday) {
            if (!dailyStreakIncrementedToday) {
                dailyStreak++
                dailyStreakIncrementedToday = true
                saveLocalData()
                updateFirestoreStreaksAndPulls(dailyStreak, weeklyStreak, totalPulls)
            }
        }
        if (FirebaseAuth.getInstance().currentUser != null) {
            saveDataToFirestore()
        } else {
            saveLocalData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
        if (FirebaseAuth.getInstance().currentUser != null) {
            saveDataToFirestore()
        } else {
            saveLocalData()
        }
    }

    override fun onPause() {
        super.onPause()
        if (FirebaseAuth.getInstance().currentUser != null) {
            saveDataToFirestore()
        } else {
            saveLocalData()
        }
    }

    private fun saveDataToFirestore() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            googleAccount?.let { account ->
                val userId = account.id ?: return
                val userData = hashMapOf(
                    "dailyStreak" to dailyStreak,
                    "weeklyStreak" to weeklyStreak,
                    "totalPulls" to totalPulls,
                    "lastUpdated" to Timestamp.now()
                )

                firestore.collection("user_data").document(userId).set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Progress saved to Firestore", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to save progress: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    private fun syncDataFromFirestore() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            googleAccount?.let { account ->
                val userId = account.id ?: return

                firestore.collection("user_data").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            dailyStreak = document.getLong("dailyStreak")?.toInt() ?: 0
                            weeklyStreak = document.getLong("weeklyStreak")?.toInt() ?: 0
                            totalPulls = document.getLong("totalPulls")?.toInt() ?: 0
                            saveLocalData()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to sync data: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    private fun updateFirestoreStreaksAndPulls(dailyStreak: Int, weeklyStreak: Int, totalPulls: Int) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            googleAccount?.let { account ->
                val userId = account.id ?: return
                val userData = hashMapOf(
                    "dailyStreak" to dailyStreak,
                    "weeklyStreak" to weeklyStreak,
                    "totalPulls" to totalPulls,
                    "lastUpdated" to Timestamp.now()
                )

                firestore.collection("users").document(userId).set(userData)
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to save progress: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    private fun rewardUser() {
        if (pullsToGive > 0) {
            val randomPrize = prizes.random()

            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pull_animation, null)
            val pullAnimationImageView = dialogView.findViewById<ImageView>(R.id.pullAnimationImageView)
            val prizeTextView = dialogView.findViewById<TextView>(R.id.prizeTextView)
            val okButton = dialogView.findViewById<Button>(R.id.okButton)

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            dialog.show()

            Glide.with(this)
                .asGif()
                .load(R.raw.pull_animation)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(pullAnimationImageView)

            pullAnimationImageView.postDelayed({
                pullAnimationImageView.animate().alpha(0f).setDuration(200).withEndAction {
                    pullAnimationImageView.setImageResource(prizeDrawables[randomPrize.first] ?: R.drawable.ic_launcher_background)
                    pullAnimationImageView.animate().alpha(1f).setDuration(200).start()
                }.start()

                prizeTextView.text = "You acquired: ${randomPrize.first}!"
                prizeTextView.visibility = View.VISIBLE
                prizeTextView.animate().alpha(1f).setDuration(400).start()

                okButton.visibility = View.VISIBLE

                recordReward(randomPrize.first)
                pullsToGive--
                totalPulls++
                saveLocalData()
                updateFirestoreStreaksAndPulls(dailyStreak, weeklyStreak, totalPulls)

                okButton.setOnClickListener {
                    dialog.dismiss()
                    if (pullsToGive == 0) {
                        pullButton.isEnabled = false
                        showPostRewardsDialog(randomPrize)
                        Toast.makeText(this, "You've used all your pulls!", Toast.LENGTH_SHORT).show()
                    }
                }
            }, 1200)
        } else {
            Toast.makeText(
                this,
                "No pulls left. Complete a session to earn more!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showPostRewardsDialog(lastPrize: Pair<String, Int>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_post_rewards, null)
        val viewGalleryButton = dialogView.findViewById<Button>(R.id.viewGalleryButton)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Start New Session") { _, _ ->
                startSessionButton.visibility = View.VISIBLE
                cancelSessionButton.visibility = View.GONE
                pullButton.visibility = View.GONE
                prizeImageView.visibility = View.GONE
                pullsToGive = 0
                Toast.makeText(this, "New session started!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Take a Break") { _, _ ->
                showBreakDialog()
            }
            .create()
            .show()

        viewGalleryButton.setOnClickListener {
            passRewardedItemToGallery(lastPrize)
        }
    }

    private fun showGameMechanicsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_mechanics, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val stopShowingCheckBox =
            dialogView.findViewById<android.widget.RadioButton>(R.id.stopShowingCheckBox)
        val showEveryStartupRadioButton =
            dialogView.findViewById<android.widget.RadioButton>(R.id.showEveryStartupRadioButton)

        dialogView.findViewById<Button>(R.id.okButton)?.setOnClickListener {
            val editor = sharedPreferences.edit()
            if (stopShowingCheckBox.isChecked) {
                editor.putBoolean(MECHANICS_PREF_KEY, false)
            } else if (showEveryStartupRadioButton.isChecked) {
                editor.putBoolean(MECHANICS_PREF_KEY, true)
            }
            editor.apply()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_progress -> startActivity(Intent(this, ProgressActivity::class.java))
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

    private fun showDifficultyDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_difficulty, null)
        val difficultyRadioGroup: RadioGroup = dialogView.findViewById(R.id.difficultyRadioGroup)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        dialogView.findViewById<Button>(R.id.cancelButton)?.setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.setDifficultyButton)?.setOnClickListener {
            selectedDifficulty = when (difficultyRadioGroup.checkedRadioButtonId) {
                R.id.easyRadioButton -> "Easy"
                R.id.moderateRadioButton -> "Moderate"
                R.id.hardRadioButton -> "Hard"
                R.id.superbRadioButton -> "Superb"
                else -> null
            }

            if (selectedDifficulty != null) {
                remainingPulls = getRewardPulls(selectedDifficulty!!)
                Toast.makeText(this, "Selected: $selectedDifficulty", Toast.LENGTH_SHORT).show()
                startSession()
                cancelSessionButton.visibility = View.VISIBLE
                startSessionButton.visibility = View.GONE
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please select a difficulty.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun getRewardPulls(difficulty: String): Int {
        return when (difficulty) {
            "Easy" -> 1
            "Moderate" -> 2
            "Hard" -> 4
            "Superb" -> 8
            else -> 0
        }
    }

    //Session Duration settings

    private fun startSession() {
        secondsElapsed = 0
        sessionStartTime = Timestamp.now()
        sessionDurationSeconds = when (selectedDifficulty) {
            "Easy" -> 5
            "Moderate" -> 5
            "Hard" -> 5
            "Superb" -> {
                val baseHours = 2
                val additionalHours = 0
                (baseHours + additionalHours) * 60 * 60
            }
            else -> 0
        }

        pullsToGive = getRewardPulls(selectedDifficulty!!)

        timerProgressBar.max = sessionDurationSeconds // Set max to total seconds
        timerProgressBar.progress = sessionDurationSeconds // Start full

        timer = timer(period = 1000) {
            runOnUiThread {
                secondsElapsed++
                updateTimerText()
                updateTimerProgress()

                if (secondsElapsed >= sessionDurationSeconds) {
                    timer?.cancel()
                    finishSession()
                }
            }
        }
        sessionStartedToday = true
    }

    private fun cancelSession() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Session")
            .setMessage("Are you sure you want to cancel? You will not receive your reward.")
            .setPositiveButton("Yes") { _, _ ->
                timer?.cancel()
                timer = null
                secondsElapsed = 0
                updateTimerText()
                timerProgressBar.progress = sessionDurationSeconds // Reset progress to full
                cancelSessionButton.visibility = View.GONE
                startSessionButton.visibility = View.VISIBLE
                pullButton.visibility = View.GONE
                prizeImageView.visibility = View.GONE
                pullsToGive = 0
                Toast.makeText(this, "Session Canceled", Toast.LENGTH_SHORT).show()
                recordSessionHistory(false)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun finishSession() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_finish_session, null)
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                pullButton.visibility = View.VISIBLE
                pullButton.isEnabled = true
                cancelSessionButton.visibility = View.GONE
                recordSessionHistory(true)
                updateChallengeData()
            }
            .create()
            .show()
    }

    private fun updateChallengeData() {
        sessionsCompletedToday++
        saveLocalData()

        if (selectedDifficulty == "Hard") {
            hardSessionCompleted = true
            saveLocalData()
        }

        if (selectedDifficulty == "Easy") {
            easySessionsCompleted++
            saveLocalData()
        }
    }

    private fun recordReward(prizeName: String) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            googleAccount?.let { account ->
                val userId = account.id ?: return
                val rewardData = hashMapOf(
                    "prizeName" to prizeName,
                    "timestamp" to Timestamp.now()
                )

                firestore.collection("users").document(userId)
                    .collection("rewards").add(rewardData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reward recorded!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to record reward: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        } else {
            saveRewardOffline(prizeName)
            Toast.makeText(this, "Reward recorded locally (Guest User).", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRewardOffline(prizeName: String) {
        val currentRewardsJson = sharedPreferences.getString(REWARD_GALLERY_KEY, "[]")
        val type = object : TypeToken<MutableList<String>>() {}.type
        val rewardList: MutableList<String> = Gson().fromJson(currentRewardsJson, type) ?: mutableListOf()
        rewardList.add(prizeName)
        val editor = sharedPreferences.edit()
        editor.putString(REWARD_GALLERY_KEY, Gson().toJson(rewardList))
        editor.apply()
    }

    private fun passRewardedItemToGallery(rewardedItem: Pair<String, Int>) {
        val intent = Intent(this, RewardGalleryActivity::class.java)
        intent.putExtra("rewardName", rewardedItem.first)
        intent.putExtra("rewardImageResId", rewardedItem.second)
        startActivity(intent)
    }

    class RewardAdapter(private val rewards: List<Pair<String, Int>>) :
        RecyclerView.Adapter<RewardAdapter.RewardViewHolder>() {

        class RewardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val rewardIcon: ImageView = view.findViewById(R.id.rewardIcon)
            val rewardName: TextView = view.findViewById(R.id.rewardName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.reward_item, parent, false)
            return RewardViewHolder(view)
        }

        override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
            val (name, drawableRes) = rewards[position]
            holder.rewardName.text = name
            holder.rewardIcon.setImageResource(drawableRes)
        }

        override fun getItemCount(): Int = rewards.size
    }

    private fun updateTimerText() {
        val minutes = secondsElapsed / 60
        val seconds = secondsElapsed % 60
        timerTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateTimerProgress() {
        val remainingSeconds = sessionDurationSeconds - secondsElapsed
        timerProgressBar.progress = remainingSeconds // Decrease progress as time elapses
    }

    private fun showBreakDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_break, null)
        val gifBreakView = dialogView.findViewById<ImageView>(R.id.gifBreakView)

        Glide.with(this)
            .asGif()
            .load(R.raw.breaktime)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(gifBreakView)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Finish") { _, _ ->
                val intent = Intent(this, SessionActivity::class.java)
                startActivity(intent)
            }
            .create()

        dialog.show()
    }

    private fun recordSessionHistory(sessionCompleted: Boolean) {
        val sessionRecord = SessionHistoryActivity.Session(
            sessionStartTime,
            secondsElapsed.toLong(),
            sessionCompleted
        )
        if (FirebaseAuth.getInstance().currentUser != null) {
            googleAccount?.let { account ->
                val userId = account.id ?: return
                val sessionData = hashMapOf(
                    "startTime" to sessionStartTime,
                    "endTime" to Timestamp.now(),
                    "durationSeconds" to secondsElapsed,
                    "difficulty" to selectedDifficulty,
                    "completed" to sessionCompleted
                )

                firestore.collection("users").document(userId)
                    .collection("sessionHistory").add(sessionData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Session history recorded!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to record session history: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        } else {
            saveSessionHistoryOffline(sessionRecord)
            Toast.makeText(this, "Session history recorded locally (Guest User).", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSessionHistoryOffline(session: SessionHistoryActivity.Session) {
        val currentHistoryJson = sharedPreferences.getString(SESSION_HISTORY_KEY, "[]")
        val type = object : TypeToken<MutableList<SessionHistoryActivity.Session>>() {}.type
        val sessionHistoryList: MutableList<SessionHistoryActivity.Session> = Gson().fromJson(currentHistoryJson, type) ?: mutableListOf()
        sessionHistoryList.add(session)
        val editor = sharedPreferences.edit()
        editor.putString(SESSION_HISTORY_KEY, Gson().toJson(sessionHistoryList))
        editor.apply()
    }
}