package com.movtery.zalithlauncher.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.craftstudio.launcher.R

class NotificationPanelDialog(context: Context) : Dialog(context, R.style.CustomDialogStyle) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_notification_panel)

        window?.apply {
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.TOP or Gravity.END)
            attributes?.windowAnimations = R.style.QuickSettingsAnimation
        }

        setupViews()
    }

    private fun setupViews() {
        val btnClose = findViewById<ImageView>(R.id.btn_close)
        val emptyState = findViewById<LinearLayout>(R.id.empty_state)

        btnClose.setOnClickListener { dismiss() }

        // Placeholder notifications
        val notifications = listOf(
            NotificationItem("Welcome to CS Launcher", "Explore the new features!", "2 min ago"),
            NotificationItem("Update Available", "Version 2.0 is ready to download", "1 hour ago"),
            NotificationItem("Community Update", "Join our Discord server", "3 hours ago")
        )

        val container = findViewById<LinearLayout>(R.id.notifications_container)
        if (container != null && notifications.isNotEmpty()) {
            emptyState?.visibility = View.GONE
            for (notification in notifications) {
                val itemView = createNotificationItem(notification)
                container.addView(itemView)
            }
        }
    }

    private fun createNotificationItem(item: NotificationItem): View {
        val context = context
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundResource(R.drawable.bg_notification_item)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 16
            layoutParams = params
        }

        val title = TextView(context).apply {
            text = item.title
            setTextColor(context.getColor(R.color.quick_settings_text))
            textSize = 14f
            paint.isFakeBoldText = true
        }

        val message = TextView(context).apply {
            text = item.message
            setTextColor(0xFFB3FFFFFF.toInt())
            textSize = 12f
            setPadding(0, 8, 0, 0)
        }

        val time = TextView(context).apply {
            text = item.time
            setTextColor(0xFF66FFFFFF.toInt())
            textSize = 10f
            setPadding(0, 4, 0, 0)
        }

        layout.addView(title)
        layout.addView(message)
        layout.addView(time)

        return layout
    }

    data class NotificationItem(
        val title: String,
        val message: String,
        val time: String
    )
}
