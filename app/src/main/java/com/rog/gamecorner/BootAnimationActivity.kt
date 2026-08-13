package com.rog.gamecorner

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

/**
 * Full-screen landscape boot sequence. The sci-fi intro is drawn at runtime,
 * so the project does not need to ship a large video file.
 */
class BootAnimationActivity : Activity() {
    private var hasLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val eyeView = RogFireEyeView(this)
        eyeView.onAnimationFinished = { openTargetApp() }
        setContentView(eyeView)
        eyeView.start()
    }

    private fun openTargetApp() {
        if (hasLaunched) return
        hasLaunched = true

        if (Settings.canDrawOverlays(this)) {
            val serviceIntent = Intent(this, GameOverlayService::class.java)
            startForegroundService(serviceIntent)
        }

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
            ?: FREE_FIRE_PACKAGE
        val gameIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        if (gameIntent == null) {
            Toast.makeText(
                this,
                "Ứng dụng không còn tồn tại hoặc không có màn hình mở.",
                Toast.LENGTH_LONG,
            ).show()
            finish()
            return
        }

        gameIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(gameIntent)
        finish()
    }

    companion object {
        const val FREE_FIRE_PACKAGE = "com.dts.freefireth"
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }
}