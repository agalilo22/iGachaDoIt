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
import com.google.android.material.navigation.NavigationView
import java.util.Timer
import kotlin.concurrent.timer


class SessionActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var startSessionButton: Button
    private lateinit var cancelSessionButton: Button
    private lateinit var prizeImageView: ImageView
    private var timer: Timer? = null
    private var secondsElapsed = 0
    private var selectedDifficulty: String? = null
    private var remainingPulls = 0
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var pullAnimationImageView: ImageView
    private lateinit var pullButton: Button // Add pull button
    private var pullsToGive = 0 // Store the number of pulls to give
    private lateinit var sharedPreferences: SharedPreferences
    private val MECHANICS_PREF_KEY = "show_mechanics"

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

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Show game mechanics dialog if it's the first time or if "Show Every Startup" is selected
        val showMechanics = sharedPreferences.getBoolean(MECHANICS_PREF_KEY, true) // Default to true (show on first run)

        if (showMechanics) {
            showGameMechanicsDialog()
        }


        val rewardRecyclerView: RecyclerView = findViewById(R.id.rewardsRecyclerView)
        rewardRecyclerView.layoutManager = LinearLayoutManager(this)
        rewardRecyclerView.adapter = RewardAdapter(prizes)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        timerTextView = findViewById(R.id.timerTextView)
        startSessionButton = findViewById(R.id.startSessionButton)
        cancelSessionButton = findViewById(R.id.cancelSessionButton)
        pullAnimationImageView = findViewById(R.id.pullAnimationImageView)
        prizeImageView = findViewById(R.id.prizeImageView)
        pullButton = findViewById(R.id.pullButton) // Initialize pull button
        pullButton.visibility = View.GONE // Initially hide the pull button

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
    private fun showGameMechanicsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_mechanics, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent dismissing by touching outside
            .create()

        val stopShowingCheckBox = dialogView.findViewById<android.widget.RadioButton>(R.id.stopShowingCheckBox)
        val showEveryStartupRadioButton = dialogView.findViewById<android.widget.RadioButton>(R.id.showEveryStartupRadioButton)

        dialogView.findViewById<Button>(R.id.okButton)?.setOnClickListener {
            val editor = sharedPreferences.edit()

            if (stopShowingCheckBox.isChecked) {
                editor.putBoolean(MECHANICS_PREF_KEY, false) // Stop showing mechanics
            } else if (showEveryStartupRadioButton.isChecked) {
                editor.putBoolean(MECHANICS_PREF_KEY, true) // Show every startup
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
        }
        drawerLayout.closeDrawer(navigationView)
        return true
    }

    private fun showDifficultyDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_difficulty, null)
        val difficultyRadioGroup: RadioGroup = dialogView.findViewById(R.id.difficultyRadioGroup)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

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


    private fun startSession() {
        secondsElapsed = 0
        val sessionDurationSeconds = when (selectedDifficulty) {
            "Easy" -> 5 // 30*60 mins in seconds
            "Moderate" -> 5// 60 * 60 , 1 hr
            "Hard" -> 5// 2 * 60 * 60, 2 hrs
            "Superb" -> {
                val baseHours = 2
                val additionalHours = 0 // You'll calculate this based on user input later
                (baseHours + additionalHours) * 60 * 60
            }
            else -> 0
        }

        pullsToGive = getRewardPulls(selectedDifficulty!!) // Set pulls based on difficulty

        timer = timer(period = 1000) {
            runOnUiThread {
                secondsElapsed++
                updateTimerText()

                if (secondsElapsed >= sessionDurationSeconds) {
                    timer?.cancel()
                    finishSession()
                }
            }
        }
    }

    private fun cancelSession() {
        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Cancel Session")
            .setMessage("Are you sure you want to cancel? You will not receive your reward.")
            .setPositiveButton("Yes") { _, _ ->
                timer?.cancel()
                timer = null
                secondsElapsed = 0
                updateTimerText()
                cancelSessionButton.visibility = View.GONE
                startSessionButton.visibility = View.VISIBLE
                pullButton.visibility = View.GONE // Hide pull button
                prizeImageView.visibility = View.GONE // Hide prize image
                pullsToGive = 0
                Toast.makeText(this, "Session Canceled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun finishSession() {
        // Show Finish Session Dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_finish_session, null)
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                pullButton.visibility = View.VISIBLE
                pullButton.isEnabled = true
                cancelSessionButton.visibility = View.GONE
            }
            .create()
            .show()
    }

    private fun showPostRewardsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_post_rewards, null)
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
    }



    private fun rewardUser() {
        if (pullsToGive > 0) {
            val randomPrize = prizes.random()

            // Hide the previous prize
            prizeImageView.visibility = View.GONE

            // Show pull animation
            pullAnimationImageView.visibility = View.VISIBLE
            Glide.with(this@SessionActivity)
                .asGif()
                .load(R.raw.pull_animation)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(pullAnimationImageView)

            // Delay to show reward after animation
            pullAnimationImageView.postDelayed({
                pullAnimationImageView.visibility = View.GONE
                prizeImageView.visibility = View.VISIBLE

                // Get the drawable for the prize
                val prizeDrawable = prizeDrawables[randomPrize.first]
                if (prizeDrawable != null) {
                    prizeImageView.setImageResource(prizeDrawable)
                    Toast.makeText(this, "You got: $randomPrize!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: Drawable not found for $randomPrize!", Toast.LENGTH_SHORT).show()
                }

                pullsToGive-- // Decrease remaining pulls
                if (pullsToGive == 0) {
                    pullButton.isEnabled = false // Disable button if no pulls are left
                    showPostRewardsDialog()
                    Toast.makeText(this, "You've used all your pulls!", Toast.LENGTH_SHORT).show()
                }
            }, 1200) // Duration of animation
        } else {
            Toast.makeText(this, "No pulls left. Complete a session to earn more!", Toast.LENGTH_SHORT).show()
        }
    }

    class RewardAdapter(private val rewards: List<Pair<String, Int>>) : RecyclerView.Adapter<RewardAdapter.RewardViewHolder>() {

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


    private fun showBreakDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_break, null)
        val gifBreakView = dialogView.findViewById<ImageView>(R.id.gifBreakView)

        // Load the GIF using Glide
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
                // Navigate to the Start Session window
                val intent = Intent(this, SessionActivity::class.java)
                startActivity(intent)
            }
            .create()

        dialog.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
    }
}