package com.craftstudio.launcher.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.craftstudio.launcher.R
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.auth.offline.TextureModel
import com.craftstudio.launcher.databinding.FragmentOfflineAccountSettingsBinding
import com.craftstudio.launcher.event.single.AccountUpdateEvent
import com.craftstudio.launcher.feature.accounts.AccountUtils
import com.craftstudio.launcher.feature.accounts.AccountsManager
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.utils.file.FileTools
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.utils.skin.SkinLoader
import com.craftstudio.launcher.utils.skin.SkinPreferenceStore
import com.craftstudio.launcher.value.MinecraftAccount
import org.greenrobot.eventbus.EventBus
import java.io.File

class OfflineAccountSettingsFragment : FragmentWithAnim(R.layout.fragment_offline_account_settings) {
    companion object {
        const val TAG: String = "OfflineAccountSettingsFragment"
    }

    private enum class AssetTarget {
        SKIN,
        CAPE
    }

    private lateinit var binding: FragmentOfflineAccountSettingsBinding
    private var openDocumentLauncher: ActivityResultLauncher<Array<String>>? = null
    private var pendingTarget: AssetTarget? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult<Array<String>, Uri>(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            val pickedUri = uri ?: return@registerForActivityResult
            val target = pendingTarget ?: return@registerForActivityResult
            val account = getLocalAccount() ?: run {
                Toast.makeText(requireContext(), "Select an offline account first", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val outputFile = resolveOutputFile(account, target, pickedUri)
            Task.runTask {
                outputFile.parentFile?.mkdirs()
                if (target == AssetTarget.CAPE) {
                    // keep only one cape type at a time (png or gif)
                    File(PathManager.DIR_USER_CAPE, "${account.uniqueUUID}-cape.png").delete()
                    File(PathManager.DIR_USER_CAPE, "${account.uniqueUUID}-cape.gif").delete()
                }
                FileTools.copyFileInBackground(requireContext(), pickedUri, outputFile)
                if (target == AssetTarget.SKIN) {
                    val avatarCache = assetFile(account, target)
                    avatarCache.parentFile?.mkdirs()
                    FileTools.copyFileInBackground(requireContext(), pickedUri, avatarCache)
                }
            }.ended(TaskExecutors.getAndroidUI()) {
                when (target) {
                    AssetTarget.SKIN -> SkinPreferenceStore.saveSkinPath(requireContext(), account.username, outputFile.absolutePath)
                    AssetTarget.CAPE -> SkinPreferenceStore.saveCapePath(requireContext(), account.username, outputFile.absolutePath)
                }
                Toast.makeText(requireContext(), getString(R.string.generic_saved), Toast.LENGTH_SHORT).show()
                EventBus.getDefault().post(AccountUpdateEvent())
                refreshAccountPreview()
            }.onThrowable(TaskExecutors.getAndroidUI()) { e ->
                Tools.showErrorRemote(requireContext(), R.string.generic_error, e)
            }.execute()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOfflineAccountSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.backButton.setOnClickListener { forceBack() }
        binding.uploadSkinButton.setOnClickListener { pickAsset(AssetTarget.SKIN) }
        binding.uploadCapeButton.setOnClickListener { pickAsset(AssetTarget.CAPE) }
        refreshAccountPreview()
    }

    private fun getLocalAccount(): MinecraftAccount? {
        val account = AccountsManager.currentAccount
        return account?.takeIf { AccountUtils.isNoLoginRequired(it) }
    }

    private fun assetFile(account: MinecraftAccount, target: AssetTarget): File {
        val baseDir = when (target) {
            AssetTarget.SKIN -> PathManager.DIR_USER_SKIN
            AssetTarget.CAPE -> PathManager.DIR_USER_CAPE
        }
        return when (target) {
            AssetTarget.SKIN -> File(baseDir, "${account.uniqueUUID}.png")
            AssetTarget.CAPE -> File(baseDir, "${account.uniqueUUID}-cape.png")
        }
    }

    private fun resolveOutputFile(account: MinecraftAccount, target: AssetTarget, uri: Uri): File {
        if (target == AssetTarget.SKIN) {
            val skinDir = requireContext().getExternalFilesDir(null)?.let { File(it, "skins") }
                ?: File(PathManager.DIR_DATA, "skins")
            skinDir.mkdirs()
            return File(skinDir, "${account.username}.png")
        }

        val fileName = (Tools.getFileName(requireContext(), uri) ?: "").lowercase()
        val isGifCape = fileName.endsWith(".gif")
        return if (isGifCape) {
            File(PathManager.DIR_USER_CAPE, "${account.uniqueUUID}-cape.gif")
        } else {
            File(PathManager.DIR_USER_CAPE, "${account.uniqueUUID}-cape.png")
        }
    }

    private fun pickAsset(target: AssetTarget) {
        if (getLocalAccount() == null) {
            Toast.makeText(requireContext(), "Login with a local account first", Toast.LENGTH_SHORT).show()
            return
        }
        pendingTarget = target
        val mimeTypes = when (target) {
            AssetTarget.SKIN -> arrayOf("image/png")
            AssetTarget.CAPE -> arrayOf("image/png", "image/gif")
        }
        openDocumentLauncher?.launch(mimeTypes)
    }

    private fun refreshAccountPreview() {
        val account = getLocalAccount()
        if (account == null) {
            binding.offlineAccountName.text = "No offline account selected"
            binding.offlineAccountStatus.text = "Local account required"
            binding.offlineAccountStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            binding.uploadSkinButton.isEnabled = false
            binding.uploadCapeButton.isEnabled = false
            binding.skinStatusText.text = "Skin upload unavailable"
            binding.capeStatusText.text = "Cape upload unavailable"
            binding.offlineAccountAvatar.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.default_head))
            return
        }

        binding.offlineAccountName.text = account.username
        binding.offlineAccountStatus.text = AccountUtils.getAccountTypeName(requireContext(), account)
        binding.offlineAccountStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        binding.uploadSkinButton.isEnabled = true
        binding.uploadCapeButton.isEnabled = true

        val skinFile = assetFile(account, AssetTarget.SKIN)
        val capePngFile = File(PathManager.DIR_USER_CAPE, "${account.uniqueUUID}-cape.png")
        val capeGifFile = File(PathManager.DIR_USER_CAPE, "${account.uniqueUUID}-cape.gif")
        binding.skinStatusText.text = if (skinFile.exists()) {
            "Skin: ${skinFile.name}"
        } else {
            "Skin: not set"
        }
        binding.capeStatusText.text = if (capeGifFile.exists()) {
            "Cape: ${capeGifFile.name} (animated GIF)"
        } else if (capePngFile.exists()) {
            "Cape: ${capePngFile.name}"
        } else {
            "Cape: not set"
        }

        try {
            val size = Tools.dpToPx(72f).toInt()
            binding.offlineAccountAvatar.setImageDrawable(
                SkinLoader.getAvatarDrawable(requireContext(), account, size)
            )
        } catch (_: Exception) {
            binding.offlineAccountAvatar.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.default_head))
        }
    }

    override fun slideIn(animPlayer: com.movtery.anim.AnimPlayer) {
        animPlayer.apply(com.movtery.anim.AnimPlayer.Entry(binding.headerCard, com.movtery.anim.animations.Animations.BounceInDown))
            .apply(com.movtery.anim.AnimPlayer.Entry(binding.actionCardsRow, com.movtery.anim.animations.Animations.BounceInUp))
    }

    override fun slideOut(animPlayer: com.movtery.anim.AnimPlayer) {
        animPlayer.apply(com.movtery.anim.AnimPlayer.Entry(binding.headerCard, com.movtery.anim.animations.Animations.FadeOutUp))
            .apply(com.movtery.anim.AnimPlayer.Entry(binding.actionCardsRow, com.movtery.anim.animations.Animations.FadeOutDown))
    }
}
