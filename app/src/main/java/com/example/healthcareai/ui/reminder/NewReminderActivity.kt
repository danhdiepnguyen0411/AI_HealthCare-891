package com.example.healthcareai.ui.reminder

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
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
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.model.ReminderItem
import com.example.healthcareai.data.repository.ReminderRepository
import com.example.healthcareai.databinding.ActivityNewReminderBinding
import com.example.healthcareai.ui.base.BaseActivity
import android.app.Dialog
import androidx.activity.result.contract.ActivityResultContracts
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.util.NotificationPermissionHelper
import com.example.healthcareai.util.PermissionReturnHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class NewReminderActivity : BaseActivity() {

    private lateinit var binding: ActivityNewReminderBinding

    // Selected hour (0..23) and minute (0..59) for single reminder time
    private var selectedHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var selectedMinute: Int = Calendar.getInstance().get(Calendar.MINUTE)

    private var editingReminderId: String? = null
    private var profileId: String? = null
    private var selectedHealthRecordItem: HealthRecordItem? = null
    private var currentCategoryKey: String = SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION
    private var currentRepeatKey: String = SelectRepeatBottomSheetDialog.REPEAT_EVERY_DAY
    private var currentFormKey: String = ""

    private var lastImeInsetsBottom = 0
    private var lastGlobalImeBottom = 0
    private var isImeReportedByInsets = false
    private var isImeAnimationRunning = false
    private var wasKeyboardOpen = false
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingReminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
        profileId = intent.getStringExtra(EXTRA_PROFILE_ID)

        setupTopBar()
        setupWindowInsetsAndKeyboard()
        setupCategoryField()
        setupChooseItemField()
        setupStrengthAndFormFields()
        setupRepeatField()
        setupTimeSection()
        setupActionButtons()

        if (editingReminderId != null) {
            loadExistingReminderIfAny()
        } else {
            // Default to Medication per design
            updateCategoryState(SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION)
        }
    }

    private fun setupTopBar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun getLocalizedCategoryName(categoryKey: String): String {
        return when (categoryKey.lowercase()) {
            SelectCategoryBottomSheetDialog.CATEGORY_TEMPERATURE.lowercase() -> getString(R.string.cat_temperature)
            SelectCategoryBottomSheetDialog.CATEGORY_HEART_RATE.lowercase() -> getString(R.string.title_heart_rate)
            SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION.lowercase() -> getString(R.string.record_medications)
            SelectCategoryBottomSheetDialog.CATEGORY_DRINK_WATER.lowercase() -> getString(R.string.cat_drink_water)
            SelectCategoryBottomSheetDialog.CATEGORY_EXERCISE.lowercase() -> getString(R.string.cat_exercise)
            SelectCategoryBottomSheetDialog.CATEGORY_SLEEP.lowercase() -> getString(R.string.cat_sleep)
            SelectCategoryBottomSheetDialog.CATEGORY_APPOINTMENTS.lowercase() -> getString(R.string.category_appointments_title)
            SelectCategoryBottomSheetDialog.CATEGORY_VACCINATION.lowercase() -> getString(R.string.record_vaccinations)
            else -> categoryKey
        }
    }

    private fun setupCategoryField() {
        binding.cardCategory.setOnClickListener {
            hideKeyboard()
            SelectCategoryBottomSheetDialog.show(supportFragmentManager, currentCategoryKey) { selected ->
                if (!selected.equals(currentCategoryKey, ignoreCase = true)) {
                    selectedHealthRecordItem = null
                    binding.tvChooseItem.text = ""
                    binding.etStrength.setText("")
                    currentFormKey = ""
                    binding.tvForm.text = ""
                }
                updateCategoryState(selected)
            }
        }
    }

    private fun updateCategoryState(category: String) {
        currentCategoryKey = category
        binding.tvCategory.text = getLocalizedCategoryName(category)
        when (category.lowercase()) {
            SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION.lowercase() -> {
                binding.layoutChooseItem.visibility = View.VISIBLE
                binding.tvLabelChooseItem.text = getString(R.string.title_choose_item_format, getString(R.string.record_medications))
                binding.tvChooseItem.hint = getString(R.string.record_medications)
                binding.layoutStrengthForm.visibility = View.VISIBLE
            }
            SelectCategoryBottomSheetDialog.CATEGORY_VACCINATION.lowercase() -> {
                binding.layoutChooseItem.visibility = View.VISIBLE
                binding.tvLabelChooseItem.text = getString(R.string.title_choose_item_format, getString(R.string.record_vaccinations))
                binding.tvChooseItem.hint = getString(R.string.record_vaccinations)
                binding.layoutStrengthForm.visibility = View.GONE
            }
            SelectCategoryBottomSheetDialog.CATEGORY_APPOINTMENTS.lowercase() -> {
                binding.layoutChooseItem.visibility = View.VISIBLE
                binding.tvLabelChooseItem.text = getString(R.string.title_choose_item_format, getString(R.string.category_appointments_title))
                binding.tvChooseItem.hint = getString(R.string.category_appointments_title)
                binding.layoutStrengthForm.visibility = View.GONE
            }
            else -> {
                // Simple categories: Drink water, Sleep, Exercise, Temperature, Heart rate
                // Only Category, Repeat, Time, Notes (optional) are shown
                binding.layoutChooseItem.visibility = View.GONE
                binding.layoutStrengthForm.visibility = View.GONE
            }
        }
    }

    private fun setupChooseItemField() {
        binding.cardChooseItem.setOnClickListener {
            hideKeyboard()
            val currentCategory = currentCategoryKey
            val currentItemText = binding.tvChooseItem.text?.toString()?.trim().orEmpty()
            val selectedId = selectedHealthRecordItem?.id?.takeIf { it.isNotBlank() }
                ?: selectedHealthRecordItem?.name?.takeIf { it.isNotBlank() }
                ?: currentItemText.takeIf { it.isNotBlank() }

            ChooseHealthItemBottomSheetDialog.show(
                supportFragmentManager,
                category = currentCategory,
                selectedItemId = selectedId,
                profileId = profileId
            ) { item ->
                selectedHealthRecordItem = item
                binding.tvChooseItem.text = item.name
                if (currentCategory.equals(SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION, ignoreCase = true)) {
                    if (item.strength.isNotBlank()) {
                        binding.etStrength.setText(item.strength)
                    }
                    if (item.form.isNotBlank()) {
                        currentFormKey = item.form
                        binding.tvForm.text = com.example.healthcareai.util.ReminderLocalizationHelper.getLocalizedForm(this, item.form)
                    }
                }
            }
        }
    }

    private fun setupStrengthAndFormFields() {
        binding.etStrength.setText("")
        binding.etReminderNotes.setText("")

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

        // Form selector (Bottom Sheet Dialog with standard app styling)
        binding.cardForm.setOnClickListener {
            hideKeyboard()
            val currentForm = currentFormKey.ifBlank { binding.tvForm.text.toString().trim() }
            SelectFormBottomSheetDialog.show(
                supportFragmentManager,
                if (currentForm.isNotEmpty()) currentForm else SelectFormBottomSheetDialog.FORM_TABLET
            ) { selectedFormKey ->
                currentFormKey = selectedFormKey
                binding.tvForm.text = com.example.healthcareai.util.ReminderLocalizationHelper.getLocalizedForm(this, selectedFormKey)
            }
        }
    }

    private fun setupRepeatField() {
        binding.tvRepeat.text = ""

        binding.cardRepeat.setOnClickListener {
            hideKeyboard()
            val repeatToPass = currentRepeatKey.ifEmpty {
                SelectRepeatBottomSheetDialog.REPEAT_EVERY_DAY
            }
            SelectRepeatBottomSheetDialog.show(supportFragmentManager, repeatToPass) { selectedKey ->
                currentRepeatKey = selectedKey
                binding.tvRepeat.text = com.example.healthcareai.util.ReminderLocalizationHelper.getLocalizedRepeat(this, selectedKey)
            }
        }
    }

    private fun setupTimeSection() {
        val hourItems = (0..23).map { String.format(Locale.US, "%02d", it) }
        val minuteItems = (0..59).map { String.format(Locale.US, "%02d", it) }

        binding.wheelHours.setItems(hourItems)
        binding.wheelMinutes.setItems(minuteItems)

        binding.wheelHours.setSelectedPosition(selectedHour)
        binding.wheelMinutes.setSelectedPosition(selectedMinute)

        binding.wheelHours.setOnItemSelectedListener { pos, _ ->
            selectedHour = pos
        }

        binding.wheelMinutes.setOnItemSelectedListener { pos, _ ->
            selectedMinute = pos
        }
    }

    private fun loadExistingReminderIfAny() {
        val id = editingReminderId ?: return
        val existing = ReminderRepository.getInstance(this).getReminder(id) ?: return

        binding.tvTitle.text = getString(R.string.title_edit_reminder)
        updateCategoryState(existing.category)
        binding.tvChooseItem.text = existing.name.ifBlank { existing.getLocalizedName(this) }
        binding.etStrength.setText(existing.strength)
        if (existing.form.isNotBlank()) {
            currentFormKey = existing.form
            binding.tvForm.text = com.example.healthcareai.util.ReminderLocalizationHelper.getLocalizedForm(this, existing.form)
        }
        if (existing.repeat.isNotBlank()) {
            currentRepeatKey = existing.repeat
            binding.tvRepeat.text = com.example.healthcareai.util.ReminderLocalizationHelper.getLocalizedRepeat(this, existing.repeat)
        }

        if (existing.timeList.isNotEmpty()) {
            val firstTime = existing.timeList.first()
            val parsed = com.example.healthcareai.util.ReminderScheduler.parseTimeString(firstTime)
            if (parsed != null) {
                selectedHour = parsed.first
                selectedMinute = parsed.second
                binding.wheelHours.setSelectedPosition(selectedHour)
                binding.wheelMinutes.setSelectedPosition(selectedMinute)
            }
        }

        binding.etReminderNotes.setText(existing.notes)
    }

    private fun setupActionButtons() {
        binding.btnSaveReminder.setOnClickListener {
            val chosenCategory = currentCategoryKey

            val isSelectableCategory = when (chosenCategory.lowercase()) {
                SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION.lowercase(),
                SelectCategoryBottomSheetDialog.CATEGORY_VACCINATION.lowercase(),
                SelectCategoryBottomSheetDialog.CATEGORY_APPOINTMENTS.lowercase() -> true
                else -> false
            }

            val reminderName = if (isSelectableCategory) {
                binding.tvChooseItem.text?.toString()?.trim().orEmpty()
            } else {
                getLocalizedCategoryName(chosenCategory)
            }

            if (reminderName.isEmpty()) {
                if (isSelectableCategory) {
                    val displayName = getLocalizedCategoryName(chosenCategory)
                    Toast.makeText(this, getString(R.string.msg_please_choose_item_format, displayName), Toast.LENGTH_SHORT).show()
                    binding.cardChooseItem.performClick()
                } else {
                    Toast.makeText(this, R.string.msg_enter_reminder_name, Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            hideKeyboard()

            val repo = ReminderRepository.getInstance(this)
            val existing = editingReminderId?.let { repo.getReminder(it) }

            val chosenRepeat = currentRepeatKey.ifBlank {
                SelectRepeatBottomSheetDialog.REPEAT_EVERY_DAY
            }
            val hour12 = when {
                selectedHour == 0 -> 12
                selectedHour > 12 -> selectedHour - 12
                else -> selectedHour
            }
            val amPm = if (selectedHour >= 12) "PM" else "AM"
            val formattedTime = String.format(Locale.US, "%02d:%02d %s", hour12, selectedMinute, amPm)
            val times = listOf(formattedTime)

            val strength = if (chosenCategory.equals(SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION, ignoreCase = true)) {
                binding.etStrength.text?.toString()?.trim().orEmpty()
            } else ""

            val form = if (chosenCategory.equals(SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION, ignoreCase = true)) {
                currentFormKey.ifBlank { binding.tvForm.text?.toString()?.trim().orEmpty() }
            } else ""

            val effectiveProfileId = existing?.profileId
                ?: profileId
                ?: com.example.healthcareai.data.repository.ProfileRepository.getInstance(this).getPrimaryProfile().id

            val now = System.currentTimeMillis()
            val createdTime = if (existing != null && existing.createdAt > 0) existing.createdAt else now
            val effectiveStartDate = if (existing != null && existing.startDate.isNotBlank()) {
                existing.startDate
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(createdTime))
            }

            val itemToSave = ReminderItem(
                id = existing?.id ?: UUID.randomUUID().toString(),
                profileId = effectiveProfileId,
                name = reminderName,
                category = chosenCategory,
                strength = strength,
                form = form,
                repeat = chosenRepeat,
                timeList = times,
                startDate = effectiveStartDate,
                endDate = existing?.endDate.orEmpty(),
                notes = binding.etReminderNotes.text?.toString()?.trim().orEmpty(),
                isDone = existing?.isDone ?: false,
                isEnabled = existing?.isReminderEnabled() ?: true,
                createdAt = createdTime
            )

            repo.saveReminder(itemToSave)

            val msg = if (existing != null) R.string.msg_reminder_updated else R.string.msg_reminder_saved
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK, Intent().putExtra(EXTRA_REMINDER_ID, itemToSave.id))
            finish()
        }

        binding.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun setupWindowInsetsAndKeyboard() {
        val density = resources.displayMetrics.density

        // 1. WindowInsetsAnimationCompat for smooth keyboard transition
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.rootNewReminder,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    super.onPrepare(animation)
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        isImeAnimationRunning = true
                    }
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                    val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                    isImeReportedByInsets = true
                    lastImeInsetsBottom = imeInsets
                    lastGlobalImeBottom = 0
                    updateInsets(statusInsets, navInsets, imeInsets)
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    super.onEnd(animation)
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        isImeAnimationRunning = false
                        val currentInsets = ViewCompat.getRootWindowInsets(binding.rootNewReminder)
                        val imeBottom = currentInsets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                        val navBottom = currentInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
                        val statusTop = currentInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
                        lastImeInsetsBottom = imeBottom
                        lastGlobalImeBottom = 0
                        updateInsets(statusTop, navBottom, imeBottom)
                    }
                }
            }
        )

        // 2. OnApplyWindowInsetsListener for state changes
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootNewReminder) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            if (imeBottom > 0) {
                isImeReportedByInsets = true
            }
            if (!isImeAnimationRunning) {
                lastImeInsetsBottom = imeBottom
                if (imeBottom == 0) lastGlobalImeBottom = 0
                updateInsets(statusTop, navBottom, imeBottom)
            }
            insets
        }

        // Apply immediately if already available
        val currentInsets = ViewCompat.getRootWindowInsets(binding.rootNewReminder)
        if (currentInsets != null) {
            val navBottom = currentInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = currentInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val statusTop = currentInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            lastImeInsetsBottom = imeBottom
            updateInsets(statusTop, navBottom, imeBottom)
        }

        // 3. Fallback ViewTreeObserver listener ONLY if insets are not supported
        val decorView = window.decorView
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (isImeReportedByInsets) return@OnGlobalLayoutListener
            val r = Rect()
            decorView.getWindowVisibleDisplayFrame(r)
            val screenHeight = decorView.rootView.height
            val heightDiff = screenHeight - r.bottom
            val isKeyboardVisible = heightDiff > (100 * density)
            val imeBottom = if (isKeyboardVisible) heightDiff else 0
            val rootInsets = ViewCompat.getRootWindowInsets(binding.rootNewReminder)
            val navBottom = rootInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            val statusTop = rootInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            updateInsets(statusTop, navBottom, imeBottom)
        }
        globalLayoutListener = layoutListener
        decorView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun updateInsets(statusTop: Int, navBottom: Int, imeBottom: Int) {
        val density = resources.displayMetrics.density
        val isKeyboardOpen = imeBottom > (50 * density)
        val keyboardStateChanged = isKeyboardOpen != wasKeyboardOpen
        wasKeyboardOpen = isKeyboardOpen

        // Adjust top bar margin for status bar (standardized 16dp spacing below status bar)
        if (statusTop > 0) {
            val targetTopMargin = statusTop + (16 * density).toInt()
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

        // Only scroll when keyboard is OPENING, not when closing
        if (isKeyboardOpen && keyboardStateChanged) {
            if (binding.etReminderNotes.hasFocus()) {
                scrollToNotes()
                binding.scrollForm.postDelayed({ scrollToNotes() }, 100)
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
        PermissionReturnHelper.stopPolling()
        globalLayoutListener?.let {
            window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_PROFILE_ID = "extra_profile_id"

        fun start(context: Context, reminderId: String? = null, profileId: String? = null) {
            val intent = Intent(context, NewReminderActivity::class.java).apply {
                reminderId?.let { putExtra(EXTRA_REMINDER_ID, it) }
                profileId?.let { putExtra(EXTRA_PROFILE_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}
