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
import com.example.healthcareai.databinding.ItemReminderTimeChipBinding
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

    // Time chips list starts empty (no mock data)
    private val timeChipsList = mutableListOf<String>()

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    private var editingReminderId: String? = null
    private var profileId: String? = null
    private var selectedHealthRecordItem: HealthRecordItem? = null
    private var notificationPermissionDialog: Dialog? = null

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionDialog = NotificationPermissionHelper.onPermissionResult(
            activity = this,
            isGranted = isGranted,
            onGranted = {
                finish()
            },
            onDenied = {
                finish()
            }
        )
    }

    private var lastImeInsetsBottom = 0
    private var lastGlobalImeBottom = 0
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

    private fun setupCategoryField() {
        binding.cardCategory.setOnClickListener {
            hideKeyboard()
            val currentCategory = binding.tvCategory.text.toString().ifEmpty {
                SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION
            }
            SelectCategoryBottomSheetDialog.show(supportFragmentManager, currentCategory) { selected ->
                if (!selected.equals(binding.tvCategory.text.toString(), ignoreCase = true)) {
                    selectedHealthRecordItem = null
                    binding.tvChooseItem.text = ""
                    binding.etStrength.setText("")
                    binding.tvForm.text = ""
                }
                updateCategoryState(selected)
            }
        }
    }

    private fun updateCategoryState(category: String) {
        binding.tvCategory.text = category
        when (category.lowercase()) {
            SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION.lowercase() -> {
                binding.layoutChooseItem.visibility = View.VISIBLE
                binding.tvLabelChooseItem.text = "Choose medication"
                binding.tvChooseItem.hint = "Medication"
                binding.layoutStrengthForm.visibility = View.VISIBLE
            }
            SelectCategoryBottomSheetDialog.CATEGORY_VACCINATION.lowercase() -> {
                binding.layoutChooseItem.visibility = View.VISIBLE
                binding.tvLabelChooseItem.text = "Choose vaccination"
                binding.tvChooseItem.hint = "Vaccination"
                binding.layoutStrengthForm.visibility = View.GONE
            }
            SelectCategoryBottomSheetDialog.CATEGORY_APPOINTMENTS.lowercase() -> {
                binding.layoutChooseItem.visibility = View.VISIBLE
                binding.tvLabelChooseItem.text = "Choose appointment"
                binding.tvChooseItem.hint = "Appointment"
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
            val currentCategory = binding.tvCategory.text.toString().ifEmpty {
                SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION
            }
            ChooseHealthItemBottomSheetDialog.show(
                supportFragmentManager,
                category = currentCategory,
                selectedItemId = selectedHealthRecordItem?.id ?: binding.tvChooseItem.text.toString(),
                profileId = profileId
            ) { item ->
                selectedHealthRecordItem = item
                binding.tvChooseItem.text = item.name
                if (currentCategory.equals(SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION, ignoreCase = true)) {
                    if (item.strength.isNotBlank()) {
                        binding.etStrength.setText(item.strength)
                    }
                    if (item.form.isNotBlank()) {
                        binding.tvForm.text = item.form
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
            AddTimeCustomDialog.show(
                this,
                alreadyAddedTimes = timeChipsList,
                onTimeAdded = { newTime ->
                    timeChipsList.add(newTime)
                    renderTimeChips()
                },
                onTimeRemoved = { removedTime ->
                    timeChipsList.remove(removedTime)
                    renderTimeChips()
                }
            )
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
        binding.tvChooseItem.text = existing.name
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
            val chosenCategory = binding.tvCategory.text.toString().ifBlank {
                SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION
            }

            val isSelectableCategory = when (chosenCategory.lowercase()) {
                SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION.lowercase(),
                SelectCategoryBottomSheetDialog.CATEGORY_VACCINATION.lowercase(),
                SelectCategoryBottomSheetDialog.CATEGORY_APPOINTMENTS.lowercase() -> true
                else -> false
            }

            val reminderName = if (isSelectableCategory) {
                binding.tvChooseItem.text?.toString()?.trim().orEmpty()
            } else {
                chosenCategory
            }

            if (reminderName.isEmpty()) {
                if (isSelectableCategory) {
                    Toast.makeText(this, "Please choose a $chosenCategory", Toast.LENGTH_SHORT).show()
                    binding.cardChooseItem.performClick()
                } else {
                    Toast.makeText(this, R.string.msg_enter_reminder_name, Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            hideKeyboard()

            val repo = ReminderRepository.getInstance(this)
            val existing = editingReminderId?.let { repo.getReminder(it) }

            val chosenRepeat = binding.tvRepeat.text.toString().ifBlank {
                SelectRepeatBottomSheetDialog.REPEAT_EVERY_DAY
            }
            val times = timeChipsList.toList()

            val strength = if (chosenCategory.equals(SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION, ignoreCase = true)) {
                binding.etStrength.text?.toString()?.trim().orEmpty()
            } else ""

            val form = if (chosenCategory.equals(SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION, ignoreCase = true)) {
                binding.tvForm.text?.toString()?.trim().orEmpty()
            } else ""

            val effectiveProfileId = existing?.profileId
                ?: profileId
                ?: com.example.healthcareai.data.repository.ProfileRepository.getInstance(this).getPrimaryProfile().id

            val itemToSave = ReminderItem(
                id = existing?.id ?: UUID.randomUUID().toString(),
                profileId = effectiveProfileId,
                name = reminderName,
                category = chosenCategory,
                strength = strength,
                form = form,
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

            if (!PermissionReturnHelper.isNotificationGranted(this)) {
                notificationPermissionDialog = NotificationPermissionHelper.checkAndRequest(
                    activity = this,
                    launcher = requestNotificationPermissionLauncher,
                    onGranted = {
                        finish()
                    },
                    onDenied = {
                        finish()
                    }
                )
            } else {
                finish()
            }
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
        PermissionReturnHelper.stopPolling()
        if (notificationPermissionDialog?.isShowing == true) {
            try { notificationPermissionDialog?.dismiss() } catch (_: Exception) {}
        }
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
