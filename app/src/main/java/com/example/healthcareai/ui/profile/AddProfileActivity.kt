package com.example.healthcareai.ui.profile

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
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
import com.example.healthcareai.databinding.BottomSheetSelectRelationshipBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.main.MainActivity
import com.example.healthcareai.util.DialogImmersiveHelper
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
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
    private var selectedRelationship: String = ""
    private var isAddMemberMode: Boolean = false
    private var isEditMode: Boolean = false
    private var editProfileId: String? = null
    private var editingProfile: UserProfile? = null

    private var lastImeInsetsBottom = 0
    private var lastGlobalImeBottom = 0
    private var isImeReportedByInsets = false
    private var isImeAnimationRunning = false
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var wasKeyboardOpen = false

    private fun saveAvatarToInternalStorage(uri: Uri): Uri? {
        return try {
            val avatarDir = File(filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val avatarFile = File(avatarDir, "avatar_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                avatarFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(avatarFile)
        } catch (_: Exception) {
            uri
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { rawUri ->
                val savedUri = saveAvatarToInternalStorage(rawUri) ?: rawUri
                selectedAvatarUri = savedUri
                binding.ivAvatar.load(savedUri) {
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

        editProfileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT, false) && !editProfileId.isNullOrBlank()

        if (isEditMode) {
            editingProfile = profileRepository.getProfileById(editProfileId!!)
                ?: if (editProfileId == ProfileRepository.PRIMARY_PROFILE_ID) profileRepository.getPrimaryProfile() else null
        }

        if (isEditMode && editingProfile != null) {
            val profile = editingProfile!!
            binding.tvTitle.setText(R.string.title_edit_profile)
            binding.tvBtnCreateProfile.setText(R.string.btn_save)
            binding.btnBack.visibility = View.VISIBLE
            binding.btnBack.setOnClickListener { finish() }

            // Pre-fill profile info
            binding.etFullName.setText(profile.name)
            selectedDob = profile.dateOfBirth
            if (selectedDob.isNotBlank()) {
                com.example.healthcareai.util.LocaleHelper.parseStoredDate(selectedDob)?.let { calendar.time = it }
                binding.tvDateOfBirth.text = com.example.healthcareai.util.LocaleHelper.formatStoredDate(this, selectedDob)
            }
            selectedGender = profile.gender
            binding.tvGender.text = when (profile.gender) {
                "Male" -> getString(R.string.gender_male)
                "Female" -> getString(R.string.gender_female)
                else -> getString(R.string.gender_other)
            }
            if (profile.heightCm > 0) binding.etHeight.setText(profile.heightCm.toString())
            if (profile.weightKg > 0) binding.etWeight.setText(profile.weightKg.toString())

            if (!profile.avatarUri.isNullOrEmpty()) {
                selectedAvatarUri = Uri.parse(profile.avatarUri)
                binding.ivAvatar.load(profile.avatarUri) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            }

            if (!profile.isPrimary) {
                binding.layoutRelationship.visibility = View.VISIBLE
                binding.cardRelationship.setOnClickListener {
                    binding.cardRelationship.setBackgroundResource(R.drawable.bg_profile_input_card)
                    showRelationshipBottomSheet()
                }
                selectedRelationship = profile.relationship.orEmpty()
                if (selectedRelationship.isNotBlank()) {
                    binding.tvRelationship.text = com.example.healthcareai.util.ProfileLocalizationHelper.getLocalizedRelationship(this, selectedRelationship)
                }
            } else {
                binding.layoutRelationship.visibility = View.GONE
            }
        } else {
            isAddMemberMode = intent.getBooleanExtra(EXTRA_IS_ADD_MEMBER, false) || preferenceHelper.isProfileCreated
            if (isAddMemberMode) {
                binding.tvTitle.setText(R.string.title_add_member)
                binding.tvBtnCreateProfile.setText(R.string.title_add_member)
                binding.btnBack.visibility = View.VISIBLE
                binding.btnBack.setOnClickListener { finish() }
                binding.layoutRelationship.visibility = View.VISIBLE
                binding.cardRelationship.setOnClickListener {
                    binding.cardRelationship.setBackgroundResource(R.drawable.bg_profile_input_card)
                    showRelationshipBottomSheet()
                }
            }
        }

        setupKeyboardInsetsListener()
        setupListeners()
        setupBackPress()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEditMode || isAddMemberMode) {
                    finish()
                } else {
                    preferenceHelper.clearLanguageIfIntroNotCompleted(this@AddProfileActivity)
                    finishAffinity()
                }
            }
        })
    }

    private fun setupKeyboardInsetsListener() {
        val density = resources.displayMetrics.density

        // 1. WindowInsetsAnimationCompat for smooth keyboard transition
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.rootAddProfile,
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
                        val currentInsets = ViewCompat.getRootWindowInsets(binding.rootAddProfile)
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootAddProfile) { _, insets ->
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

        val currentInsets = ViewCompat.getRootWindowInsets(binding.rootAddProfile)
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
            val rootInsets = ViewCompat.getRootWindowInsets(binding.rootAddProfile)
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

        // Update top bar margin for status bar (standardized 16dp spacing below status bar)
        if (statusTop > 0) {
            val targetTopMargin = statusTop + (16 * density).toInt()
            (binding.topBar.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                if (lp.topMargin != targetTopMargin) {
                    lp.topMargin = targetTopMargin
                    binding.topBar.layoutParams = lp
                }
            }
        }

        // Keep layoutBottomAction (Create profile button) fixed at the bottom of the screen (keyboard covers it)
        (binding.layoutBottomAction.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            if (lp.bottomMargin != navBottom) {
                lp.bottomMargin = navBottom
                binding.layoutBottomAction.layoutParams = lp
            }
        }

        // Adjust scrollView bottom margin so its content floats above the keyboard when open
        val actionHeight = if (binding.layoutBottomAction.height > 0) {
            binding.layoutBottomAction.height
        } else {
            (78 * density).toInt()
        }
        val bottomBarHeight = actionHeight + navBottom
        val excessKeyboard = (imeBottom - bottomBarHeight).coerceAtLeast(0)

        (binding.scrollView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            if (lp.bottomMargin != excessKeyboard) {
                lp.bottomMargin = excessKeyboard
                binding.scrollView.layoutParams = lp
            }
        }

        // Only scroll when keyboard is OPENING, never when closing
        if (isKeyboardOpen && keyboardStateChanged) {
            val focused = currentFocus
            val shouldScroll = focused == null || focused.id == R.id.etHeight || focused.id == R.id.etWeight
            if (shouldScroll) {
                binding.scrollView.post { scrollToBottom() }
                binding.scrollView.postDelayed({ scrollToBottom() }, 100)
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
        // Enforce input length limits (30 chars for name, 3 digits for height and weight)
        binding.etFullName.filters = arrayOf(InputFilter.LengthFilter(30))
        binding.etHeight.filters = arrayOf(InputFilter.LengthFilter(3))
        binding.etWeight.filters = arrayOf(InputFilter.LengthFilter(3))

        // Reset error styling when user types
        binding.etFullName.doAfterTextChanged {
            binding.cardFullNameInput.setBackgroundResource(R.drawable.bg_profile_input_card)
        }
        binding.etHeight.doAfterTextChanged {
            binding.cardHeightInput.setBackgroundResource(R.drawable.bg_profile_input_card)
        }
        binding.etWeight.doAfterTextChanged {
            binding.cardWeightInput.setBackgroundResource(R.drawable.bg_profile_input_card)
        }

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
            binding.cardDateOfBirth.setBackgroundResource(R.drawable.bg_profile_input_card)
            currentFocus?.clearFocus()
            hideKeyboard()
            showDatePicker()
        }

        // Gender picker
        binding.cardGender.setOnClickListener {
            binding.cardGender.setBackgroundResource(R.drawable.bg_profile_input_card)
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
        Locale.setDefault(com.example.healthcareai.util.LocaleHelper.getSavedLocale(this))
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
                binding.tvDateOfBirth.text = com.example.healthcareai.util.LocaleHelper.formatDate(this, calendar.time)
                binding.cardDateOfBirth.setBackgroundResource(R.drawable.bg_profile_input_card)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        // Upper bound: today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.setOnDismissListener { hideNavigationBar() }
        DialogImmersiveHelper.showImmersive(datePickerDialog)
    }

    private fun showGenderBottomSheet() {
        hideKeyboard()
        val bottomSheet = BottomSheetDialog(this, R.style.TransparentBottomSheetDialogTheme)
        val sheetBinding = BottomSheetSelectGenderBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        bottomSheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheet.behavior.skipCollapsed = true
        bottomSheet.setOnShowListener {
            DialogImmersiveHelper.setupBottomSheetImmersive(bottomSheet)
        }
        bottomSheet.setOnDismissListener {
            hideNavigationBar()
        }

        var tempGender = selectedGender.ifEmpty { "Male" }

        val genderOptions = listOf(
            Triple(sheetBinding.cardGenderMale, sheetBinding.tvGenderMale, sheetBinding.ivCheckMale) to "Male",
            Triple(sheetBinding.cardGenderFemale, sheetBinding.tvGenderFemale, sheetBinding.ivCheckFemale) to "Female",
            Triple(sheetBinding.cardGenderOther, sheetBinding.tvGenderOther, sheetBinding.ivCheckOther) to "Other"
        )

        fun updateGenderUI(selected: String) {
            tempGender = selected
            for ((views, name) in genderOptions) {
                val (card, tv, iv) = views
                val isSelected = (name == selected)
                if (isSelected) {
                    card.setBackgroundResource(R.drawable.bg_bottom_sheet_item_selected)
                    tv.setTextColor(ContextCompat.getColor(this, R.color.white))
                    iv.setImageResource(R.drawable.bg_checkbox_selected_white)
                } else {
                    card.setBackgroundResource(R.drawable.bg_bottom_sheet_item_unselected)
                    tv.setTextColor(ContextCompat.getColor(this, R.color.color_text))
                    iv.setImageResource(R.drawable.bg_checkbox_unchecked)
                }
            }
        }

        updateGenderUI(tempGender)

        for ((views, name) in genderOptions) {
            views.first.setOnClickListener {
                updateGenderUI(name)
            }
        }

        sheetBinding.btnClose.setOnClickListener {
            bottomSheet.dismiss()
        }

        sheetBinding.btnSaveGender.setOnClickListener {
            selectedGender = tempGender
            binding.tvGender.text = when (tempGender) {
                "Male" -> getString(R.string.gender_male)
                "Female" -> getString(R.string.gender_female)
                else -> getString(R.string.gender_other)
            }
            binding.cardGender.setBackgroundResource(R.drawable.bg_profile_input_card)
            bottomSheet.dismiss()
        }

        DialogImmersiveHelper.showImmersive(bottomSheet)
    }

    private fun showRelationshipBottomSheet() {
        hideKeyboard()
        val bottomSheet = BottomSheetDialog(this, R.style.TransparentBottomSheetDialogTheme)
        val sheetBinding = BottomSheetSelectRelationshipBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        bottomSheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheet.behavior.skipCollapsed = true
        bottomSheet.setOnShowListener {
            DialogImmersiveHelper.setupBottomSheetImmersive(bottomSheet)
        }
        bottomSheet.setOnDismissListener {
            hideNavigationBar()
        }

        var tempRelationship = selectedRelationship

        val relOptions = listOf(
            Triple(sheetBinding.cardRelFather, sheetBinding.tvRelFather, sheetBinding.ivCheckFather) to "Father",
            Triple(sheetBinding.cardRelMother, sheetBinding.tvRelMother, sheetBinding.ivCheckMother) to "Mother",
            Triple(sheetBinding.cardRelSpouse, sheetBinding.tvRelSpouse, sheetBinding.ivCheckSpouse) to "Spouse",
            Triple(sheetBinding.cardRelSon, sheetBinding.tvRelSon, sheetBinding.ivCheckSon) to "Son",
            Triple(sheetBinding.cardRelDaughter, sheetBinding.tvRelDaughter, sheetBinding.ivCheckDaughter) to "Daughter",
            Triple(sheetBinding.cardRelBrother, sheetBinding.tvRelBrother, sheetBinding.ivCheckBrother) to "Brother",
            Triple(sheetBinding.cardRelSister, sheetBinding.tvRelSister, sheetBinding.ivCheckSister) to "Sister",
            Triple(sheetBinding.cardRelGrandparent, sheetBinding.tvRelGrandparent, sheetBinding.ivCheckGrandparent) to "Grandparent",
            Triple(sheetBinding.cardRelOther, sheetBinding.tvRelOther, sheetBinding.ivCheckOther) to "Other"
        )

        fun updateRelationshipUI(selected: String) {
            tempRelationship = selected
            for ((views, name) in relOptions) {
                val (card, tv, iv) = views
                val isSelected = selected.isNotBlank() && (name.equals(selected, ignoreCase = true))
                if (isSelected) {
                    card.setBackgroundResource(R.drawable.bg_bottom_sheet_item_selected)
                    tv.setTextColor(ContextCompat.getColor(this, R.color.white))
                    iv.setImageResource(R.drawable.bg_checkbox_selected_white)
                } else {
                    card.setBackgroundResource(R.drawable.bg_bottom_sheet_item_unselected)
                    tv.setTextColor(ContextCompat.getColor(this, R.color.color_text))
                    iv.setImageResource(R.drawable.bg_checkbox_unchecked)
                }
            }
        }

        updateRelationshipUI(tempRelationship)

        for ((views, name) in relOptions) {
            views.first.setOnClickListener {
                updateRelationshipUI(name)
            }
        }

        sheetBinding.btnClose.setOnClickListener {
            bottomSheet.dismiss()
        }

        sheetBinding.btnSaveRelationship.setOnClickListener {
            if (tempRelationship.isNotBlank()) {
                selectedRelationship = tempRelationship
                binding.tvRelationship.text = com.example.healthcareai.util.ProfileLocalizationHelper.getLocalizedRelationship(this, tempRelationship)
                binding.cardRelationship.setBackgroundResource(R.drawable.bg_profile_input_card)
                bottomSheet.dismiss()
            } else {
                Toast.makeText(this, R.string.error_relationship_empty, Toast.LENGTH_SHORT).show()
            }
        }

        DialogImmersiveHelper.showImmersive(bottomSheet)
    }

    private fun handleCreateProfile() {
        val name = binding.etFullName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            binding.cardFullNameInput.setBackgroundResource(R.drawable.bg_profile_input_card_error)
            Toast.makeText(this, R.string.error_name_empty, Toast.LENGTH_SHORT).show()
            binding.etFullName.requestFocus()
            return
        }

        if (binding.layoutRelationship.visibility == View.VISIBLE && selectedRelationship.isBlank()) {
            binding.cardRelationship.setBackgroundResource(R.drawable.bg_profile_input_card_error)
            Toast.makeText(this, R.string.error_relationship_empty, Toast.LENGTH_SHORT).show()
            showRelationshipBottomSheet()
            return
        }

        if (selectedDob.isEmpty()) {
            binding.cardDateOfBirth.setBackgroundResource(R.drawable.bg_profile_input_card_error)
            Toast.makeText(this, R.string.error_dob_empty, Toast.LENGTH_SHORT).show()
            showDatePicker()
            return
        }

        if (selectedGender.isEmpty()) {
            binding.cardGender.setBackgroundResource(R.drawable.bg_profile_input_card_error)
            Toast.makeText(this, R.string.error_gender_empty, Toast.LENGTH_SHORT).show()
            showGenderBottomSheet()
            return
        }

        val heightStr = binding.etHeight.text?.toString()?.trim().orEmpty()
        val height = heightStr.toIntOrNull()
        if (height == null || height <= 0) {
            binding.cardHeightInput.setBackgroundResource(R.drawable.bg_profile_input_card_error)
            Toast.makeText(this, R.string.error_height_empty, Toast.LENGTH_SHORT).show()
            binding.etHeight.requestFocus()
            return
        }

        val weightStr = binding.etWeight.text?.toString()?.trim().orEmpty()
        val weight = weightStr.toIntOrNull()
        if (weight == null || weight <= 0) {
            binding.cardWeightInput.setBackgroundResource(R.drawable.bg_profile_input_card_error)
            Toast.makeText(this, R.string.error_weight_empty, Toast.LENGTH_SHORT).show()
            binding.etWeight.requestFocus()
            return
        }

        if (isEditMode && editingProfile != null) {
            val current = editingProfile!!
            val updated = current.copy(
                name = name,
                avatarUri = selectedAvatarUri?.toString(),
                dateOfBirth = selectedDob,
                gender = selectedGender,
                heightCm = height,
                weightKg = weight,
                relationship = if (!current.isPrimary) selectedRelationship.ifEmpty { null } else null
            )
            profileRepository.updateProfile(updated)
            Toast.makeText(this, R.string.msg_profile_updated, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val isFirstProfile = !preferenceHelper.isProfileCreated && !isAddMemberMode
        if (!isFirstProfile) {
            val profile = UserProfile(
                name = name,
                avatarUri = selectedAvatarUri?.toString(),
                dateOfBirth = selectedDob,
                gender = selectedGender,
                heightCm = height,
                weightKg = weight,
                isPrimary = false,
                relationship = selectedRelationship.ifEmpty { null }
            )
            profileRepository.addFamilyMemberProfile(profile)
            Toast.makeText(this, R.string.msg_member_added, Toast.LENGTH_SHORT).show()
            finish()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        finish()
    }

    companion object {
        const val EXTRA_IS_ADD_MEMBER = "extra_is_add_member"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        const val EXTRA_IS_EDIT = "extra_is_edit"

        fun start(context: Context, isAddMember: Boolean = false) {
            val intent = Intent(context, AddProfileActivity::class.java).apply {
                putExtra(EXTRA_IS_ADD_MEMBER, isAddMember)
            }
            context.startActivity(intent)
        }

        fun startEdit(context: Context, profileId: String) {
            val intent = Intent(context, AddProfileActivity::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
                putExtra(EXTRA_IS_EDIT, true)
            }
            context.startActivity(intent)
        }
    }
}
