package com.example.healthcareai.ui.reminder

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.example.healthcareai.R
import com.example.healthcareai.data.model.ReminderItem
import com.example.healthcareai.data.repository.ReminderRepository
import com.example.healthcareai.databinding.ActivityNewReminderBinding
import com.example.healthcareai.databinding.ItemReminderTimeChipBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class NewReminderActivity : BaseActivity() {

    private lateinit var binding: ActivityNewReminderBinding

    // Time chips list starts empty (no mock data)
    private val timeChipsList = mutableListOf<String>()

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    private var editingReminderId: String? = null

    private var lastImeInsetsBottom = 0
    private var lastGlobalImeBottom = 0
    private var wasKeyboardOpen = false
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingReminderId = intent.getStringExtra(EXTRA_REMINDER_ID)

        setupTopBar()
        setupWindowInsetsAndKeyboard()
        setupCategoryField()
        setupNameAndStrengthFields()
        setupRepeatField()
        setupTimeSection()
        setupActionButtons()

        loadExistingReminderIfAny()
    }

    private fun setupTopBar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupCategoryField() {
        // Initial state: empty text so hint "Select category" is displayed
        binding.tvCategory.text = ""
        binding.layoutStrengthForm.visibility = View.GONE
        binding.etReminderName.hint = getString(R.string.hint_reminder_name)

        binding.cardCategory.setOnClickListener {
            hideKeyboard()
            val currentCategory = binding.tvCategory.text.toString().ifEmpty {
                SelectCategoryBottomSheetDialog.CATEGORY_HEALTH_MONITORING
            }
            SelectCategoryBottomSheetDialog.show(supportFragmentManager, currentCategory) { selected ->
                updateCategoryState(selected)
            }
        }
    }

    private fun updateCategoryState(category: String) {
        binding.tvCategory.text = category
        val isMedication = category.equals(SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION, ignoreCase = true)

        if (isMedication) {
            binding.layoutStrengthForm.visibility = View.VISIBLE
            binding.etReminderName.hint = getString(R.string.hint_enter_medication_name)
        } else {
            binding.layoutStrengthForm.visibility = View.GONE
            binding.etReminderName.hint = getString(R.string.hint_reminder_name)
        }
    }

    private fun setupNameAndStrengthFields() {
        // Textfields are kept completely empty without any mock data
        binding.etReminderName.setText("")
        binding.etStrength.setText("")
        binding.etReminderNotes.setText("")

        // Listen for focus to smoothly position fields above keyboard
        binding.etReminderName.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) scrollToView(v)
        }
        binding.etStrength.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) scrollToView(v)
        }

        binding.etReminderNotes.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollToNotes()
                binding.scrollForm.postDelayed({ scrollToNotes() }, 150)
                binding.scrollForm.postDelayed({ scrollToNotes() }, 300)
            }
        }
        binding.etReminderNotes.setOnClickListener {
            scrollToNotes()
            binding.scrollForm.postDelayed({ scrollToNotes() }, 150)
        }
        binding.cardReminderNotes.setOnClickListener {
            binding.etReminderNotes.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etReminderNotes, InputMethodManager.SHOW_IMPLICIT)
            scrollToNotes()
            binding.scrollForm.postDelayed({ scrollToNotes() }, 150)
        }

        // Form selector
        binding.cardForm.setOnClickListener {
            hideKeyboard()
            val formOptions = arrayOf(
                getString(R.string.form_tablet),
                getString(R.string.form_capsule),
                getString(R.string.form_liquid),
                getString(R.string.form_drops),
                getString(R.string.form_injection),
                getString(R.string.form_inhaler),
                getString(R.string.form_other)
            )

            val currentForm = binding.tvForm.text.toString()
            val selectedIndex = formOptions.indexOf(currentForm).coerceAtLeast(-1)

            AlertDialog.Builder(this)
                .setTitle(R.string.label_reminder_form)
                .setSingleChoiceItems(formOptions, selectedIndex) { dialog, which ->
                    binding.tvForm.text = formOptions[which]
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }
    }

    private fun setupRepeatField() {
        // Repeat field starts with empty text so hint "Choose repeat schedule" is displayed
        binding.tvRepeat.text = ""

        binding.cardRepeat.setOnClickListener {
            hideKeyboard()
            val currentRepeat = binding.tvRepeat.text.toString().ifEmpty {
                SelectRepeatBottomSheetDialog.REPEAT_EVERY_DAY
            }
            SelectRepeatBottomSheetDialog.show(supportFragmentManager, currentRepeat) { selected ->
                binding.tvRepeat.text = selected
            }
        }
    }

    private fun setupTimeSection() {
        renderTimeChips()

        val onAddTimeClicked = View.OnClickListener {
            hideKeyboard()
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val chosenCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                    }
                    val formatted = timeFormat.format(chosenCal.time)
                    if (timeChipsList.contains(formatted)) {
                        Toast.makeText(this, R.string.msg_time_already_added, Toast.LENGTH_SHORT).show()
                    } else {
                        timeChipsList.add(formatted)
                        renderTimeChips()
                    }
                },
                hour,
                minute,
                false
            )
            timePicker.show()
        }

        binding.btnAddTimeEmpty.setOnClickListener(onAddTimeClicked)
        binding.btnAddTimeFilled.setOnClickListener(onAddTimeClicked)
    }

    private fun renderTimeChips() {
        if (timeChipsList.isEmpty()) {
            binding.layoutTimeEmpty.visibility = View.VISIBLE
            binding.layoutTimeFilled.visibility = View.GONE
        } else {
            binding.layoutTimeEmpty.visibility = View.GONE
            binding.layoutTimeFilled.visibility = View.VISIBLE

            binding.flowTimeChips.removeAllViews()
            for (timeStr in timeChipsList) {
                val chipBinding = ItemReminderTimeChipBinding.inflate(layoutInflater, binding.flowTimeChips, false)
                chipBinding.tvChipTime.text = timeStr
                chipBinding.btnRemoveTime.setOnClickListener {
                    timeChipsList.remove(timeStr)
                    renderTimeChips()
                }
                binding.flowTimeChips.addView(chipBinding.root)
            }
        }
    }


    private fun loadExistingReminderIfAny() {
        val id = editingReminderId ?: return
        val existing = ReminderRepository.getInstance(this).getReminder(id) ?: return

        binding.tvTitle.text = getString(R.string.title_edit_reminder)
        updateCategoryState(existing.category)
        binding.etReminderName.setText(existing.name)
        binding.etStrength.setText(existing.strength)
        if (existing.form.isNotBlank()) {
            binding.tvForm.text = existing.form
        }
        if (existing.repeat.isNotBlank()) {
            binding.tvRepeat.text = existing.repeat
        }

        if (existing.timeList.isNotEmpty()) {
            timeChipsList.clear()
            timeChipsList.addAll(existing.timeList)
            renderTimeChips()
        }

        binding.etReminderNotes.setText(existing.notes)
    }

    private fun setupActionButtons() {
        binding.btnSaveReminder.setOnClickListener {
            val reminderName = binding.etReminderName.text?.toString()?.trim().orEmpty()
            if (reminderName.isEmpty()) {
                binding.etReminderName.requestFocus()
                Toast.makeText(this, R.string.msg_enter_reminder_name, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()

            val repo = ReminderRepository.getInstance(this)
            val existing = editingReminderId?.let { repo.getReminder(it) }

            val chosenCategory = binding.tvCategory.text.toString().ifBlank {
                SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION
            }
            val chosenRepeat = binding.tvRepeat.text.toString().ifBlank {
                SelectRepeatBottomSheetDialog.REPEAT_EVERY_DAY
            }
            val times = timeChipsList.toList()

            val itemToSave = ReminderItem(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = reminderName,
                category = chosenCategory,
                strength = binding.etStrength.text?.toString()?.trim().orEmpty(),
                form = binding.tvForm.text?.toString()?.trim().orEmpty(),
                repeat = chosenRepeat,
                timeList = times,
                startDate = existing?.startDate.orEmpty(),
                endDate = existing?.endDate.orEmpty(),
                notes = binding.etReminderNotes.text?.toString()?.trim().orEmpty(),
                isDone = existing?.isDone ?: false,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )

            repo.saveReminder(itemToSave)

            val msg = if (existing != null) R.string.msg_reminder_updated else R.string.msg_reminder_saved
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupWindowInsetsAndKeyboard() {
        val density = resources.displayMetrics.density

        // 1. WindowInsetsAnimationCompat for smooth keyboard transition
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.rootNewReminder,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                    val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                    lastImeInsetsBottom = imeInsets
                    updateInsets(statusInsets, navInsets, getEffectiveImeBottom())
                    return insets
                }
            }
        )

        // 2. OnApplyWindowInsetsListener for state changes
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootNewReminder) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            lastImeInsetsBottom = imeBottom
            updateInsets(statusTop, navBottom, getEffectiveImeBottom())
            insets
        }

        // Apply immediately if already available
        val currentInsets = ViewCompat.getRootWindowInsets(binding.rootNewReminder)
        if (currentInsets != null) {
            val navBottom = currentInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = currentInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val statusTop = currentInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            lastImeInsetsBottom = imeBottom
            updateInsets(statusTop, navBottom, getEffectiveImeBottom())
        }

        // 3. Fallback ViewTreeObserver listener for OEM devices where IME insets are not dispatched
        val decorView = window.decorView
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            decorView.getWindowVisibleDisplayFrame(r)
            val screenHeight = decorView.rootView.height
            val heightDiff = screenHeight - r.bottom
            val isKeyboardVisible = heightDiff > (100 * density)
            lastGlobalImeBottom = if (isKeyboardVisible) heightDiff else 0
            val rootInsets = ViewCompat.getRootWindowInsets(binding.rootNewReminder)
            val navBottom = rootInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            val statusTop = rootInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            updateInsets(statusTop, navBottom, getEffectiveImeBottom())
        }
        globalLayoutListener = layoutListener
        decorView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun getEffectiveImeBottom(): Int {
        return if (lastImeInsetsBottom > 0) lastImeInsetsBottom else lastGlobalImeBottom
    }

    private fun updateInsets(statusTop: Int, navBottom: Int, imeBottom: Int) {
        val density = resources.displayMetrics.density
        val isKeyboardOpen = imeBottom > (50 * density)
        val keyboardStateChanged = isKeyboardOpen != wasKeyboardOpen
        wasKeyboardOpen = isKeyboardOpen

        // Adjust top bar margin for status bar
        if (statusTop > 0) {
            val targetTopMargin = statusTop + (8 * density).toInt()
            (binding.topBar.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                if (lp.topMargin != targetTopMargin) {
                    lp.topMargin = targetTopMargin
                    binding.topBar.layoutParams = lp
                }
            }
        }

        // Keep layoutBottomAction fixed at the bottom of the screen (keyboard will cover it)
        (binding.layoutBottomAction.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            if (lp.bottomMargin != navBottom) {
                lp.bottomMargin = navBottom
                binding.layoutBottomAction.layoutParams = lp
            }
        }

        // Adjust scrollForm bottom margin so its content floats above the keyboard when open
        val actionHeight = if (binding.layoutBottomAction.height > 0) {
            binding.layoutBottomAction.height
        } else {
            (146 * density).toInt()
        }
        val bottomBarHeight = actionHeight + navBottom
        val excessKeyboard = (imeBottom - bottomBarHeight).coerceAtLeast(0)

        (binding.scrollForm.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            if (lp.bottomMargin != excessKeyboard) {
                lp.bottomMargin = excessKeyboard
                binding.scrollForm.layoutParams = lp
            }
        }

        // When keyboard opens or state changes and note field is focused, scroll to note field nicely
        if (isKeyboardOpen) {
            if (binding.etReminderNotes.hasFocus()) {
                scrollToNotes()
                if (keyboardStateChanged) {
                    binding.scrollForm.postDelayed({ scrollToNotes() }, 100)
                    binding.scrollForm.postDelayed({ scrollToNotes() }, 250)
                }
            } else {
                currentFocus?.let { focused ->
                    if (focused is EditText) {
                        scrollToView(focused)
                    }
                }
            }
        }
    }

    private fun scrollToNotes() {
        binding.scrollForm.post {
            val card = binding.cardReminderNotes
            val directChild = binding.scrollForm.getChildAt(0) ?: return@post
            val viewportHeight = binding.scrollForm.height
            if (viewportHeight <= 0) return@post

            var top = 0
            var v: View? = card
            while (v != null && v != directChild && v != binding.scrollForm) {
                top += v.top
                v = v.parent as? View
            }

            val cardHeight = card.height.coerceAtLeast((54 * resources.displayMetrics.density).toInt())
            val labelSpace = (36 * resources.displayMetrics.density).toInt()
            // Center the card in the visible viewport, keeping the "Notes" label comfortably visible above it
            val targetY = top - labelSpace - (viewportHeight - cardHeight - labelSpace).coerceAtLeast(0) / 2
            val maxScroll = maxOf(0, directChild.height - viewportHeight)
            val clampedY = targetY.coerceIn(0, maxScroll)
            binding.scrollForm.smoothScrollTo(0, clampedY)
        }
    }

    private fun scrollToView(view: View) {
        binding.scrollForm.post {
            val directChild = binding.scrollForm.getChildAt(0) ?: return@post
            val viewportHeight = binding.scrollForm.height
            if (viewportHeight <= 0) return@post

            var top = 0
            var v: View? = view
            while (v != null && v != directChild && v != binding.scrollForm) {
                top += v.top
                v = v.parent as? View
            }

            val viewHeight = view.height
            val targetY = top - (viewportHeight - viewHeight).coerceAtLeast(0) / 3
            val maxScroll = maxOf(0, directChild.height - viewportHeight)
            val clampedY = targetY.coerceIn(0, maxScroll)
            binding.scrollForm.smoothScrollTo(0, clampedY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        globalLayoutListener?.let {
            window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}
