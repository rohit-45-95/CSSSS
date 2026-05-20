package com.craftstudio.launcher.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.ModManagerLayoutBinding
import com.craftstudio.launcher.feature.mod.ModToggleHandler
import com.craftstudio.launcher.feature.mod.ModUtils
import com.craftstudio.launcher.feature.mod.parser.ModInfo
import com.craftstudio.launcher.feature.mod.parser.ModParser
import com.craftstudio.launcher.feature.mod.parser.ModParserListener
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.ui.adapter.ModManagerAdapter
import com.craftstudio.launcher.ui.dialog.FilesDialog
import com.craftstudio.launcher.ui.dialog.FilesDialog.FilesButton
import com.craftstudio.launcher.ui.dialog.ModInfoDialog
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileTools
import com.craftstudio.launcher.utils.file.PasteFile
import com.craftstudio.launcher.utils.path.PathManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class ModManagerFragment : FragmentWithAnim(R.layout.mod_manager_layout) {
    companion object {
        const val TAG = "ModManagerFragment"
        const val BUNDLE_VERSION = "version"
    }

    private lateinit var binding: ModManagerLayoutBinding
    private lateinit var modAdapter: ModManagerAdapter
    private lateinit var version: com.craftstudio.launcher.feature.version.Version
    private var currentMods = mutableListOf<ModInfo>()

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            addModFromFile(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ModManagerLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        version = requireArguments().getParcelable(BUNDLE_VERSION) ?: return

        initViews()
        loadMods()
        setupClickListeners()
    }

    private fun initViews() {
        binding.apply {
            modAdapter = ModManagerAdapter(
                onModSelected = { mod, _ ->
                    updateModCounts()
                },
                onModMenuClick = { mod, view ->
                    showModMenu(mod, view)
                }
            )

            modsRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = modAdapter
            }

            bottomActionBar.visibility = View.GONE
            emptyStateText.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            addModFab.setOnClickListener {
                openDocumentLauncher.launch(arrayOf("application/java-archive", "application/x-jar"))
            }

            downloadModFab.setOnClickListener {
                ZHTools.swapFragmentWithAnim(
                    this@ModManagerFragment,
                    DownloadModFragment::class.java,
                    DownloadModFragment.TAG,
                    Bundle().apply {
                        putString(DownloadModFragment.BUNDLE_VERSION_ID, version.id)
                    }
                )
            }

            selectAllButton.setOnClickListener {
                val allSelected = modAdapter.itemCount == modAdapter.getSelectedMods().size
                modAdapter.selectAll(!allSelected)
                updateActionButtons()
            }

            enableSelectedButton.setOnClickListener { enableSelectedMods() }
            disableSelectedButton.setOnClickListener { disableSelectedMods() }
            deleteSelectedButton.setOnClickListener { deleteSelectedMods() }
        }
    }

    private fun loadMods() {
        binding.emptyStateText.visibility = View.VISIBLE

        ModParser.checkAllMods(version, object : ModParserListener {
            override fun onProgress(recentlyParsedModInfo: ModInfo, totalFileCount: Int) {
            }

            override fun onParseEnded(mods: List<ModInfo>) {
                currentMods = mods.toMutableList()
                modAdapter.updateMods(mods)

                binding.apply {
                    if (mods.isEmpty()) {
                        emptyStateText.visibility = View.VISIBLE
                        modsRecyclerView.visibility = View.GONE
                    } else {
                        emptyStateText.visibility = View.GONE
                        modsRecyclerView.visibility = View.VISIBLE
                    }
                    updateModCounts()
                }
            }
        })
    }

    private fun updateModCounts() {
        val totalCount = currentMods.size
        val enabledCount = currentMods.count {
            it.file?.name?.endsWith(ModUtils.JAR_FILE_SUFFIX) == true
        }
        val disabledCount = totalCount - enabledCount

        binding.apply {
            totalModsText.text = getString(R.string.mods_total_count, totalCount)
            enabledModsText.text = getString(R.string.mods_enabled_count, enabledCount)
            disabledModsText.text = getString(R.string.mods_disabled_count, disabledCount)
        }
    }

    private fun updateActionButtons() {
        val selectedCount = modAdapter.getSelectedMods().size
        binding.apply {
            bottomActionBar.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
            enableSelectedButton.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
            disableSelectedButton.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
            deleteSelectedButton.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
        }
    }

    private fun showModMenu(mod: ModInfo, view: View) {
        val filesButton = FilesButton()
        filesButton.setButtonVisibility(true, true, true, false, true, true)
        filesButton.setMessageText(getString(R.string.mod_info_title))

        val dialog = FilesDialog(requireContext(), filesButton,
            Task.runTask(TaskExecutors.getAndroidUI()) { loadMods() },
            mod.file?.parentFile ?: File(PathManager.DIR_GAME), mod.file!!
        )

        if (mod.file?.name?.endsWith(ModUtils.JAR_FILE_SUFFIX) == true) {
            filesButton.setMoreButtonText(getString(R.string.mods_disable))
            dialog.setMoreButtonClick {
                ModUtils.disableMod(mod.file)
                loadMods()
                dialog.dismiss()
            }
        } else {
            filesButton.setMoreButtonText(getString(R.string.mods_enable))
            dialog.setMoreButtonClick {
                ModUtils.enableMod(mod.file)
                loadMods()
                dialog.dismiss()
            }
        }

        filesButton.setCopyButtonText(getString(R.string.mods_info_title))
        dialog.setCopyButtonClick { showModInfoDialog(mod) }

        filesButton.setDeleteButtonText(getString(R.string.mods_delete))
        dialog.setDeleteButtonClick {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.generic_warning)
                .setMessage(getString(R.string.generic_confirm_delete, mod.name ?: mod.file?.name))
                .setPositiveButton(R.string.generic_yes) { _, _ ->
                    mod.file?.delete()
                    loadMods()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.generic_no, null)
                .show()
        }

        dialog.show()
    }

    private fun showModInfoDialog(mod: ModInfo) {
        ModInfoDialog.newInstance(mod).show(childFragmentManager, ModInfoDialog.TAG)
    }

    private fun enableSelectedMods() {
        val selectedMods = modAdapter.getSelectedMods()
        if (selectedMods.isEmpty()) return

        ModToggleHandler(
            requireContext(),
            selectedMods.map { it.file!! },
            Task.runTask(TaskExecutors.getAndroidUI()) { loadMods() }
        ).start()
    }

    private fun disableSelectedMods() {
        val selectedMods = modAdapter.getSelectedMods()
        if (selectedMods.isEmpty()) return

        ModToggleHandler(
            requireContext(),
            selectedMods.map { it.file!! },
            Task.runTask(TaskExecutors.getAndroidUI()) { loadMods() }
        ).start()
    }

    private fun deleteSelectedMods() {
        val selectedMods = modAdapter.getSelectedMods()
        if (selectedMods.isEmpty()) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.generic_warning)
            .setMessage(getString(R.string.generic_confirm_delete_multiple, selectedMods.size))
            .setPositiveButton(R.string.generic_yes) { _, _ ->
                selectedMods.forEach { mod -> mod.file?.delete() }
                loadMods()
            }
            .setNegativeButton(R.string.generic_no, null)
            .show()
    }

    private fun addModFromFile(uri: Uri) {
        val dialog = ZHTools.showTaskRunningDialog(requireActivity())

        Task.runTask {
            val destFile = File(version.getGameDir(), "mods/${System.currentTimeMillis()}.jar")
            FileTools.copyFileInBackground(requireContext(), uri, destFile.absolutePath)
        }.ended(TaskExecutors.getAndroidUI()) {
            Toast.makeText(requireContext(), getString(R.string.profile_mods_added_mod), Toast.LENGTH_SHORT).show()
            loadMods()
        }.onThrowable { e ->
            Tools.showErrorRemote(e)
        }.finallyTask(TaskExecutors.getAndroidUI()) {
            dialog.dismiss()
        }.execute()
    }

    override fun slideIn(animPlayer: com.movtery.anim.AnimPlayer) {
        binding.apply {
            animPlayer.apply(com.movtery.anim.AnimPlayer.Entry(appBarLayout, com.movtery.anim.animations.Animations.SlideInDown))
                .apply(com.movtery.anim.AnimPlayer.Entry(modsRecyclerView, com.movtery.anim.animations.Animations.FadeInUp))
                .apply(com.movtery.anim.AnimPlayer.Entry(bottomActionBar, com.movtery.anim.animations.Animations.SlideInUp))
        }
    }

    override fun slideOut(animPlayer: com.movtery.anim.AnimPlayer) {
        binding.apply {
            animPlayer.apply(com.movtery.anim.AnimPlayer.Entry(appBarLayout, com.movtery.anim.animations.Animations.SlideOutUp))
                .apply(com.movtery.anim.AnimPlayer.Entry(modsRecyclerView, com.movtery.anim.animations.Animations.FadeOutDown))
                .apply(com.movtery.anim.AnimPlayer.Entry(bottomActionBar, com.movtery.anim.animations.Animations.SlideOutDown))
        }
    }
}
