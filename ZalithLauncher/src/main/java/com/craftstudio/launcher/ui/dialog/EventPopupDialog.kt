package com.craftstudio.launcher.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.craftstudio.launcher.R

class EventPopupDialog(
    context: Context,
    private val onNavigateToCursorStudio: (() -> Unit)? = null
) : Dialog(context, R.style.EventPopupStyle) {

    enum class ActionType {
        OPEN_URL,
        NAVIGATE_CURSOR_STUDIO
    }

    data class PopupItem(
        val imageRes: Int,
        val title: String,
        val actionType: ActionType,
        val actionUrl: String? = null
    )

    private val popupQueue = mutableListOf<PopupItem>()
    private var currentIndex = 0
    private var dontShowAgain = false
    private var actionClicked = false

    private lateinit var cardView: View
    private lateinit var promoImage: ImageView
    private lateinit var promoTitle: TextView
    private lateinit var btnClose: ImageView
    private lateinit var checkboxDontShow: CheckBox

    private val prefs = context.getSharedPreferences("event_popup_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DONT_SHOW = "dont_show_events"

        fun shouldShow(context: Context): Boolean {
            return !context.getSharedPreferences("event_popup_prefs", Context.MODE_PRIVATE)
                .getBoolean(KEY_DONT_SHOW, false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_event_popup)

        window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.CENTER)
            setDimAmount(0f)
        }

        initViews()
        setupQueue()
        showCurrentPopup()
    }

    private fun initViews() {
        cardView = findViewById(R.id.event_card)
        promoImage = findViewById(R.id.promo_image)
        promoTitle = findViewById(R.id.promo_title)
        btnClose = findViewById(R.id.btn_close)
        checkboxDontShow = findViewById(R.id.checkbox_dont_show)

        btnClose.setOnClickListener { dismissAndAdvance() }
        findViewById<View>(R.id.event_popup_root).setOnClickListener { dismissAndAdvance() }
        cardView.setOnClickListener { /* consume, don't dismiss */ }

        promoImage.setOnClickListener {
            val item = popupQueue[currentIndex]
            actionClicked = true
            when (item.actionType) {
                ActionType.OPEN_URL -> {
                    item.actionUrl?.let { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                    dismissAndAdvance()
                }
                ActionType.NAVIGATE_CURSOR_STUDIO -> {
                    dismiss()
                    onNavigateToCursorStudio?.invoke()
                }
            }
        }

        checkboxDontShow.setOnCheckedChangeListener { _, isChecked ->
            dontShowAgain = isChecked
        }
    }

    private fun setupQueue() {
        popupQueue.add(PopupItem(
            R.drawable.promo_discord,
            "Join Our Official Discord",
            ActionType.OPEN_URL,
            "https://discord.gg/RcazEkcFWR"
        ))
        popupQueue.add(PopupItem(
            R.drawable.promo_website,
            "Visit Our Official Website",
            ActionType.OPEN_URL,
            "https://cs-launcher.netlify.app/"
        ))
        popupQueue.add(PopupItem(
            R.drawable.promo_mouse,
            "New Feature: Custom Mouse Cursor",
            ActionType.NAVIGATE_CURSOR_STUDIO
        ))
    }

    private fun showCurrentPopup() {
        if (currentIndex >= popupQueue.size) {
            savePreference()
            dismiss()
            return
        }

        val item = popupQueue[currentIndex]
        promoImage.setImageResource(item.imageRes)
        promoTitle.text = item.title
        checkboxDontShow.isChecked = dontShowAgain
        actionClicked = false

        // Entrance animation
        cardView.visibility = View.INVISIBLE
        cardView.post {
            cardView.visibility = View.VISIBLE
            val enterAnim = AnimationUtils.loadAnimation(context, R.anim.event_popup_enter)
            cardView.startAnimation(enterAnim)
        }
    }

    private fun dismissAndAdvance() {
        val exitAnim = AnimationUtils.loadAnimation(context, R.anim.event_popup_exit)
        exitAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                currentIndex++
                showCurrentPopup()
            }
        })
        cardView.startAnimation(exitAnim)
    }

    private fun savePreference() {
        if (dontShowAgain) {
            prefs.edit().putBoolean(KEY_DONT_SHOW, true).apply()
        }
    }
}
