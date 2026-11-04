package com.example.healthcareai.ui.camera

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.databinding.BottomSheetReportResultBinding
import com.example.healthcareai.databinding.ItemBottomSheetReportOptionBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ReportResultBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetReportResultBinding? = null
    private val binding get() = _binding!!

    private var onReportSubmittedListener: ((String) -> Unit)? = null

    data class ReportOption(
        val titleRes: Int,
        val key: String
    )

    private val reportOptions = listOf(
        ReportOption(R.string.report_harmful_content, "Harmful content"),
        ReportOption(R.string.report_illegal_content, "Illegal content"),
        ReportOption(R.string.report_offensive_content, "Offensive content"),
        ReportOption(R.string.report_biased_content, "Biased content"),
        ReportOption(R.string.report_trademark_violation, "Trademark violation"),
        ReportOption(R.string.report_copyright_violation, "Copyright violation"),
        ReportOption(R.string.report_nudity_sexual_content, "Nudity/sexual content"),
        ReportOption(R.string.report_violence_gore, "Violence/gore"),
        ReportOption(R.string.report_others, "Others")
    )

    private var selectedOptionIndex = 0
    private var customFeedbackText: String = ""

    private var lastImeInsetsBottom = 0
    private var lastGlobalImeBottom = 0
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReportResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        val adapter = ReportOptionsAdapter(reportOptions)
        binding.rvReportOptions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReportOptions.adapter = adapter
    }

    private fun setupListeners() {
        // Close (X) button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Report Button
        binding.btnReport.setOnClickListener {
            val selectedOption = reportOptions.getOrElse(selectedOptionIndex) { reportOptions.last() }
            val selectedReason = getString(selectedOption.titleRes)
            val finalReason = if (selectedOption.key.equals("Others", ignoreCase = true) && customFeedbackText.isNotBlank()) {
                "$selectedReason: ${customFeedbackText.trim()}"
            } else {
                selectedReason
            }

            onReportSubmittedListener?.invoke(finalReason)
            Toast.makeText(
                requireContext(),
                R.string.toast_report_submitted,
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            if (it is BottomSheetDialog) {
                DialogImmersiveHelper.setupBottomSheetImmersive(it)
                // Clear FLAG_LAYOUT_NO_LIMITS so the window resizes properly with the keyboard
                it.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                @Suppress("DEPRECATION")
                it.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
        setupKeyboardObserver()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        @Suppress("DEPRECATION")
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.background = null
                sheet.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                // Ensure it stays well within screen boundary and layout_weight fills available height
                val displayMetrics = resources.displayMetrics
                val maxAllowedHeight = (displayMetrics.heightPixels * 0.88).toInt()
                behavior.maxHeight = maxAllowedHeight

                val layoutParams = sheet.layoutParams
                layoutParams.height = maxAllowedHeight
                sheet.layoutParams = layoutParams
            }
        }
        DialogImmersiveHelper.setupBottomSheetImmersive(dialog)
        return dialog
    }

    private fun setupKeyboardObserver() {
        val decorView = dialog?.window?.decorView ?: return

        // 1. Smooth 60fps keyboard animation callback
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.rootReportResult,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    lastImeInsetsBottom = imeInsets
                    updateBottomPadding(getEffectiveImeBottom())
                    return insets
                }
            }
        )

        // 2. OnApplyWindowInsetsListener
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            lastImeInsetsBottom = imeBottom
            updateBottomPadding(getEffectiveImeBottom())
            insets
        }

        // 3. Fallback ViewTreeObserver layout listener
        val density = resources.displayMetrics.density
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (_binding == null) return@OnGlobalLayoutListener
            val r = Rect()
            decorView.getWindowVisibleDisplayFrame(r)
            val screenHeight = decorView.rootView.height
            val heightDiff = screenHeight - r.bottom
            val isKeyboardVisible = heightDiff > (100 * density)
            lastGlobalImeBottom = if (isKeyboardVisible) heightDiff else 0
            updateBottomPadding(getEffectiveImeBottom())
        }
        globalLayoutListener = layoutListener
        decorView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun getEffectiveImeBottom(): Int {
        return if (lastImeInsetsBottom > 0) lastImeInsetsBottom else lastGlobalImeBottom
    }

    private fun updateBottomPadding(imeBottom: Int) {
        val binding = _binding ?: return
        val density = resources.displayMetrics.density

        // Keep root padding fixed at the bottom so btnReport NEVER moves up
        val horizontalPadding = (20 * density).toInt()
        val topPadding = (12 * density).toInt()
        val basePaddingBottom = (24 * density).toInt()
        if (binding.rootReportResult.paddingBottom != basePaddingBottom) {
            binding.rootReportResult.setPadding(
                horizontalPadding,
                topPadding,
                horizontalPadding,
                basePaddingBottom
            )
        }

        // Only apply padding to rvReportOptions so textfield / items are pushed up above keyboard
        val targetRvPaddingBottom = if (imeBottom > 0) {
            imeBottom + (16 * density).toInt()
        } else {
            0
        }

        if (binding.rvReportOptions.paddingBottom != targetRvPaddingBottom) {
            binding.rvReportOptions.setPadding(0, 0, 0, targetRvPaddingBottom)
        }

        if (imeBottom > 0 && selectedOptionIndex == reportOptions.size - 1) {
            scrollToOthers()
        }
    }

    private fun scrollToOthers() {
        val rv = _binding?.rvReportOptions ?: return
        val density = resources.displayMetrics.density
        val topOffset = (8 * density).toInt()

        val scrollAction = Runnable {
            val lm = rv.layoutManager as? LinearLayoutManager
            lm?.scrollToPositionWithOffset(reportOptions.size - 1, topOffset)
        }

        rv.post(scrollAction)
        rv.postDelayed(scrollAction, 100)
        rv.postDelayed(scrollAction, 250)
        rv.postDelayed(scrollAction, 400)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        globalLayoutListener?.let { listener ->
            val decorView = dialog?.window?.decorView
            decorView?.viewTreeObserver?.let { observer ->
                if (observer.isAlive) {
                    observer.removeOnGlobalLayoutListener(listener)
                }
            }
        }
        globalLayoutListener = null
        _binding = null
    }

    private inner class ReportOptionsAdapter(
        private val options: List<ReportOption>
    ) : RecyclerView.Adapter<ReportOptionsAdapter.OptionViewHolder>() {

        inner class OptionViewHolder(val itemBinding: ItemBottomSheetReportOptionBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            var textWatcher: TextWatcher? = null
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
            val itemBinding = ItemBottomSheetReportOptionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return OptionViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
            val option = options[position]
            val isSelected = position == selectedOptionIndex
            val isOthers = option.key.equals("Others", ignoreCase = true)
            val context = holder.itemView.context

            // Detach previous text watcher to prevent duplicate callbacks
            holder.textWatcher?.let { holder.itemBinding.etCustomFeedback.removeTextChangedListener(it) }

            holder.itemBinding.tvOptionTitle.text = context.getString(option.titleRes)

            // Selection Visuals
            if (isSelected) {
                holder.itemBinding.cardReportOption.setBackgroundResource(R.drawable.bg_bottom_sheet_item_selected)
                holder.itemBinding.tvOptionTitle.setTextColor(Color.WHITE)
                holder.itemBinding.ivCheckbox.setImageResource(R.drawable.bg_checkbox_selected_white)
            } else {
                holder.itemBinding.cardReportOption.setBackgroundResource(R.drawable.bg_bottom_sheet_item_unselected)
                holder.itemBinding.tvOptionTitle.setTextColor(ContextCompat.getColor(context, R.color.color_text))
                holder.itemBinding.ivCheckbox.setImageResource(R.drawable.bg_checkbox_unchecked)
            }

            // Others Feedback Box (Visible only when Others is selected)
            if (isOthers && isSelected) {
                holder.itemBinding.etCustomFeedback.visibility = View.VISIBLE
                if (holder.itemBinding.etCustomFeedback.text.toString() != customFeedbackText) {
                    holder.itemBinding.etCustomFeedback.setText(customFeedbackText)
                    holder.itemBinding.etCustomFeedback.setSelection(customFeedbackText.length)
                }

                // Internal scrolling for multi-line text input
                holder.itemBinding.etCustomFeedback.setOnTouchListener { v, event ->
                    if (v.hasFocus()) {
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
                            v.parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }

                // On click or focus, scroll into plain sight
                holder.itemBinding.etCustomFeedback.setOnClickListener {
                    scrollToOthers()
                }
                holder.itemBinding.etCustomFeedback.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        scrollToOthers()
                    }
                }

                // Done action on keyboard
                holder.itemBinding.etCustomFeedback.setOnEditorActionListener { v, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        v.clearFocus()
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(v.windowToken, 0)
                        true
                    } else {
                        false
                    }
                }

                // Save feedback text
                val watcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        customFeedbackText = s?.toString() ?: ""
                    }
                }
                holder.textWatcher = watcher
                holder.itemBinding.etCustomFeedback.addTextChangedListener(watcher)
            } else {
                holder.itemBinding.etCustomFeedback.visibility = View.GONE
            }

            // Click listener on card
            holder.itemView.setOnClickListener {
                if (selectedOptionIndex != position) {
                    val prev = selectedOptionIndex
                    selectedOptionIndex = position
                    notifyItemChanged(prev)
                    notifyItemChanged(selectedOptionIndex)

                    if (isOthers) {
                        scrollToOthers()
                        // Request focus and show keyboard
                        holder.itemBinding.etCustomFeedback.postDelayed({
                            holder.itemBinding.etCustomFeedback.requestFocus()
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showSoftInput(holder.itemBinding.etCustomFeedback, InputMethodManager.SHOW_IMPLICIT)
                            scrollToOthers()
                        }, 100)
                    } else {
                        // Hide keyboard when switching away from Others
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(holder.itemView.windowToken, 0)
                    }
                }
            }
        }

        override fun getItemCount(): Int = options.size
    }

    companion object {
        const val TAG = "ReportResultBottomSheetDialog"

        fun show(
            fragmentManager: FragmentManager,
            onReportSubmitted: ((String) -> Unit)? = null
        ): ReportResultBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? ReportResultBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = ReportResultBottomSheetDialog().apply {
                this.onReportSubmittedListener = onReportSubmitted
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
