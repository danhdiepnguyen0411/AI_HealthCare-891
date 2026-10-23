package com.example.healthcareai.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.example.healthcareai.R
import com.example.healthcareai.databinding.FragmentScanBinding

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    enum class ScanCategory(val titleResId: Int) {
        SKIN(R.string.category_skin),
        MOUTH(R.string.category_mouth),
        EYE(R.string.category_eye),
        NAILS(R.string.category_nails),
        OTHER_AREA(R.string.category_other_area)
    }

    private var selectedCategory: ScanCategory = ScanCategory.SKIN
    private lateinit var categoryCardMap: Map<ScanCategory, FrameLayout>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCardMapping()
        setupWindowInsets()
        setupCategorySelection()
        setupListeners()
    }

    private fun initCardMapping() {
        categoryCardMap = mapOf(
            ScanCategory.SKIN to binding.cardSkin,
            ScanCategory.MOUTH to binding.cardMouth,
            ScanCategory.EYE to binding.cardEye,
            ScanCategory.NAILS to binding.cardNails,
            ScanCategory.OTHER_AREA to binding.cardOtherArea
        )
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootScanLayout) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val density = resources.displayMetrics.density
            val targetTopMargin = if (statusBarTop > 0) {
                statusBarTop + (8 * density).toInt()
            } else {
                (24 * density).toInt()
            }
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (topMargin != targetTopMargin) {
                    topMargin = targetTopMargin
                }
            }
            insets
        }
    }

    private fun setupCategorySelection() {
        // Initial state: SKIN selected by default (matches Figma design)
        updateCardVisuals(selectedCategory)

        categoryCardMap.forEach { (category, cardView) ->
            cardView.setOnClickListener {
                if (selectedCategory != category) {
                    val previousCategory = selectedCategory
                    selectedCategory = category
                    animateCardSelection(cardView)
                    updateCardVisuals(selectedCategory, previousCategory)
                }
            }
        }
    }

    private fun updateCardVisuals(selected: ScanCategory, previous: ScanCategory? = null) {
        if (previous != null) {
            categoryCardMap[previous]?.setBackgroundResource(R.drawable.bg_scan_card_unselected)
        } else {
            categoryCardMap.forEach { (category, card) ->
                val bgRes = if (category == selected) {
                    R.drawable.bg_scan_card_selected
                } else {
                    R.drawable.bg_scan_card_unselected
                }
                card.setBackgroundResource(bgRes)
            }
        }
        categoryCardMap[selected]?.setBackgroundResource(R.drawable.bg_scan_card_selected)
    }

    private fun animateCardSelection(cardView: View) {
        cardView.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80)
            .withEndAction {
                cardView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun setupListeners() {
        // Start Scan Button -> Open Camera Screen
        binding.btnStartScan.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.example.healthcareai.ui.camera.CameraActivity::class.java).apply {
                putExtra(com.example.healthcareai.ui.camera.CameraActivity.EXTRA_CATEGORY, selectedCategory.name)
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ScanFragment()
    }
}
