package com.movtery.zalithlauncher.ui.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentAboutBinding
import com.craftstudio.launcher.ui.view.AnimButton
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.stringutils.StringUtils

class AboutFragment : FragmentWithAnim(R.layout.fragment_about) {
    companion object {
        const val TAG: String = "AboutFragment"
        private const val CARD_DELAY = 120L
        private const val SECTION_DELAY = 250L
    }

    private lateinit var binding: FragmentAboutBinding
    private val animatedViews = mutableSetOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupVersionInfo()
        setupButtons()
        populateTeamCards()
        setupScrollReveal()
        playCascadeAnimation()
    }

    private fun setupVersionInfo() {
        binding.aboutVersionInfo.text = StringUtils.insertSpace(
            getString(R.string.about_version_name),
            ZHTools.getVersionName()
        ) + "  •  " + StringUtils.insertSpace(
            getString(R.string.about_version_code),
            ZHTools.getVersionCode()
        )
    }

    private fun setupButtons() {
        binding.returnButton.setOnClickListener { ZHTools.onBackPressed(requireActivity()) }
        binding.creditsGithubButton.setOnClickListener {
            ZHTools.openLink(requireActivity(), "https://github.com/PojavLauncherTeam/PojavLauncher")
        }
        binding.footerDiscordButton.setOnClickListener {
            ZHTools.openLink(requireActivity(), "https://discord.gg/MqNT9j46Tg")
        }
        binding.footerWebsiteButton.setOnClickListener {
            ZHTools.openLink(requireActivity(), "https://cs-launcher.netlify.app/")
        }
    }

    // ═══════════════════════════════════════════════════
    //  TEAM CARD BUILDER
    // ═══════════════════════════════════════════════════

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun populateTeamCards() {
        val devIcon = resources.getDrawable(R.drawable.ic_role_dev, requireContext().theme)
        val founderIcon = resources.getDrawable(R.drawable.ic_role_founder, requireContext().theme)
        val staffIcon = resources.getDrawable(R.drawable.ic_role_staff, requireContext().theme)

        // Developers
        addTeamCard(binding.devCardsContainer, devIcon, "ROHIT", "Lead Developer")
        addTeamCard(binding.devCardsContainer, devIcon, "ENDER WARRIOR", "Developer")
        addTeamCard(binding.devCardsContainer, devIcon, "MINER ADI", "Developer")
        addTeamCard(binding.devCardsContainer, devIcon, "ONIZ.EXE", "Developer")

        // Founders
        addTeamCard(binding.founderCardsContainer, founderIcon, "NOT DANGER", "Founder & Owner")

        // Providers & Staff
        addTeamCard(binding.staffCardsContainer, staffIcon, "RKMC", "Main Provider")
        addTeamCard(binding.staffCardsContainer, staffIcon, "REALONESKY", "Second Provider")
        addTeamCard(binding.staffCardsContainer, staffIcon, "BLIND GAMERRZ", "Head Moderator")
        addTeamCard(binding.staffCardsContainer, staffIcon, "NOTERRORX", "Head Administrator")
    }

    private fun addTeamCard(container: LinearLayout, icon: android.graphics.drawable.Drawable, name: String, role: String) {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = resources.getDrawable(R.drawable.bg_about_card_green, requireContext().theme)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }

        val iconView = ImageView(requireContext()).apply {
            setImageDrawable(icon)
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
        }

        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            }
        }

        val nameText = TextView(requireContext()).apply {
            text = name
            setTextColor(Color.parseColor("#24B538"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }

        val roleText = TextView(requireContext()).apply {
            text = role
            setTextColor(Color.parseColor("#808080"))
            textSize = 11f
        }

        textLayout.addView(nameText)
        textLayout.addView(roleText)
        card.addView(iconView)
        card.addView(textLayout)

        // Glow indicator bar
        val glowBar = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(3), dp(32)).apply { marginEnd = dp(0) }
            setBackgroundColor(Color.parseColor("#24B538"))
        }
        card.addView(glowBar, 0)

        container.addView(card)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // ═══════════════════════════════════════════════════
    //  CASCADING ENTRANCE ANIMATION
    // ═══════════════════════════════════════════════════

    private fun playCascadeAnimation() {
        val sections = listOf(
            binding.headerSection,
            binding.creditsSection,
            binding.teamDevSection,
            binding.teamFounderSection,
            binding.teamStaffSection,
            binding.footerSection
        )

        sections.forEachIndexed { index, section ->
            section.postDelayed({
                animateSectionIn(section)
                animatedViews.add(section)
            }, index * SECTION_DELAY)
        }

        // Logo shimmer starts after header appears
        binding.aboutLogo.postDelayed({ startLogoShimmer() }, SECTION_DELAY + 300)
    }

    private fun animateSectionIn(view: View) {
        val translateY = ObjectAnimator.ofFloat(view, "translationY", 60f, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        AnimatorSet().apply {
            playTogether(translateY, alpha)
            duration = 400
            interpolator = OvershootInterpolator(1.0f)
            start()
        }
    }

    // ═══════════════════════════════════════════════════
    //  LOGO SHIMMER ANIMATION
    // ═══════════════════════════════════════════════════

    private fun startLogoShimmer() {
        // Subtle floating animation
        val floatUp = ObjectAnimator.ofFloat(binding.aboutLogo, "translationY", -6f, 6f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Gentle rotation
        val rotate = ObjectAnimator.ofFloat(binding.aboutLogo, "rotation", -2f, 2f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Scale pulse
        val scaleX = ObjectAnimator.ofFloat(binding.aboutLogo, "scaleX", 0.95f, 1.05f).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val scaleY = ObjectAnimator.ofFloat(binding.aboutLogo, "scaleY", 0.95f, 1.05f).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }

        AnimatorSet().apply {
            playTogether(floatUp, rotate, scaleX, scaleY)
            start()
        }

        // Title shimmer
        startTitleShimmer()
    }

    private fun startTitleShimmer() {
        val shimmer = ObjectAnimator.ofFloat(binding.aboutBrandTitle, "alpha", 0.6f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        shimmer.start()
    }

    // ═══════════════════════════════════════════════════
    //  SCROLL-BASED CARD REVEAL
    // ═══════════════════════════════════════════════════

    private fun setupScrollReveal() {
        binding.aboutScroll.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, _, _, _ ->
                val scrollY = v.scrollY
                val screenHeight = v.height

                // Reveal team cards as they scroll into view
                revealCardsOnScroll(binding.devCardsContainer, scrollY, screenHeight)
                revealCardsOnScroll(binding.founderCardsContainer, scrollY, screenHeight)
                revealCardsOnScroll(binding.staffCardsContainer, scrollY, screenHeight)
            }
        )
    }

    private fun revealCardsOnScroll(container: LinearLayout, scrollY: Int, screenHeight: Int) {
        for (i in 0 until container.childCount) {
            val card = container.getChildAt(i)
            if (animatedViews.contains(card)) continue

            val location = IntArray(2)
            card.getLocationOnScreen(location)
            val cardTop = location[1]

            if (cardTop < scrollY + screenHeight - dp(50)) {
                animatedViews.add(card)
                card.alpha = 0f
                card.translationY = 30f

                card.postDelayed({
                    ObjectAnimator.ofFloat(card, "alpha", 0f, 1f).apply {
                        duration = 300
                        start()
                    }
                    ObjectAnimator.ofFloat(card, "translationY", 30f, 0f).apply {
                        duration = 350
                        interpolator = OvershootInterpolator(1.0f)
                        start()
                    }
                }, i * CARD_DELAY)
            }
        }
    }

    // ═══════════════════════════════════════════════════
    //  FRAGMENT ANIMATIONS (from FragmentWithAnim)
    // ═══════════════════════════════════════════════════

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.aboutScroll, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.aboutScroll, Animations.FadeOutUp))
    }
}
