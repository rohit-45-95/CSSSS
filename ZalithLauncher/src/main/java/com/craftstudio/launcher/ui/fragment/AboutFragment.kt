package com.craftstudio.launcher.ui.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
        val defaultAvatar = resources.getDrawable(R.drawable.ic_profile_default, requireContext().theme)

        // Developers
        addTeamCard(binding.devCardsContainer, defaultAvatar, "ROHIT", "Lead Developer")
        addTeamCard(binding.devCardsContainer, defaultAvatar, "ENDER WARRIOR", "Developer")
        addTeamCard(binding.devCardsContainer, defaultAvatar, "MINER ADI", "Developer")
        addTeamCard(binding.devCardsContainer, defaultAvatar, "ONIZ.EXE", "Developer")

        // Founders
        addTeamCard(binding.founderCardsContainer, defaultAvatar, "NOT DANGER", "Founder & Owner")

        // Providers & Staff
        addTeamCard(binding.staffCardsContainer, defaultAvatar, "RKMC", "Main Provider")
        addTeamCard(binding.staffCardsContainer, defaultAvatar, "REALONESKY", "Second Provider")
        addTeamCard(binding.staffCardsContainer, defaultAvatar, "BLIND GAMERRZ", "Head Moderator")
        addTeamCard(binding.staffCardsContainer, defaultAvatar, "NOTERRORX", "Head Administrator")
    }

    private fun addTeamCard(container: LinearLayout, avatar: android.graphics.drawable.Drawable, name: String, role: String) {
        val card = LayoutInflater.from(requireContext()).inflate(R.layout.item_about_member, container, false)

        card.findViewById<ImageView>(R.id.member_avatar).setImageDrawable(avatar)
        card.findViewById<TextView>(R.id.member_name).text = name
        card.findViewById<TextView>(R.id.member_role).text = role

        container.addView(card)
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

            if (cardTop < scrollY + screenHeight - (50 * resources.displayMetrics.density).toInt()) {
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
