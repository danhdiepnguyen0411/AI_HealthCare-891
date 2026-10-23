package com.example.healthcareai.ui.profile

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import coil.load
import coil.transform.CircleCropTransformation
import com.example.healthcareai.R
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.data.model.UserProfile
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.databinding.ActivityAddProfileBinding
import com.example.healthcareai.databinding.BottomSheetSelectGenderBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.main.MainActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityAddProfileBinding
    private lateinit var profileRepository: ProfileRepository
    private lateinit var preferenceHelper: PreferenceHelper

    private var selectedAvatarUri: Uri? = null
    private val calendar = Calendar.getInstance()
    private val dobDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private var selectedDob: String = ""
    private var selectedGender: String = ""

    private var lastImeInsetsBottom = 0
    private var lastGlobalImeBottom = 0
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var wasKeyboardOpen = false

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedAvatarUri = it
                binding.ivAvatar.load(it) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileRepository = ProfileRepository.getInstance(this)
        preferenceHelper = PreferenceHelper.getInstance(this)

        setupKeyboardInsetsListener()
        setupListeners()
        setupBackPress()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@AddProfileActivity, com.example.healthcareai.ui.intro.IntroActivity::class.java).apply {
                    putExtra(com.example.healthcareai.ui.intro.IntroActivity.EXTRA_START_PAGE, 3)
                }
                startActivity(intent)
                finish()
            }
        })
    }

    private fun setupKeyboardInsetsListener() {
        val density = resources.displayMetrics.density

        // 1. WindowInsetsAnimationCompat for smooth keyboard transition
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.rootAddProfile,
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootAddProfile) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            lastImeInsetsBottom = imeBottom
            updateInsets(statusTop, navBottom, getEffectiveImeBottom())
            insets
        }

        val currentInsets = ViewCompat.getRootWindowInsets(binding.rootAddProfile)
        if (currentInsets != null) {
            val navBottom = currentInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = currentInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val statusTop = currentInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            lastImeInsetsBottom = imeBottom
            updateInsets(statusTop, navBottom, getEffectiveImeBottom())
        }

        // 3. GlobalLayoutListener as fallback
        val decorView = window.decorView
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            decorView.getWindowVisibleDisplayFrame(r)
            val screenHeight = decorView.rootView.height
            val heightDiff = screenHeight - r.bottom
            val isKeyboardVisible = heightDiff > (100 * density)
            lastGlobalImeBottom = if (isKeyboardVisible) heightDiff else 0
            val rootInsets = ViewCompat.getRootWindowInsets(binding.rootAddProfile)
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
        val isKeyboardOpen = imeBottom > 0
        val keyboardStateChanged = isKeyboardOpen != wasKeyboardOpen
        wasKeyboardOpen = isKeyboardOpen

        // Update top bar margin for status bar
        if (statusTop > 0) {
            val targetTopMargin = statusTop + (8 * density).toInt()
            (binding.topBar.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                if (lp.topMargin != targetTopMargin) {
                    lp.topMargin = targetTopMargin
                    binding.topBar.layoutParams = lp
                }
            }
        }

        // Update layoutBottomAction (Create profile button) position
        val finalBottomMargin = if (isKeyboardOpen) imeBottom + (8 * density).toInt() else navBottom + (16 * density).toInt()
        val targetPaddingBottom = if (isKeyboardOpen) (4 * density).toInt() else (24 * density).toInt()

        (binding.layoutBottomAction.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            if (lp.bottomMargin != finalBottomMargin) {
                lp.bottomMargin = finalBottomMargin
                binding.layoutBottomAction.layoutParams = lp
            }
        }

        if (binding.layoutBottomAction.paddingBottom != targetPaddingBottom) {
            binding.layoutBottomAction.setPadding(
                (20 * density).toInt(),
                binding.layoutBottomAction.paddingTop,
                (20 * density).toInt(),
                targetPaddingBottom
            )
        }

        // When keyboard is open and focus is on height or weight, smoothly scroll down so textfields are completely visible
        if (isKeyboardOpen) {
            val focused = currentFocus
            val shouldScroll = focused == null || focused.id == R.id.etHeight || focused.id == R.id.etWeight
            if (shouldScroll) {
                binding.scrollView.post { scrollToBottom() }
                if (keyboardStateChanged) {
                    binding.scrollView.postDelayed({ scrollToBottom() }, 100)
                    binding.scrollView.postDelayed({ scrollToBottom() }, 250)
                }
            }
        }
    }

    private fun scrollToBottom() {
        val child = binding.scrollView.getChildAt(0) ?: return
        val childHeight = maxOf(child.height, child.bottom)
        val maxScroll = maxOf(0, childHeight + binding.scrollView.paddingBottom - binding.scrollView.height)
        binding.scrollView.smoothScrollTo(0, maxScroll)
    }

    override fun onDestroy() {
        super.onDestroy()
        globalLayoutListener?.let {
            window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
    }

    private fun setupListeners() {
        // Dismiss keyboard when touching background
        binding.rootAddProfile.setOnClickListener {
            currentFocus?.clearFocus()
            hideKeyboard()
        }
        binding.scrollView.setOnClickListener {
            currentFocus?.clearFocus()
            hideKeyboard()
        }

        // Avatar selection
        val onPickAvatarClick = View.OnClickListener {
            currentFocus?.clearFocus()
            hideKeyboard()
            pickImageLauncher.launch("image/*")
        }
        binding.avatarContainer.setOnClickListener(onPickAvatarClick)
        binding.btnPickAvatar.setOnClickListener(onPickAvatarClick)

        // Keyboard actions
        binding.etFullName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.etFullName.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }

        binding.etWeight.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.etWeight.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Auto-scroll when height or weight gains focus or is clicked
        val onInputFocusChange = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.scrollView.postDelayed({ scrollToBottom() }, 100)
                binding.scrollView.postDelayed({ scrollToBottom() }, 250)
            }
        }
        binding.etHeight.onFocusChangeListener = onInputFocusChange
        binding.etWeight.onFocusChangeListener = onInputFocusChange

        binding.etHeight.setOnClickListener {
            binding.scrollView.postDelayed({ scrollToBottom() }, 100)
        }
        binding.etWeight.setOnClickListener {
            binding.scrollView.postDelayed({ scrollToBottom() }, 100)
        }

        // Date of birth picker
        binding.cardDateOfBirth.setOnClickListener {
            currentFocus?.clearFocus()
            hideKeyboard()
            showDatePicker()
        }

        // Gender picker
        binding.cardGender.setOnClickListener {
            currentFocus?.clearFocus()
            hideKeyboard()
            showGenderBottomSheet()
        }

        // Create profile
        binding.btnCreateProfile.setOnClickListener {
            currentFocus?.clearFocus()
            hideKeyboard()
            handleCreateProfile()
        }
    }

    private fun showDatePicker() {
        hideKeyboard()
        val cal = Calendar.getInstance()
        if (selectedDob.isNotEmpty()) {
            cal.time = calendar.time
        } else {
            // Default picker view to year 2000 for good UX
            cal.set(2000, Calendar.JANUARY, 1)
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDob = dobDateFormat.format(calendar.time)
                binding.tvDateOfBirth.text = selectedDob
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        // Upper bound: today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showGenderBottomSheet() {
        hideKeyboard()
        val bottomSheet = BottomSheetDialog(this, R.style.TransparentBottomSheetDialogTheme)
        val sheetBinding = BottomSheetSelectGenderBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        fun updateChecks(gender: String) {
            sheetBinding.ivCheckMale.visibility = if (gender == "Male") View.VISIBLE else View.GONE
            sheetBinding.ivCheckFemale.visibility = if (gender == "Female") View.VISIBLE else View.GONE
            sheetBinding.ivCheckOther.visibility = if (gender == "Other") View.VISIBLE else View.GONE
        }

        updateChecks(selectedGender)

        sheetBinding.cardGenderMale.setOnClickListener {
            selectedGender = "Male"
            binding.tvGender.text = getString(R.string.gender_male)
            updateChecks(selectedGender)
            bottomSheet.dismiss()
        }

        sheetBinding.cardGenderFemale.setOnClickListener {
            selectedGender = "Female"
            binding.tvGender.text = getString(R.string.gender_female)
            updateChecks(selectedGender)
            bottomSheet.dismiss()
        }

        sheetBinding.cardGenderOther.setOnClickListener {
            selectedGender = "Other"
            binding.tvGender.text = getString(R.string.gender_other)
            updateChecks(selectedGender)
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun handleCreateProfile() {
        val name = binding.etFullName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.error_name_empty, Toast.LENGTH_SHORT).show()
            binding.etFullName.requestFocus()
            return
        }

        if (selectedDob.isEmpty()) {
            Toast.makeText(this, R.string.error_dob_empty, Toast.LENGTH_SHORT).show()
            showDatePicker()
            return
        }

        if (selectedGender.isEmpty()) {
            Toast.makeText(this, R.string.error_gender_empty, Toast.LENGTH_SHORT).show()
            showGenderBottomSheet()
            return
        }

        val heightStr = binding.etHeight.text?.toString()?.trim().orEmpty()
        val height = heightStr.toIntOrNull()
        if (height == null || height <= 0) {
            Toast.makeText(this, R.string.error_height_empty, Toast.LENGTH_SHORT).show()
            binding.etHeight.requestFocus()
            return
        }

        val weightStr = binding.etWeight.text?.toString()?.trim().orEmpty()
        val weight = weightStr.toIntOrNull()
        if (weight == null || weight <= 0) {
            Toast.makeText(this, R.string.error_weight_empty, Toast.LENGTH_SHORT).show()
            binding.etWeight.requestFocus()
            return
        }

        val profile = UserProfile(
            name = name,
            avatarUri = selectedAvatarUri?.toString(),
            dateOfBirth = selectedDob,
            gender = selectedGender,
            heightCm = height,
            weightKg = weight,
            isPrimary = true
        )

        profileRepository.savePrimaryProfile(profile)
        preferenceHelper.isIntroCompleted = true
        preferenceHelper.isProfileCreated = true

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AddProfileActivity::class.java)
            context.startActivity(intent)
        }
    }
}
