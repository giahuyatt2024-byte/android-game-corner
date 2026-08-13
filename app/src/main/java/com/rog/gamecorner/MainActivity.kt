package com.rog.gamecorner

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.animation.ValueAnimator
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

/**
 * GAME CORNER home screen.
 *
 * The app list is always populated from Android's launcher activities. There
 * is no mock game data: selecting a row stores its real package name and the
 * launch flow opens that package after the boot/loading sequence.
 */
class MainActivity : Activity() {
    private lateinit var permissionStatus: TextView
    private lateinit var recentApps: LinearLayout
    private lateinit var detectedApps: LinearLayout
    private lateinit var slideToOpen: SeekBar
    private lateinit var launchButton: Button
    private lateinit var gameSpaceMode: LinearLayout
    private lateinit var directOpenMode: LinearLayout
    private lateinit var appsStatValue: TextView
    private lateinit var ramStatValue: TextView
    private lateinit var displayStatValue: TextView
    private lateinit var statsReader: DeviceStatsReader

    private var selectedPackage: String? = null
    private var selectedLabel = "Free Fire"
    private var gameSpaceSelected = true
    private val preferences by lazy {
        getSharedPreferences("game_corner_state", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        statsReader = DeviceStatsReader(this)
        setContentView(createContent())
        refreshPermissionState()
        refreshInstalledApps()
    }

    override fun onResume() {
        super.onResume()
        if (::permissionStatus.isInitialized) {
            refreshPermissionState()
            refreshInstalledApps()
        }
    }

    private fun createContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(14))
            setBackgroundColor(BG)
        }

        val topBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        val homeTitle = TextView(this).apply {
            text = "Home"
            textSize = 23f
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
        }
        topBar.addView(homeTitle, LinearLayout.LayoutParams(0, dp(38), 1f))
        val cornerBadge = TextView(this).apply {
            text = "GAME CORNER"
            textSize = 9f
            letterSpacing = 0.14f
            gravity = Gravity.CENTER
            setTextColor(CYAN)
            background = strokeBackground(CYAN, dp(8))
            setPadding(dp(10), 0, dp(10), 0)
        }
        topBar.addView(cornerBadge, LinearLayout.LayoutParams(dp(118), dp(30)))
        root.addView(topBar)

        root.addView(createBrandBanner(), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(128),
        ).apply {
            topMargin = dp(10)
        })

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        stats.addView(createStatCard("APPS FOUND", "--", "real"), weightParams())
        stats.addView(createStatCard("RAM USED", "--", "now"), weightParams())
        stats.addView(createStatCard("DISPLAY", "--", "refresh"), weightParams())
        root.addView(stats, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(70),
        ).apply {
            topMargin = dp(10)
        })

        root.addView(sectionTitle("RECENTLY PLAYED"), matchWrap().apply {
            topMargin = dp(12)
        })
        recentApps = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val recentScroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            addView(recentApps)
        }
        root.addView(recentScroller, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(86),
        ))

        val libraryHeader = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        libraryHeader.addView(sectionTitle("GAME LIBRARY"), LinearLayout.LayoutParams(0, dp(30), 1f))
        libraryHeader.addView(TextView(this).apply {
            text = "REAL APPS"
            textSize = 9f
            letterSpacing = 0.12f
            setTextColor(MUTED)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(30),
        ))
        root.addView(libraryHeader)

        detectedApps = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val libraryScroller = ScrollView(this).apply {
            isFillViewport = true
            addView(detectedApps)
        }
        root.addView(libraryScroller, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))

        root.addView(sectionTitle("SELECT LAUNCH MODE"), matchWrap().apply {
            topMargin = dp(10)
        })
        val modes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        gameSpaceMode = createModeCard(
            "Game Space",
            "Optimize & launch",
        )
        directOpenMode = createModeCard(
            "Direct Open",
            "Open app directly",
        )
        gameSpaceMode.setOnClickListener {
            gameSpaceSelected = true
            refreshModeSelection()
        }
        directOpenMode.setOnClickListener {
            gameSpaceSelected = false
            refreshModeSelection()
        }
        modes.addView(gameSpaceMode, weightParams())
        modes.addView(directOpenMode, weightParams())
        root.addView(modes, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(72),
        ))
        refreshModeSelection()

        permissionStatus = TextView(this).apply {
            textSize = 10f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(10), 0)
        }
        root.addView(permissionStatus, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(28),
        ).apply {
            topMargin = dp(6)
        })

        val slideShell = FrameLayout(this).apply {
            background = gradientBackground(
                intArrayOf(Color.rgb(21, 27, 48), Color.rgb(32, 21, 55)),
                dp(12),
            )
        }
        slideToOpen = SeekBar(this).apply {
            max = 100
            progress = 0
            splitTrack = false
            progressTintList = ColorStateList.valueOf(CYAN)
            thumbTintList = ColorStateList.valueOf(CYAN)
            setPadding(dp(10), 0, dp(10), 0)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser && value >= 92) {
                        bar.progress = 100
                        launchGame()
                    }
                }

                override fun onStartTrackingTouch(bar: SeekBar) = Unit

                override fun onStopTrackingTouch(bar: SeekBar) {
                    if (bar.progress < 92) bar.progress = 0
                }
            })
        }
        slideShell.addView(slideToOpen, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(54),
        ))
        slideShell.addView(TextView(this).apply {
            text = "Slide to Open"
            textSize = 11f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(Color.argb(215, 255, 255, 255))
            isClickable = false
        }, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(54),
        ))
        root.addView(slideShell, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(54),
        ).apply {
            topMargin = dp(8)
        })

        launchButton = Button(this).apply {
            text = "LAUNCH"
            textSize = 11f
            letterSpacing = 0.1f
            isAllCaps = false
            setTextColor(TEXT)
            background = strokeBackground(Color.rgb(86, 96, 129), dp(9))
            setOnClickListener { launchGame() }
        }
        root.addView(launchButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(42),
        ).apply {
            topMargin = dp(6)
        })

        animateHome(root)
        return root
    }

    private fun createBrandBanner(): View {
        val banner = FrameLayout(this).apply {
            background = gradientBackground(
                intArrayOf(Color.rgb(31, 22, 65), Color.rgb(11, 40, 61)),
                dp(12),
            )
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        copy.addView(TextView(this).apply {
            text = "GAME\nCORNER"
            textSize = 18f
            letterSpacing = 0.05f
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(dp(140), dp(54)))
        copy.addView(TextView(this).apply {
            text = "Your real games. Your space."
            textSize = 10f
            setTextColor(Color.rgb(176, 187, 215))
        }, matchWrap())
        banner.addView(copy, FrameLayout.LayoutParams(
            dp(220),
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.START or Gravity.CENTER_VERTICAL,
        ))

        val mark = TextView(this).apply {
            text = "GC"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(Color.argb(210, 138, 229, 255))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            rotation = -12f
        }
        banner.addView(mark, FrameLayout.LayoutParams(
            dp(120),
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.END or Gravity.CENTER_VERTICAL,
        ))
        ValueAnimator.ofFloat(-7f, 7f).apply {
            duration = 1800L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { mark.rotation = it.animatedValue as Float }
            start()
        }
        return banner
    }

    private fun createStatCard(label: String, value: String, caption: String): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(10), 0)
            background = solidBackground(PANEL, dp(9))
        }
        card.addView(TextView(this).apply {
            text = label
            textSize = 8f
            letterSpacing = 0.08f
            setTextColor(MUTED)
        }, matchWrap())
        val valueRow = LinearLayout(this).apply {
            gravity = Gravity.BOTTOM
        }
        val valueText = TextView(this).apply {
            text = value
            textSize = 17f
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
        }
        when (label) {
            "APPS FOUND" -> appsStatValue = valueText
            "RAM USED" -> ramStatValue = valueText
            "DISPLAY" -> displayStatValue = valueText
        }
        valueRow.addView(valueText, LinearLayout.LayoutParams(0, dp(28), 1f))
        valueRow.addView(TextView(this).apply {
            text = caption
            textSize = 8f
            setTextColor(CYAN)
            gravity = Gravity.BOTTOM
            setPadding(0, 0, 0, dp(3))
        }, matchWrap())
        card.addView(valueRow, matchWrap())
        return card
    }

    private fun updateHomeStats(appCount: Int) {
        if (!::appsStatValue.isInitialized) return
        val stats = statsReader.read()
        appsStatValue.text = appCount.toString()
        ramStatValue.text = stats.ramText
        displayStatValue.text = stats.refreshRateText
    }

    private fun createModeCard(title: String, subtitle: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
        }
        card.addView(TextView(this).apply {
            text = title
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
        }, matchWrap())
        card.addView(TextView(this).apply {
            text = subtitle
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(MUTED)
        }, matchWrap())
        return card
    }

    private fun refreshModeSelection() {
        if (!::gameSpaceMode.isInitialized) return
        gameSpaceMode.background = if (gameSpaceSelected) {
            gradientBackground(
                intArrayOf(Color.rgb(26, 37, 72), Color.rgb(53, 29, 86)),
                dp(10),
            )
        } else {
            solidBackground(PANEL, dp(10))
        }
        directOpenMode.background = if (!gameSpaceSelected) {
            gradientBackground(
                intArrayOf(Color.rgb(26, 37, 72), Color.rgb(53, 29, 86)),
                dp(10),
            )
        } else {
            solidBackground(PANEL, dp(10))
        }
    }

    private fun refreshPermissionState() {
        val granted = Settings.canDrawOverlays(this)
        permissionStatus.text = if (granted) {
            "● OVERLAY READY   •   HUD can appear above the real app"
        } else {
            "○ OVERLAY REQUIRED   •   Tap here to allow GAME CORNER"
        }
        permissionStatus.setTextColor(if (granted) CYAN else Color.rgb(255, 185, 96))
        permissionStatus.background = solidBackground(
            if (granted) Color.rgb(15, 40, 54) else Color.rgb(52, 34, 25),
            dp(7),
        )
        permissionStatus.setOnClickListener {
            if (!granted) openOverlaySettings()
        }
        launchButton.isEnabled = granted
        launchButton.alpha = if (granted) 1f else 0.45f
        slideToOpen.isEnabled = granted
        slideToOpen.alpha = if (granted) 1f else 0.45f
        updateLaunchButton()
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun launchGame() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings()
            return
        }
        if (selectedPackage == null) {
            Toast.makeText(
                this,
                "Không tìm thấy app có thể mở trên thiết bị.",
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        if (!gameSpaceSelected) {
            openSelectedApp()
        } else {
            showLoadingDialog()
        }
    }

    private fun openSelectedApp() {
        val targetPackage = selectedPackage ?: return
        rememberRecentPackage(targetPackage)
        startActivity(
            Intent(this, BootAnimationActivity::class.java).putExtra(
                BootAnimationActivity.EXTRA_TARGET_PACKAGE,
                targetPackage,
            ),
        )
    }

    private fun showLoadingDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = gradientBackground(
                intArrayOf(Color.rgb(25, 29, 47), Color.rgb(37, 25, 59)),
                dp(14),
            )
        }
        val title = TextView(this).apply {
            text = "Loading game..."
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
        }
        card.addView(title, matchWrap())
        card.addView(TextView(this).apply {
            text = "Preparing the real app selected on your device"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(MUTED)
            setPadding(0, dp(6), 0, dp(16))
        }, matchWrap())

        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 10
            progress = 0
            progressTintList = ColorStateList.valueOf(CYAN)
            progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(67, 71, 94))
        }
        card.addView(progress, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(8),
        ))

        val timerLabel = TextView(this).apply {
            text = "10s"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(CYAN)
            setPadding(0, dp(8), 0, dp(14))
        }
        card.addView(timerLabel, matchWrap())

        val reward = TextView(this).apply {
            text = "REAL APP  •  ${selectedLabel.uppercase()}"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(215, 205, 255))
            background = solidBackground(Color.rgb(48, 39, 72), dp(9))
            setPadding(dp(10), dp(12), dp(10), dp(12))
        }
        card.addView(reward, matchWrap())

        dialog.setContentView(card)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog.window?.let { window ->
                window.attributes = window.attributes.apply {
                    dimAmount = 0.72f
                }
            }
            dialog.window?.setLayout(dp(326), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()

        val handler = Handler(Looper.getMainLooper())
        var remaining = 10
        lateinit var timer: Runnable
        timer = object : Runnable {
            override fun run() {
                remaining -= 1
                progress.progress = 10 - remaining
                timerLabel.text = "${remaining.coerceAtLeast(0)}s"
                if (remaining <= 0) {
                    dialog.dismiss()
                    openSelectedApp()
                } else {
                    handler.postDelayed(this, 1000L)
                }
            }
        }
        dialog.setOnDismissListener { handler.removeCallbacks(timer) }
        handler.postDelayed(timer, 1000L)
    }

    private fun refreshInstalledApps() {
        if (!::detectedApps.isInitialized) return
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                android.content.pm.PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val apps = results
            .asSequence()
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { it.toInstalledApp() }
            .sortedWith(
                compareByDescending<InstalledApp> { it.isGame }
                    .thenBy { it.label.lowercase() },
            )
            .toList()

        if (apps.isEmpty()) {
            selectedPackage = null
            recentApps.removeAllViews()
            detectedApps.removeAllViews()
            updateHomeStats(0)
            detectedApps.addView(TextView(this).apply {
                text = "Không có ứng dụng launcher nào được hệ thống trả về."
                textSize = 12f
                setTextColor(MUTED)
                setPadding(dp(12), dp(16), dp(12), dp(16))
            })
            updateLaunchButton()
            return
        }

        val preferred = apps.firstOrNull {
            it.packageName == BootAnimationActivity.FREE_FIRE_PACKAGE
        } ?: apps.firstOrNull { it.isGame } ?: apps.first()
        if (selectedPackage == null || apps.none { it.packageName == selectedPackage }) {
            selectedPackage = preferred.packageName
            selectedLabel = preferred.label
        }

        val recentPackageOrder = preferences
            .getString(RECENT_PACKAGES_KEY, "")
            .orEmpty()
            .split('|')
            .filter { it.isNotBlank() }
        val recentAppsToShow = recentPackageOrder
            .mapNotNull { recentPackage ->
                apps.firstOrNull { it.packageName == recentPackage }
            } + apps.filter { app ->
                app.packageName !in recentPackageOrder
            }

        recentApps.removeAllViews()
        recentAppsToShow.take(6).forEach { app ->
            recentApps.addView(createRecentCard(app), LinearLayout.LayoutParams(
                dp(148),
                dp(78),
            ).apply {
                marginEnd = dp(8)
            })
        }

        detectedApps.removeAllViews()
        apps.forEach { app ->
            detectedApps.addView(createLibraryRow(app), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58),
            ).apply {
                bottomMargin = dp(6)
            })
        }
        updateHomeStats(apps.size)
        updateLaunchButton()
    }

    private fun rememberRecentPackage(packageName: String) {
        val current = preferences
            .getString(RECENT_PACKAGES_KEY, "")
            .orEmpty()
            .split('|')
            .filter { it.isNotBlank() }
            .toMutableList()
        current.remove(packageName)
        current.add(0, packageName)
        preferences.edit()
            .putString(RECENT_PACKAGES_KEY, current.take(12).joinToString("|"))
            .apply()
    }

    private fun createRecentCard(app: InstalledApp): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(8), dp(8))
            background = if (app.packageName == selectedPackage) {
                gradientBackground(
                    intArrayOf(Color.rgb(27, 45, 72), Color.rgb(53, 32, 77)),
                    dp(10),
                )
            } else {
                solidBackground(PANEL, dp(10))
            }
            setOnClickListener {
                selectedPackage = app.packageName
                selectedLabel = app.label
                refreshInstalledApps()
            }
        }
        card.addView(ImageView(this).apply {
            setImageDrawable(app.icon)
            contentDescription = app.label
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply {
            marginEnd = dp(8)
        })
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        copy.addView(TextView(this).apply {
            text = app.label
            textSize = 11f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
        }, matchWrap())
        copy.addView(TextView(this).apply {
            text = if (app.isGame) "Game" else "App"
            textSize = 9f
            setTextColor(CYAN)
        }, matchWrap())
        card.addView(copy, LinearLayout.LayoutParams(0, dp(50), 1f))
        return card
    }

    private fun createLibraryRow(app: InstalledApp): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = if (app.packageName == selectedPackage) {
                gradientBackground(
                    intArrayOf(Color.rgb(26, 43, 67), Color.rgb(47, 29, 69)),
                    dp(9),
                )
            } else {
                solidBackground(Color.rgb(18, 22, 35), dp(9))
            }
            setOnClickListener {
                selectedPackage = app.packageName
                selectedLabel = app.label
                refreshInstalledApps()
            }
        }
        row.addView(ImageView(this).apply {
            setImageDrawable(app.icon)
            contentDescription = app.label
        }, LinearLayout.LayoutParams(dp(40), dp(40)).apply {
            marginEnd = dp(10)
        })
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        copy.addView(TextView(this).apply {
            text = if (app.packageName == selectedPackage) "✓  ${app.label}" else app.label
            textSize = 13f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
        }, matchWrap())
        copy.addView(TextView(this).apply {
            text = app.packageName
            textSize = 9f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(MUTED)
        }, matchWrap())
        row.addView(copy, LinearLayout.LayoutParams(0, dp(46), 1f))
        row.addView(TextView(this).apply {
            text = if (app.isGame) "GAME" else "APP"
            textSize = 8f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(if (app.isGame) CYAN else Color.rgb(181, 157, 255))
            background = solidBackground(Color.rgb(29, 35, 57), dp(6))
            setPadding(dp(8), 0, dp(8), 0)
        }, LinearLayout.LayoutParams(dp(48), dp(24)))
        return row
    }

    private fun ResolveInfo.toInstalledApp(): InstalledApp {
        val info = activityInfo.applicationInfo
        return InstalledApp(
            packageName = activityInfo.packageName,
            label = info.loadLabel(packageManager).toString(),
            icon = info.loadIcon(packageManager),
            isGame = info.category == ApplicationInfo.CATEGORY_GAME ||
                activityInfo.packageName == BootAnimationActivity.FREE_FIRE_PACKAGE,
        )
    }

    private fun updateLaunchButton() {
        if (!::launchButton.isInitialized) return
        if (!Settings.canDrawOverlays(this)) {
            launchButton.text = "ALLOW OVERLAY PERMISSION"
        } else if (selectedPackage == null) {
            launchButton.text = "NO LAUNCHABLE APP"
        } else {
            launchButton.text = "LAUNCH  $selectedLabel"
        }
    }

    private fun animateHome(root: LinearLayout) {
        root.alpha = 0f
        root.post {
            root.animate()
                .alpha(1f)
                .setDuration(420L)
                .start()
            for (index in 0 until root.childCount) {
                val child = root.getChildAt(index)
                child.alpha = 0f
                child.translationY = dp(12).toFloat()
                child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(index * 45L)
                    .setDuration(360L)
                    .start()
            }
        }
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 10f
        letterSpacing = 0.13f
        setTextColor(MUTED)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun weightParams() = LinearLayout.LayoutParams(0, dp(70), 1f).apply {
        marginEnd = dp(6)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    private fun solidBackground(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun strokeBackground(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        setStroke(dp(1), color)
        cornerRadius = radius.toFloat()
    }

    private fun gradientBackground(colors: IntArray, radius: Int) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = radius.toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private data class InstalledApp(
        val packageName: String,
        val label: String,
        val icon: Drawable,
        val isGame: Boolean,
    )

    companion object {
        private const val RECENT_PACKAGES_KEY = "recent_packages"
        private val BG = Color.rgb(8, 11, 20)
        private val PANEL = Color.rgb(20, 25, 41)
        private val TEXT = Color.rgb(239, 245, 255)
        private val MUTED = Color.rgb(133, 146, 174)
        private val CYAN = Color.rgb(91, 227, 255)
    }
}