package com.craftstudio.launcher.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.craftstudio.launcher.InfoCenter
import com.craftstudio.launcher.InfoDistributor
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.ActivitySplashBinding
import com.craftstudio.launcher.feature.unpack.Components
import com.craftstudio.launcher.feature.unpack.Jre
import com.craftstudio.launcher.feature.unpack.UnpackComponentsTask
import com.craftstudio.launcher.feature.unpack.UnpackJreTask
import com.craftstudio.launcher.feature.unpack.UnpackSingleFilesTask
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.utils.StoragePermissionsUtils
import com.craftstudio.launcher.LauncherActivity
import com.craftstudio.launcher.MissingStorageActivity
import com.craftstudio.launcher.Tools

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private var isStarted: Boolean = false
    private lateinit var binding: ActivitySplashBinding
    private lateinit var installableAdapter: InstallableAdapter
    private val items: MutableList<InstallableItem> = ArrayList()
    private val handler = Handler(Looper.getMainLooper())
    private var shimmerRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initItems()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startLogoAnimation()
        startShimmerAnimation()

        if (!Tools.checkStorageRoot()) {
            startActivity(Intent(this, MissingStorageActivity::class.java))
            finish()
            return
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !StoragePermissionsUtils.hasStoragePermissions(this)) {
            TipDialog.Builder(this)
                .setTitle(R.string.generic_warning)
                .setMessage(InfoCenter.replaceName(this, R.string.permissions_write_external_storage))
                .setWarning()
                .setConfirmClickListener { requestStoragePermissions() }
                .setCancelClickListener { checkEnd() }
                .showDialog()
        } else {
            checkEnd()
        }
    }

    private fun startLogoAnimation() {
        binding.ivLogo.apply {
            scaleX = 0.9f
            scaleY = 0.9f
            alpha = 0.8f
            animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .alpha(1f)
                .setDuration(1200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .alpha(0.9f)
                        .setDuration(1200)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction { startLogoAnimation() }
                        .start()
                }
                .start()
        }
    }

    private fun startShimmerAnimation() {
        val shimmerView = binding.progressBar.findViewById<View>(R.id.progress_shimmer) ?: return
        shimmerRunnable = object : Runnable {
            override fun run() {
                val parentWidth = (shimmerView.parent as? View)?.width ?: shimmerView.width
                shimmerView.translationX = -shimmerView.width.toFloat()
                shimmerView.animate()
                    .translationX(parentWidth.toFloat())
                    .setDuration(1500)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { this.run() }
                    .start()
            }
        }
        handler.post(shimmerRunnable!!)
    }

    private fun updateProgress(progress: Int, status: String) {
        handler.post {
            binding.tvStatus.text = status
            binding.tvPercent.text = "$progress%"

            binding.progressBar.findViewById<View>(R.id.progress_fill)?.let { fill ->
                fill.layoutParams = fill.layoutParams.apply {
                    width = (binding.progressBar.width * progress / 100)
                }
            }
        }
    }

    private fun startInstallIfNeeded() {
        if (isStarted) return
        isStarted = true
        updateProgress(0, getString(R.string.splash_screen_installing))
        installableAdapter.startAllTasks()
    }

    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            checkEnd()
        }
    }

    private fun initItems() {
        Components.entries.forEach {
            val unpackComponentsTask = UnpackComponentsTask(this, it)
            if (!unpackComponentsTask.isCheckFailed()) {
                items.add(
                    InstallableItem(
                        it.displayName,
                        it.summary?.let { it1 -> getString(it1) },
                        unpackComponentsTask
                    )
                )
            }
        }
        Jre.entries.forEach {
            val unpackJreTask = UnpackJreTask(this, it)
            if (!unpackJreTask.isCheckFailed()) {
                items.add(
                    InstallableItem(
                        it.jreName,
                        getString(it.summary),
                        unpackJreTask
                    )
                )
            }
        }
        items.sort()
        installableAdapter = InstallableAdapter(items) {
            toMain()
        }
    }
    
    private fun checkEnd() {
        installableAdapter.checkAllTask()
        Task.runTask {
            UnpackSingleFilesTask(this).run()
        }.execute()

        startInstallIfNeeded()
    }

    private fun toMain() {
        startActivity(Intent(this, LauncherActivity::class.java))
        finish()
    }

    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1
    }

    override fun onDestroy() {
        super.onDestroy()
        shimmerRunnable?.let { handler.removeCallbacks(it) }
    }
}