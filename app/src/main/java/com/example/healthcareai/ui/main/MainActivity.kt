package com.example.healthcareai.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.healthcareai.R
import com.example.healthcareai.databinding.ActivityMainBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.util.NotificationScheduler

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var activeTabId = R.id.tabHome

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Schedule daily reminder alarms
        NotificationScheduler.scheduleDailyNotifications(this)

        setupViewPager()
        setupBottomNav()
        setupBackPress()

        // Handle target tab from Intent (e.g. from Notifications or Uninstall return)
        val targetTab = intent.getStringExtra(EXTRA_TARGET_TAB) ?: TAB_HOME
        selectTabDirectly(targetTab)

        binding.bottomNavBar.post {
            updateTabState(activeTabId, animate = false)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targetTab = intent.getStringExtra(EXTRA_TARGET_TAB) ?: TAB_HOME
        selectTabDirectly(targetTab)
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = MainPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 4
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val targetTabId = when (position) {
                    0 -> R.id.tabHome
                    1 -> R.id.tabScan
                    2 -> R.id.tabMyHealth
                    3 -> R.id.tabFamily
                    4 -> R.id.tabSetting
                    else -> R.id.tabHome
                }
                if (activeTabId != targetTabId) {
                    updateTabState(targetTabId, animate = true)
                }
                if (position == 3) {
                    (supportFragmentManager.findFragmentByTag("f3") as? FamilyFragment)?.refreshData()
                }
            }
        })
    }

    private fun setupBottomNav() {
        binding.tabHome.setOnClickListener { navigateToTab(0) }
        binding.tabScan.setOnClickListener { navigateToTab(1) }
        binding.tabMyHealth.setOnClickListener { navigateToTab(2) }
        binding.tabFamily.setOnClickListener { navigateToTab(3) }
        binding.tabSetting.setOnClickListener { navigateToTab(4) }
    }

    fun navigateToTab(position: Int) {
        if (binding.viewPager.currentItem == position) return
        binding.viewPager.setCurrentItem(position, false)
    }

    private fun selectTabDirectly(tabKey: String) {
        val position = when (tabKey) {
            TAB_HOME -> 0
            TAB_SCAN -> 1
            TAB_MY_HEALTH -> 2
            TAB_FAMILY -> 3
            TAB_SETTINGS -> 4
            else -> 0
        }
        val targetTabId = when (position) {
            0 -> R.id.tabHome
            1 -> R.id.tabScan
            2 -> R.id.tabMyHealth
            3 -> R.id.tabFamily
            4 -> R.id.tabSetting
            else -> R.id.tabHome
        }
        activeTabId = targetTabId
        binding.viewPager.setCurrentItem(position, false)
        updateTabState(targetTabId, animate = false)
    }

    private fun updateTabState(selectedTabId: Int, animate: Boolean) {
        activeTabId = selectedTabId

        if (animate) {
            val transition = TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                addTransition(ChangeBounds().apply {
                    duration = 240
                    interpolator = AccelerateDecelerateInterpolator()
                })
                addTransition(Fade().apply {
                    duration = 160
                    interpolator = AccelerateDecelerateInterpolator()
                })
            }
            TransitionManager.beginDelayedTransition(binding.bottomNavBar, transition)
        }

        val tabs = listOf(
            TabViews(binding.tabHome, binding.pillHome, binding.ivHome, binding.tvHome),
            TabViews(binding.tabScan, binding.pillScan, binding.ivScan, binding.tvScan),
            TabViews(binding.tabMyHealth, binding.pillMyHealth, binding.ivMyHealth, binding.tvMyHealth),
            TabViews(binding.tabFamily, binding.pillFamily, binding.ivFamily, binding.tvFamily),
            TabViews(binding.tabSetting, binding.pillSetting, binding.ivSetting, binding.tvSetting)
        )

        val selectedIconColor = ContextCompat.getColor(this, R.color.white)
        val unselectedIconColor = ContextCompat.getColor(this, R.color.nav_unselected)

        val density = resources.displayMetrics.density
        val pillHeight = (48 * density).toInt()
        val activePadH = (12 * density).toInt()
        val activePadV = (4 * density).toInt()
        val inactivePadH = (10 * density).toInt()
        val inactivePadV = (4 * density).toInt()

        val screenWidthPx = resources.displayMetrics.widthPixels
        val navBarWidthPx = binding.bottomNavBar.width.takeIf { it > 0 }
            ?: (screenWidthPx - (52 * density).toInt())

        // Minimum reserved width for each unselected tab so icons are NEVER squeezed
        val minInactiveWidthPx = (44 * density).toInt()
        val totalInactiveReservedPx = 4 * minInactiveWidthPx

        // Max width allowed for the active selected tab pill
        val maxSelectedTabWidthPx = (navBarWidthPx - totalInactiveReservedPx).coerceAtLeast((100 * density).toInt())

        val pillOverheadPx = (activePadH * 2) + (28 * density).toInt()
        val maxTextWidthPx = (maxSelectedTabWidthPx - pillOverheadPx).coerceAtLeast((50 * density).toInt())

        for (tab in tabs) {
            val isSelected = tab.tabLayout.id == selectedTabId

            val pillParams = tab.pillLayout.layoutParams
            if (pillParams.height != pillHeight) {
                pillParams.height = pillHeight
                tab.pillLayout.layoutParams = pillParams
            }

            if (isSelected) {
                tab.pillLayout.setBackgroundResource(R.drawable.bg_bottom_nav_pill)
                tab.pillLayout.setPadding(activePadH, activePadV, activePadH, activePadV)
                tab.icon.setColorFilter(selectedIconColor)
                tab.label.visibility = View.VISIBLE
                tab.label.maxWidth = maxTextWidthPx
                tab.label.isSelected = true
                tab.label.post { tab.label.isSelected = true }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                tab.tabLayout.minimumWidth = 0
                tab.tabLayout.layoutParams = params
            } else {
                tab.pillLayout.background = null
                tab.pillLayout.setPadding(inactivePadH, inactivePadV, inactivePadH, inactivePadV)
                tab.icon.setColorFilter(unselectedIconColor)
                tab.label.visibility = View.GONE
                tab.label.isSelected = false

                val params = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.0f
                )
                tab.tabLayout.minimumWidth = minInactiveWidthPx
                tab.tabLayout.layoutParams = params
            }
        }
    }

    private inner class MainPagerAdapter(activity: BaseActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment.newInstance()
                1 -> ScanFragment.newInstance()
                2 -> MyHealthFragment.newInstance()
                3 -> FamilyFragment.newInstance()
                4 -> SettingsFragment.newInstance()
                else -> HomeFragment.newInstance()
            }
        }
    }

    private data class TabViews(
        val tabLayout: LinearLayout,
        val pillLayout: LinearLayout,
        val icon: ImageView,
        val label: TextView
    )

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewPager.currentItem != 0) {
                    navigateToTab(0)
                } else {
                    finish()
                }
            }
        })
    }

    companion object {
        const val EXTRA_TARGET_TAB = "extra_target_tab"
        const val EXTRA_SKIP_NOTI_POPUP = "extra_skip_noti_popup"
        const val EXTRA_SKIP_DAILY_REWARD = "extra_skip_daily_reward"

        const val TAB_HOME = "home"
        const val TAB_SCAN = "scan"
        const val TAB_MY_HEALTH = "my_health"
        const val TAB_FAMILY = "family"
        const val TAB_SETTINGS = "settings"
    }
}
