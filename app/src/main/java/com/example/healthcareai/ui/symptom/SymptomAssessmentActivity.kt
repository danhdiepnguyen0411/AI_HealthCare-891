package com.example.healthcareai.ui.symptom

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import android.text.InputFilter
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthcareai.R
import com.example.healthcareai.databinding.ActivitySymptomAssessmentBinding
import com.example.healthcareai.ui.base.BaseActivity
import kotlin.math.abs

class SymptomAssessmentActivity : BaseActivity() {

    private lateinit var binding: ActivitySymptomAssessmentBinding

    enum class Gender {
        MALE, FEMALE
    }

    private var currentQuestionIndex = 1
    private var currentAge = 19
    private val minAge = 1
    private val maxAge = 120
    private var selectedGender: Gender = Gender.MALE

    companion object {
        const val TOTAL_QUESTIONS = 10
    }

    private lateinit var gestureDetector: GestureDetector
    private var accumulatedScrollY = 0f
    private val scrollThresholdDp = 32f

    // Question 3: Symptoms data & adapter
    private lateinit var symptomsAdapter: SymptomsAdapter
    private val allSymptoms = mutableListOf<SymptomItem>()
    private val selectedSymptomIds = mutableSetOf<String>()

    // Question 4: Related Symptoms data & adapter
    private lateinit var relatedSymptomsAdapter: SymptomsAdapter
    private val relatedSymptoms = mutableListOf<SymptomItem>()
    private val selectedRelatedSymptomIds = mutableSetOf<String>()

    // Question 5: Insect bite selection
    enum class InsectBiteOption {
        YES, NO, DONT_KNOW
    }
    private var selectedInsectBiteOption: InsectBiteOption = InsectBiteOption.NO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySymptomAssessmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackNavigation()
        initAgeWheel()
        initGenderSelection()
        initSymptomsList()
        initRelatedSymptoms()
        initInsectBiteSelection()
        setupListeners()

        // Display initial question 1 (1/10) without animation
        goToQuestion(1, animate = false)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootAssessmentLayout) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            if (statusBarTop > 0) {
                binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = statusBarTop + 8
                }
            }
            if (navBarBottom > 0) {
                binding.layoutBottomButtons.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = navBarBottom + 24
                }
            }
            insets
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentQuestionIndex > 1) {
                    goToQuestion(currentQuestionIndex - 1, animate = true)
                } else {
                    showExitConfirmationDialog()
                }
            }
        })
    }

    // =========================================================================
    // QUESTION 1: AGE PICKER WHEEL
    // =========================================================================
    private fun initAgeWheel() {
        updateAgeDisplay(currentAge, animate = false)

        val thresholdPx = scrollThresholdDp * resources.displayMetrics.density

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                accumulatedScrollY += distanceY
                if (abs(accumulatedScrollY) >= thresholdPx) {
                    val step = (accumulatedScrollY / thresholdPx).toInt()
                    if (step != 0) {
                        setAge(currentAge + step)
                        accumulatedScrollY %= thresholdPx
                    }
                }
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val flingStep = if (velocityY < 0) 2 else -2
                setAge(currentAge + flingStep)
                return true
            }
        })

        binding.wheelContainer.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                accumulatedScrollY = 0f
                view.performClick()
            }
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun setAge(newAge: Int) {
        val clamped = newAge.coerceIn(minAge, maxAge)
        if (clamped != currentAge) {
            currentAge = clamped
            binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            updateAgeDisplay(currentAge, animate = true)
        }
    }

    private fun updateAgeDisplay(age: Int, animate: Boolean) {
        binding.tvSelectedAge.text = age.toString()

        val minus1 = age - 1
        if (minus1 >= minAge) {
            binding.tvAgeMinus1.visibility = View.VISIBLE
            binding.tvAgeMinus1.text = minus1.toString()
        } else {
            binding.tvAgeMinus1.visibility = View.INVISIBLE
        }

        val minus2 = age - 2
        if (minus2 >= minAge) {
            binding.tvAgeMinus2.visibility = View.VISIBLE
            binding.tvAgeMinus2.text = minus2.toString()
        } else {
            binding.tvAgeMinus2.visibility = View.INVISIBLE
        }

        val plus1 = age + 1
        if (plus1 <= maxAge) {
            binding.tvAgePlus1.visibility = View.VISIBLE
            binding.tvAgePlus1.text = plus1.toString()
        } else {
            binding.tvAgePlus1.visibility = View.INVISIBLE
        }

        val plus2 = age + 2
        if (plus2 <= maxAge) {
            binding.tvAgePlus2.visibility = View.VISIBLE
            binding.tvAgePlus2.text = plus2.toString()
        } else {
            binding.tvAgePlus2.visibility = View.INVISIBLE
        }

        if (animate) {
            ObjectAnimator.ofFloat(binding.frameSelectedAge, View.SCALE_X, 0.95f, 1f).apply {
                duration = 160
                interpolator = OvershootInterpolator(1.2f)
                start()
            }
            ObjectAnimator.ofFloat(binding.frameSelectedAge, View.SCALE_Y, 0.95f, 1f).apply {
                duration = 160
                interpolator = OvershootInterpolator(1.2f)
                start()
            }
        }
    }

    // =========================================================================
    // QUESTION 2: GENDER SELECTION
    // =========================================================================
    private fun initGenderSelection() {
        applyGenderUi(selectedGender, animate = false)

        binding.frameCardMale.setOnClickListener {
            if (selectedGender != Gender.MALE) {
                selectedGender = Gender.MALE
                binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                applyGenderUi(selectedGender, animate = true)
            }
        }

        binding.frameCardFemale.setOnClickListener {
            if (selectedGender != Gender.FEMALE) {
                selectedGender = Gender.FEMALE
                binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                applyGenderUi(selectedGender, animate = true)
            }
        }
    }

    private fun applyGenderUi(gender: Gender, animate: Boolean) {
        if (gender == Gender.MALE) {
            binding.frameCardMale.setBackgroundResource(R.drawable.bg_card_male_selected)
            binding.frameCardFemale.setBackgroundResource(R.drawable.bg_card_female_unselected)
            if (animate) {
                animateCardSelection(binding.frameCardMale)
            }
        } else {
            binding.frameCardMale.setBackgroundResource(R.drawable.bg_card_male_unselected)
            binding.frameCardFemale.setBackgroundResource(R.drawable.bg_card_female_selected)
            if (animate) {
                animateCardSelection(binding.frameCardFemale)
            }
        }
    }

    private fun animateCardSelection(view: View) {
        ObjectAnimator.ofFloat(view, View.SCALE_X, 0.95f, 1f).apply {
            duration = 180
            interpolator = OvershootInterpolator(1.3f)
            start()
        }
        ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.95f, 1f).apply {
            duration = 180
            interpolator = OvershootInterpolator(1.3f)
            start()
        }
    }

    // =========================================================================
    // QUESTION 3: SYMPTOMS LIST & REAL-TIME SEARCH (Max 50 chars)
    // =========================================================================
    private fun initSymptomsList() {
        populateSymptomsData()

        symptomsAdapter = SymptomsAdapter(
            isItemSelected = { id -> selectedSymptomIds.contains(id) },
            onItemClick = { item ->
                if (selectedSymptomIds.contains(item.id)) {
                    selectedSymptomIds.remove(item.id)
                } else {
                    selectedSymptomIds.add(item.id)
                }
                updateSelectedChips()
                binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        )

        binding.rvSymptoms.apply {
            layoutManager = LinearLayoutManager(this@SymptomAssessmentActivity)
            adapter = symptomsAdapter
            setHasFixedSize(true)
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    updateScrollIndicator()
                }
            })
        }

        symptomsAdapter.submitList(allSymptoms)
        binding.rvSymptoms.post { updateScrollIndicator() }
        updateSelectedChips()

        // Strict 50-character limit & real-time smooth search
        binding.etSearchSymptoms.filters = arrayOf(InputFilter.LengthFilter(50))
        binding.etSearchSymptoms.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            binding.ivClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            filterSymptoms(query)
        }

        binding.ivClearSearch.setOnClickListener {
            binding.etSearchSymptoms.text?.clear()
        }
    }

    private fun updateScrollIndicator() {
        val recyclerView = binding.rvSymptoms
        val offset = recyclerView.computeVerticalScrollOffset()
        val extent = recyclerView.computeVerticalScrollExtent()
        val range = recyclerView.computeVerticalScrollRange()

        if (range <= extent) {
            binding.layoutScrollTrack.visibility = View.INVISIBLE
            return
        }

        binding.layoutScrollTrack.visibility = View.VISIBLE
        val trackHeight = binding.layoutScrollTrack.height
        if (trackHeight <= 0) {
            binding.layoutScrollTrack.post { updateScrollIndicator() }
            return
        }

        val dynamicThumbHeight = ((extent.toFloat() / range) * trackHeight)
            .toInt()
            .coerceIn((36 * resources.displayMetrics.density).toInt(), trackHeight / 2)

        binding.viewScrollThumb.updateLayoutParams<ViewGroup.LayoutParams> {
            height = dynamicThumbHeight
        }

        val maxScroll = (range - extent).coerceAtLeast(1)
        val maxTrack = (trackHeight - dynamicThumbHeight).coerceAtLeast(0)
        val thumbY = (offset.toFloat() / maxScroll * maxTrack).coerceIn(0f, maxTrack.toFloat())
        binding.viewScrollThumb.translationY = thumbY
    }

    private fun updateSelectedChips() {
        binding.layoutSelectedChips.removeAllViews()

        if (selectedSymptomIds.isEmpty()) {
            binding.layoutSelectedContainer.visibility = View.GONE
            return
        }

        binding.layoutSelectedContainer.visibility = View.VISIBLE

        selectedSymptomIds.forEach { id ->
            val symptom = allSymptoms.firstOrNull { it.id == id } ?: return@forEach
            val chipBinding = com.example.healthcareai.databinding.ItemSelectedChipBinding.inflate(
                layoutInflater,
                binding.layoutSelectedChips,
                true
            )
            chipBinding.tvChipName.text = symptom.name
            chipBinding.ivChipRemove.setOnClickListener {
                selectedSymptomIds.remove(symptom.id)
                updateSelectedChips()
                val pos = symptomsAdapter.currentList.indexOfFirst { it.id == symptom.id }
                if (pos >= 0) {
                    symptomsAdapter.notifyItemChanged(pos, SymptomsAdapter.PAYLOAD_SELECTION_CHANGED)
                }
            }
        }
    }

    private fun populateSymptomsData() {
        allSymptoms.clear()
        val rawList = listOf(
            // Matching Figma inspect screenshot (first 5 items exactly as shown)
            "Arm ache",
            "Erythema of upper arm",
            "Arm rash",
            "Arid tongue",
            "Achy left arm",

            // Upper Extremities & Shoulders
            "Achy right arm",
            "Arm numbness or tingling",
            "Arm muscle twitching",
            "Arm weakness",
            "Shoulder pain & stiffness",
            "Frozen shoulder",
            "Elbow joint pain",
            "Wrist pain & soreness",
            "Numbness in fingers",
            "Tingling sensation in hands",
            "Cold hands & pale fingers",
            "Hand tremor & shaking",
            "Swollen hands or fingers",

            // Head, Neck & Neurological
            "Headache",
            "Migraine",
            "Tension headache",
            "Cluster headache",
            "Dizziness & lightheadedness",
            "Vertigo (spinning sensation)",
            "Brain fog & mental fatigue",
            "Neck stiffness & pain",
            "Difficulty swallowing",
            "Hoarse or raspy voice",
            "Swollen lymph nodes in neck",

            // Respiratory & ENT
            "Sore throat",
            "Dry cough",
            "Productive / Chest cough",
            "Persistent coughing fit",
            "Shortness of breath (Dyspnea)",
            "Chest tightness & pressure",
            "Wheezing when breathing",
            "Sharp chest pain when inhaling",
            "Runny or congested nose",
            "Sinus pressure & pain",
            "Frequent sneezing",
            "Loss of smell (Anosmia)",
            "Loss of taste (Ageusia)",
            "Earache & ear fullness",
            "Tinnitus (Ringing in ears)",

            // Eyes & Vision
            "Red or bloodshot eyes",
            "Dry or burning eyes",
            "Watery or itchy eyes",
            "Blurred or distorted vision",
            "Sensitivity to light (Photophobia)",

            // Gastrointestinal & Digestive
            "Nausea & queasiness",
            "Vomiting",
            "Stomach cramps & spasms",
            "Acid reflux & heartburn",
            "Indigestion (Dyspepsia)",
            "Abdominal bloating & gas",
            "Upper abdominal pain",
            "Lower abdominal pain",
            "Loss of appetite",
            "Diarrhea",
            "Constipation",
            "Difficulty digesting food",

            // Back, Pelvis & Lower Extremities
            "Lower back pain (Lumbago)",
            "Upper back pain",
            "Sciatica (Shooting leg pain)",
            "Hip pain & stiffness",
            "Joint pain & stiffness",
            "Knee pain & swelling",
            "Calf muscle cramps",
            "Shin splints",
            "Ankle sprain & swelling",
            "Foot pain or heel ache",
            "Cold feet & toes",
            "Burning feet sensation",
            "Restless legs at night",

            // Skin & Dermatological
            "Skin rash & hives",
            "Skin redness & itching",
            "Dry, peeling skin",
            "Eczema flare-up",
            "Skin blisters or sores",
            "Bruising easily",
            "Excessive sweating (Hyperhidrosis)",
            "Night sweats",

            // General, Systemic & Sleep
            "High fever & chills",
            "Low-grade / Mild fever",
            "Persistent fatigue & exhaustion",
            "General weakness & malaise",
            "Dehydration & extreme thirst",
            "Unexplained weight loss",
            "Difficulty falling asleep (Insomnia)",
            "Waking up frequently during night",
            "Daytime drowsiness & lethargy",
            "Rapid or fluttering heartbeat",
            "Anxiety & nervousness",
            "Stress & mental burnout"
        )

        rawList.forEachIndexed { index, name ->
            allSymptoms.add(
                SymptomItem(
                    id = "symptom_$index",
                    name = name
                )
            )
        }
    }

    private fun filterSymptoms(query: String) {
        val cleanQuery = query.trim()
        val filtered = if (cleanQuery.isEmpty()) {
            allSymptoms
        } else {
            allSymptoms.filter {
                it.name.contains(cleanQuery, ignoreCase = true)
            }.sortedWith(
                compareByDescending<SymptomItem> {
                    it.name.startsWith(cleanQuery, ignoreCase = true)
                }.thenBy {
                    it.name.indexOf(cleanQuery, ignoreCase = true)
                }
            )
        }

        symptomsAdapter.submitList(filtered) {
            binding.rvSymptoms.scrollToPosition(0)
            binding.rvSymptoms.post { updateScrollIndicator() }
        }

        if (filtered.isEmpty()) {
            binding.tvEmptySearch.visibility = View.VISIBLE
            binding.tvEmptySearch.text = getString(R.string.no_symptoms_found, cleanQuery)
            binding.rvSymptoms.visibility = View.GONE
            binding.layoutScrollTrack.visibility = View.INVISIBLE
        } else {
            binding.tvEmptySearch.visibility = View.GONE
            binding.rvSymptoms.visibility = View.VISIBLE
        }
    }

    // =========================================================================
    // QUESTION 4: RELATED SYMPTOMS (Dynamically driven by Q3 selection)
    // =========================================================================
    private fun initRelatedSymptoms() {
        relatedSymptomsAdapter = SymptomsAdapter(
            isItemSelected = { id -> selectedRelatedSymptomIds.contains(id) },
            onItemClick = { item ->
                if (selectedRelatedSymptomIds.contains(item.id)) {
                    selectedRelatedSymptomIds.remove(item.id)
                } else {
                    selectedRelatedSymptomIds.add(item.id)
                }
                binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        )

        binding.rvRelatedSymptoms.apply {
            layoutManager = LinearLayoutManager(this@SymptomAssessmentActivity)
            adapter = relatedSymptomsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadRelatedSymptomsForQuestion4() {
        relatedSymptoms.clear()

        val selectedNames = allSymptoms
            .filter { selectedSymptomIds.contains(it.id) }
            .map { it.name.lowercase() }

        val relatedList = mutableListOf<String>()

        val hasArmSkin = selectedNames.any {
            it.contains("arm") || it.contains("erythema") || it.contains("rash") ||
            it.contains("skin") || it.contains("itch") || it.contains("limb") || it.contains("wound")
        } || selectedNames.isEmpty() // Default to Figma inspect screenshot items

        if (hasArmSkin) {
            // Exact Figma inspect screenshot items
            relatedList.add("Swelling")
            relatedList.add("Itching")
            relatedList.add("Skin pain")
            relatedList.add("Pain when pressing upper limb")
            relatedList.add("Skin wound")
        }

        val hasHeadNeuro = selectedNames.any {
            it.contains("head") || it.contains("migraine") || it.contains("dizz") ||
            it.contains("vertigo") || it.contains("fog") || it.contains("brain")
        }
        if (hasHeadNeuro) {
            relatedList.add("Sensitivity to light (Photophobia)")
            relatedList.add("Nausea or queasiness")
            relatedList.add("Throbbing or pulsing pain")
            relatedList.add("Dizziness or unsteady balance")
            relatedList.add("Neck stiffness & tension")
        }

        val hasRespEnt = selectedNames.any {
            it.contains("cough") || it.contains("throat") || it.contains("breath") ||
            it.contains("nose") || it.contains("sinus") || it.contains("ear")
        }
        if (hasRespEnt) {
            relatedList.add("Difficulty swallowing")
            relatedList.add("Fever or chills")
            relatedList.add("Runny or blocked nasal passage")
            relatedList.add("Chest tightness or congestion")
            relatedList.add("Phlegm or mucus coughing")
        }

        val hasDigestive = selectedNames.any {
            it.contains("stomach") || it.contains("nausea") || it.contains("reflux") ||
            it.contains("bloat") || it.contains("cramp") || it.contains("diarrhea")
        }
        if (hasDigestive) {
            relatedList.add("Acid reflux or heartburn")
            relatedList.add("Abdominal bloating & tightness")
            relatedList.add("Loss of appetite")
            relatedList.add("Vomiting or dry heaving")
            relatedList.add("Stomach cramps after eating")
        }

        val hasJointMusculo = selectedNames.any {
            it.contains("back") || it.contains("knee") || it.contains("joint") ||
            it.contains("hip") || it.contains("shoulder") || it.contains("muscle")
        }
        if (hasJointMusculo) {
            relatedList.add("Morning stiffness lasting > 30 min")
            relatedList.add("Joint swelling or tenderness")
            relatedList.add("Pain when walking or climbing stairs")
            relatedList.add("Reduced range of motion")
        }

        val distinctItems = relatedList.distinct()

        distinctItems.forEachIndexed { index, name ->
            relatedSymptoms.add(
                SymptomItem(
                    id = "related_$index",
                    name = name
                )
            )
        }

        relatedSymptomsAdapter.submitList(relatedSymptoms.toList())
    }

    // =========================================================================
    // QUESTION 5: INSECT BITE SELECTION (Yes, No, Don't know)
    // =========================================================================
    private fun initInsectBiteSelection() {
        applyInsectBiteUi(selectedInsectBiteOption, animate = false)

        binding.cardInsectYes.setOnClickListener {
            if (selectedInsectBiteOption != InsectBiteOption.YES) {
                selectedInsectBiteOption = InsectBiteOption.YES
                binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                applyInsectBiteUi(selectedInsectBiteOption, animate = true)
            }
        }

        binding.cardInsectNo.setOnClickListener {
            if (selectedInsectBiteOption != InsectBiteOption.NO) {
                selectedInsectBiteOption = InsectBiteOption.NO
                binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                applyInsectBiteUi(selectedInsectBiteOption, animate = true)
            }
        }

        binding.cardInsectDontKnow.setOnClickListener {
            if (selectedInsectBiteOption != InsectBiteOption.DONT_KNOW) {
                selectedInsectBiteOption = InsectBiteOption.DONT_KNOW
                binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                applyInsectBiteUi(selectedInsectBiteOption, animate = true)
            }
        }
    }

    private fun applyInsectBiteUi(option: InsectBiteOption, animate: Boolean) {
        val colorWhite = ContextCompat.getColor(this, R.color.white)
        val colorText = ContextCompat.getColor(this, R.color.color_text)

        // Reset all 3 options
        binding.cardInsectYes.setBackgroundResource(R.drawable.bg_symptom_card_unselected)
        binding.tvInsectYes.setTextColor(colorText)
        binding.ivInsectYesCheck.setImageResource(R.drawable.bg_checkbox_unchecked)

        binding.cardInsectNo.setBackgroundResource(R.drawable.bg_symptom_card_unselected)
        binding.tvInsectNo.setTextColor(colorText)
        binding.ivInsectNoCheck.setImageResource(R.drawable.bg_checkbox_unchecked)

        binding.cardInsectDontKnow.setBackgroundResource(R.drawable.bg_symptom_card_unselected)
        binding.tvInsectDontKnow.setTextColor(colorText)
        binding.ivInsectDontKnowCheck.setImageResource(R.drawable.bg_checkbox_unchecked)

        val targetCard = when (option) {
            InsectBiteOption.YES -> {
                binding.cardInsectYes.setBackgroundResource(R.drawable.bg_symptom_card_selected)
                binding.tvInsectYes.setTextColor(colorWhite)
                binding.ivInsectYesCheck.setImageResource(R.drawable.bg_checkbox_selected_white)
                binding.cardInsectYes
            }
            InsectBiteOption.NO -> {
                binding.cardInsectNo.setBackgroundResource(R.drawable.bg_symptom_card_selected)
                binding.tvInsectNo.setTextColor(colorWhite)
                binding.ivInsectNoCheck.setImageResource(R.drawable.bg_checkbox_selected_white)
                binding.cardInsectNo
            }
            InsectBiteOption.DONT_KNOW -> {
                binding.cardInsectDontKnow.setBackgroundResource(R.drawable.bg_symptom_card_selected)
                binding.tvInsectDontKnow.setTextColor(colorWhite)
                binding.ivInsectDontKnowCheck.setImageResource(R.drawable.bg_checkbox_selected_white)
                binding.cardInsectDontKnow
            }
        }

        if (animate) {
            ObjectAnimator.ofFloat(targetCard, View.SCALE_X, 0.97f, 1f).apply {
                duration = 180
                interpolator = OvershootInterpolator(1.2f)
                start()
            }
            ObjectAnimator.ofFloat(targetCard, View.SCALE_Y, 0.97f, 1f).apply {
                duration = 180
                interpolator = OvershootInterpolator(1.2f)
                start()
            }
        }
    }

    // =========================================================================
    // NAVIGATION & BOTTOM BUTTONS (Q1: 1 btn; Q2+: 2 btns)
    // =========================================================================
    private fun setupListeners() {
        // Top-left circular back button: shows exit confirmation popup
        binding.btnBack.setOnClickListener {
            showExitConfirmationDialog()
        }

        // Bottom secondary back button: goes to previous question
        binding.btnBottomBack.setOnClickListener {
            if (currentQuestionIndex > 1) {
                goToQuestion(currentQuestionIndex - 1, animate = true)
            } else {
                showExitConfirmationDialog()
            }
        }

        binding.tvAgeMinus2.setOnClickListener { setAge(currentAge - 2) }
        binding.tvAgeMinus1.setOnClickListener { setAge(currentAge - 1) }
        binding.tvAgePlus1.setOnClickListener { setAge(currentAge + 1) }
        binding.tvAgePlus2.setOnClickListener { setAge(currentAge + 2) }

        binding.btnContinue.setOnClickListener {
            when (currentQuestionIndex) {
                1 -> goToQuestion(2, animate = true)
                2 -> goToQuestion(3, animate = true)
                3 -> goToQuestion(4, animate = true)
                4 -> goToQuestion(5, animate = true)
                5 -> {
                    val label = when (selectedInsectBiteOption) {
                        InsectBiteOption.YES -> getString(R.string.option_yes)
                        InsectBiteOption.NO -> getString(R.string.option_no)
                        InsectBiteOption.DONT_KNOW -> getString(R.string.option_dont_know)
                    }
                    Toast.makeText(
                        this,
                        "Insect bite: $label. Proceeding to question 6...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun goToQuestion(index: Int, animate: Boolean) {
        val prevIndex = currentQuestionIndex
        currentQuestionIndex = index.coerceIn(1, TOTAL_QUESTIONS)

        // Update progress counter (e.g. "1/10", "2/10", "3/10", "4/10", "5/10")
        binding.tvProgressCount.text = getString(
            R.string.assessment_progress_format,
            currentQuestionIndex,
            TOTAL_QUESTIONS
        )

        // Update line progress bar
        val targetProgress = (currentQuestionIndex * 100) / TOTAL_QUESTIONS
        if (animate) {
            ObjectAnimator.ofInt(binding.progressBar, "progress", binding.progressBar.progress, targetProgress).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                start()
            }
        } else {
            binding.progressBar.progress = targetProgress
        }

        // Toggle bottom buttons (Q1: single full-width Continue; Q2+: Back + Continue side-by-side)
        val gapPx = (6 * resources.displayMetrics.density).toInt()
        if (currentQuestionIndex == 1) {
            binding.btnBottomBack.visibility = View.GONE
            binding.btnContinue.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = 0
            }
        } else {
            binding.btnBottomBack.visibility = View.VISIBLE
            binding.btnBottomBack.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd = gapPx
            }
            binding.btnContinue.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = gapPx
            }
        }

        // Toggle active question layout views
        when (currentQuestionIndex) {
            1 -> {
                binding.layoutAgeQuestion.visibility = View.VISIBLE
                binding.layoutGenderQuestion.visibility = View.GONE
                binding.layoutSymptomsQuestion.visibility = View.GONE
                binding.layoutRelatedSymptomsQuestion.visibility = View.GONE
                binding.layoutInsectBiteQuestion.visibility = View.GONE
            }
            2 -> {
                binding.layoutAgeQuestion.visibility = View.GONE
                binding.layoutGenderQuestion.visibility = View.VISIBLE
                binding.layoutSymptomsQuestion.visibility = View.GONE
                binding.layoutRelatedSymptomsQuestion.visibility = View.GONE
                binding.layoutInsectBiteQuestion.visibility = View.GONE
            }
            3 -> {
                binding.layoutAgeQuestion.visibility = View.GONE
                binding.layoutGenderQuestion.visibility = View.GONE
                binding.layoutSymptomsQuestion.visibility = View.VISIBLE
                binding.layoutRelatedSymptomsQuestion.visibility = View.GONE
                binding.layoutInsectBiteQuestion.visibility = View.GONE
            }
            4 -> {
                binding.layoutAgeQuestion.visibility = View.GONE
                binding.layoutGenderQuestion.visibility = View.GONE
                binding.layoutSymptomsQuestion.visibility = View.GONE
                binding.layoutRelatedSymptomsQuestion.visibility = View.VISIBLE
                binding.layoutInsectBiteQuestion.visibility = View.GONE
                loadRelatedSymptomsForQuestion4()
            }
            5 -> {
                binding.layoutAgeQuestion.visibility = View.GONE
                binding.layoutGenderQuestion.visibility = View.GONE
                binding.layoutSymptomsQuestion.visibility = View.GONE
                binding.layoutRelatedSymptomsQuestion.visibility = View.GONE
                binding.layoutInsectBiteQuestion.visibility = View.VISIBLE
            }
        }
    }

    private fun showExitConfirmationDialog() {
        val dialog = android.app.Dialog(this)
        val dialogBinding = com.example.healthcareai.databinding.DialogExitAssessmentBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)

        dialog.window?.let { window ->
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.88).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.setOnDismissListener {
            hideNavigationBar()
        }

        // Action: Continue Assessment (Stay)
        dialogBinding.btnContinueAssessment.setOnClickListener {
            dialog.dismiss()
        }

        // Action: Exit to Main Screen (Leave and return to SymptomCheckerActivity)
        dialogBinding.btnExitAssessment.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }
}
