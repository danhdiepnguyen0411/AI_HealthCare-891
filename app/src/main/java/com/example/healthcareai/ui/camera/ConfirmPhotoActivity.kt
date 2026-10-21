package com.example.healthcareai.ui.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.example.healthcareai.R
import com.example.healthcareai.databinding.ActivityConfirmPhotoBinding
import com.example.healthcareai.ui.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

class ConfirmPhotoActivity : BaseActivity() {

    private lateinit var binding: ActivityConfirmPhotoBinding

    enum class Ratio(val w: Float, val h: Float, val title: String) {
        RATIO_9_16(9f, 16f, "9:16"),
        RATIO_1_1(1f, 1f, "1:1"),
        RATIO_2_3(2f, 3f, "2:3"),
        RATIO_3_4(3f, 4f, "3:4"),
        RATIO_16_9(16f, 9f, "16:9")
    }

    private var activeRatio = Ratio.RATIO_9_16

    private data class RatioItemViews(
        val container: View,
        val icon: ImageView,
        val text: TextView,
        val ratio: Ratio
    )

    private lateinit var ratioViewsList: List<RatioItemViews>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        initRatioViews()
        setupListeners()
        loadImage()

        // Wait for frameContainer to layout, then compute initial 9:16 frame dimensions
        binding.frameContainer.post {
            applyAspectRatio(Ratio.RATIO_9_16, animate = false)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootConfirmPhotoLayout) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            if (statusBarTop > 0) {
                binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = statusBarTop + 8
                }
            }
            insets
        }
    }

    private fun initRatioViews() {
        ratioViewsList = listOf(
            RatioItemViews(binding.itemRatio916, binding.ivRatio916, binding.tvRatio916, Ratio.RATIO_9_16),
            RatioItemViews(binding.itemRatio11, binding.ivRatio11, binding.tvRatio11, Ratio.RATIO_1_1),
            RatioItemViews(binding.itemRatio23, binding.ivRatio23, binding.tvRatio23, Ratio.RATIO_2_3),
            RatioItemViews(binding.itemRatio34, binding.ivRatio34, binding.tvRatio34, Ratio.RATIO_3_4),
            RatioItemViews(binding.itemRatio169, binding.ivRatio169, binding.tvRatio169, Ratio.RATIO_16_9)
        )

        ratioViewsList.forEach { item ->
            item.container.setOnClickListener {
                if (activeRatio != item.ratio) {
                    activeRatio = item.ratio
                    updateRatioItemStates()
                    applyAspectRatio(item.ratio, animate = true)
                }
            }
        }
    }

    private fun updateRatioItemStates() {
        val unselectedColor = ContextCompat.getColor(this, R.color.ratio_unselected)
        ratioViewsList.forEach { item ->
            val isSelected = item.ratio == activeRatio
            if (isSelected) {
                item.container.setBackgroundResource(R.drawable.bg_ratio_item_selected)
                item.icon.setColorFilter(Color.WHITE)
                item.text.setTextColor(Color.WHITE)
            } else {
                item.container.setBackgroundResource(R.drawable.bg_ratio_item_unselected)
                item.icon.setColorFilter(unselectedColor)
                item.text.setTextColor(unselectedColor)
            }
        }
    }

    private fun applyAspectRatio(ratio: Ratio, animate: Boolean) {
        val containerWidth = binding.frameContainer.width.toFloat()
        val containerHeight = binding.frameContainer.height.toFloat()
        if (containerWidth <= 0f || containerHeight <= 0f) return

        // Compute maximum fitting dimensions inside container preserving target aspect ratio
        val targetRatio = ratio.w / ratio.h
        val containerRatio = containerWidth / containerHeight

        val targetWidth: Float
        val targetHeight: Float

        if (targetRatio > containerRatio) {
            targetWidth = containerWidth
            targetHeight = containerWidth / targetRatio
        } else {
            targetHeight = containerHeight
            targetWidth = containerHeight * targetRatio
        }

        if (animate) {
            val transition = ChangeBounds().apply {
                duration = 200
            }
            TransitionManager.beginDelayedTransition(binding.frameContainer, transition)
        }

        binding.cardFrame.updateLayoutParams<ViewGroup.LayoutParams> {
            width = targetWidth.toInt()
            height = targetHeight.toInt()
        }

        binding.cardFrame.post {
            binding.cropImageView.fitImageToView()
        }
    }

    private fun loadImage() {
        // 1. Instant in-memory handoff (< 1ms, zero delay!)
        val memoryBitmap = CapturedImageHolder.bitmap
        if (memoryBitmap != null) {
            binding.cropImageView.setImageBitmap(memoryBitmap)
            return
        }

        // 2. Decode from local file path in background if activity restored
        val filePath = intent.getStringExtra(EXTRA_IMAGE_PATH) ?: CapturedImageHolder.imagePath
        if (!filePath.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val bitmap = CapturedImageHolder.decodeScreenScaledBitmap(filePath)
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        CapturedImageHolder.bitmap = bitmap
                        binding.cropImageView.setImageBitmap(bitmap)
                    }
                }
            }
            return
        }

        // 3. Decode from content URI
        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (!uriString.isNullOrEmpty()) {
            val uri = Uri.parse(uriString)
            lifecycleScope.launch(Dispatchers.IO) {
                val bitmap = CapturedImageHolder.decodeScreenScaledBitmap(this@ConfirmPhotoActivity, uri)
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        CapturedImageHolder.bitmap = bitmap
                        binding.cropImageView.setImageBitmap(bitmap)
                    }
                }
            }
            return
        }

        // 4. Fallback demo image
        binding.cropImageView.setImageResource(R.drawable.intro1)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRetakePhoto.setOnClickListener {
            finish()
        }

        binding.btnUsePhoto.setOnClickListener {
            val croppedBitmap: Bitmap? = binding.cropImageView.cropVisibleBitmap()
            Toast.makeText(
                this,
                "Photo confirmed (${activeRatio.title})! Ready for AI analysis.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CapturedImageHolder.clear()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
    }
}
