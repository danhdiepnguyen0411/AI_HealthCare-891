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
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebView
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
        const val TOTAL_QUESTIONS = 6
    }

    private lateinit var gestureDetector: GestureDetector
    private var accumulatedScrollY = 0f
    private val scrollThresholdDp = 32f

    // Question 3: Body Anatomy selection
    private var selectedBodyRegionId: String = "arms"
    private var selectedBodyRegionName: String = "Arm"

    // Question 4: Specific symptoms selection
    private lateinit var symptomsAdapter: SymptomsAdapter
    private val allSymptoms = mutableListOf<SymptomItem>()
    private val selectedSymptomIds = mutableSetOf<String>()

    // Question 5: Related symptoms (dynamically filtered)
    private lateinit var q5SymptomsAdapter: SymptomsAdapter
    private val q5Symptoms = mutableListOf<SymptomItem>()
    private val selectedQ5SymptomIds = mutableSetOf<String>()
    private var lastLoadedQ5Category: String = ""

    // Question 6: Insect bite selection
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
        initBodyAnatomyWebView()
        initQuestion4Symptoms()
        initQuestion5Symptoms()
        initInsectBiteSelection()
        setupListeners()

        // Display initial question 1 (1/6) without animation
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
    // QUESTION 3: INTERACTIVE BODY ANATOMY MODEL (SCREENSHOT 1)
    // =========================================================================
    private fun initBodyAnatomyWebView() {
        binding.webViewBodyAnatomy.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                useWideViewPort = true
                loadWithOverviewMode = true
                displayZoomControls = false
                builtInZoomControls = false
                setSupportZoom(false)
            }
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            addJavascriptInterface(object : Any() {
                @JavascriptInterface
                fun onRegionSelected(regionId: String, regionName: String) {
                    runOnUiThread {
                        handleBodyPartSelected(regionId, regionName)
                    }
                }
            }, "AndroidBridge")
            loadUrl("file:///android_asset/body_anatomy.html")
        }

        binding.etBodyPartSearch.filters = arrayOf(InputFilter.LengthFilter(50))
        binding.etBodyPartSearch.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty().trim()
            binding.ivClearBodyPartSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            highlightBodyPartFromSearch(query)
        }

        binding.ivClearBodyPartSearch.setOnClickListener {
            binding.etBodyPartSearch.text?.clear()
            selectedBodyRegionId = ""
            selectedBodyRegionName = ""
            binding.webViewBodyAnatomy.evaluateJavascript("clearAllHighlights();", null)
        }
    }

    private fun handleBodyPartSelected(regionId: String, regionName: String) {
        selectedBodyRegionId = regionId
        selectedBodyRegionName = if (regionName.isNotEmpty()) regionName else "Arm"
        binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

        if (regionName.isNotEmpty()) {
            binding.etBodyPartSearch.setText(regionName)
            binding.etBodyPartSearch.setSelection(regionName.length)
        } else {
            binding.etBodyPartSearch.text?.clear()
        }
    }

    private fun highlightBodyPartFromSearch(query: String) {
        if (query.isEmpty()) return
        val lower = query.lowercase()
        val targetRegion = when {
            lower.contains("arm") || lower.contains("hand") || lower.contains("wrist") || lower.contains("elbow") || lower.contains("finger") || lower.contains("tay") -> "arms" to "Arm"
            lower.contains("head") || lower.contains("face") || lower.contains("eye") || lower.contains("ear") || lower.contains("đầu") -> "head" to "Head"
            lower.contains("neck") || lower.contains("throat") || lower.contains("cổ") -> "neck" to "Neck"
            lower.contains("shoulder") || lower.contains("vai") -> "shoulders" to "Shoulders"
            lower.contains("chest") || lower.contains("breast") || lower.contains("ngực") -> "chest" to "Chest"
            lower.contains("abdom") || lower.contains("stomach") || lower.contains("belly") || lower.contains("bụng") -> "abdomen" to "Abdomen"
            lower.contains("leg") || lower.contains("knee") || lower.contains("foot") || lower.contains("ankle") || lower.contains("chân") -> "legs" to "Legs"
            else -> null
        }

        targetRegion?.let { (rId, rName) ->
            selectedBodyRegionId = rId
            selectedBodyRegionName = rName
            binding.webViewBodyAnatomy.evaluateJavascript("highlightRegion('$rId', '$rName', true);", null)
        }
    }

    // =========================================================================
    // QUESTION 4: SPECIFIC SYMPTOMS LIST (SCREENSHOT 2)
    // =========================================================================
    private fun initQuestion4Symptoms() {
        populateSymptomsData()

        symptomsAdapter = SymptomsAdapter(
            isItemSelected = { id -> selectedSymptomIds.contains(id) },
            onItemClick = { item ->
                toggleQ4SymptomSelection(item)
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

    private fun prepareQuestion4View() {
        val regionName = if (selectedBodyRegionName.isNotEmpty()) selectedBodyRegionName else "Arm"
        binding.etSearchSymptoms.setText(regionName)
        binding.etSearchSymptoms.setSelection(regionName.length)

        // Seed with "Erythema of upper arm" (symptom_1) if nothing selected yet to match Screenshot 2
        if (selectedSymptomIds.isEmpty()) {
            selectedSymptomIds.add("symptom_1")
        }
        updateSelectedChips()
        filterSymptoms(regionName)
    }

    private fun toggleQ4SymptomSelection(item: SymptomItem) {
        if (selectedSymptomIds.contains(item.id)) {
            selectedSymptomIds.remove(item.id)
        } else {
            selectedSymptomIds.add(item.id)
        }
        updateSelectedChips()
        binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

        val pos = symptomsAdapter.currentList.indexOfFirst { it.id == item.id }
        if (pos >= 0) {
            symptomsAdapter.notifyItemChanged(pos, SymptomsAdapter.PAYLOAD_SELECTION_CHANGED)
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
        } else if (cleanQuery.equals("Arm", ignoreCase = true)) {
            // First 5 items exactly match Screenshot 2, followed by remaining arm items
            allSymptoms.filter { item ->
                item.name in listOf("Arm ache", "Erythema of upper arm", "Arm rash", "Arid tongue", "Achy left arm") ||
                item.name.contains("arm", ignoreCase = true)
            }.sortedBy { item ->
                val priorityOrder = listOf("Arm ache", "Erythema of upper arm", "Arm rash", "Arid tongue", "Achy left arm")
                val idx = priorityOrder.indexOf(item.name)
                if (idx >= 0) idx else 100
            }
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
    // QUESTION 5: RELATED SYMPTOMS (DYNAMICALLY FILTERED)
    // =========================================================================
    private fun initQuestion5Symptoms() {
        q5SymptomsAdapter = SymptomsAdapter(
            isItemSelected = { id -> selectedQ5SymptomIds.contains(id) },
            onItemClick = { item ->
                if (selectedQ5SymptomIds.contains(item.id)) {
                    selectedQ5SymptomIds.remove(item.id)
                } else {
                    selectedQ5SymptomIds.add(item.id)
                }
                binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        )

        binding.rvQ5Symptoms.apply {
            layoutManager = LinearLayoutManager(this@SymptomAssessmentActivity)
            adapter = q5SymptomsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadRelatedSymptomsForQuestion5() {
        val selectedNames = selectedSymptomIds.mapNotNull { id ->
            allSymptoms.firstOrNull { it.id == id }?.name
        }
        val combinedText = (selectedNames.joinToString(" ") + " " + selectedBodyRegionName).lowercase()

        val category = when {
            combinedText.contains("head") || combinedText.contains("migraine") || combinedText.contains("dizzy") ||
            combinedText.contains("vertigo") || combinedText.contains("brain") || combinedText.contains("light") ||
            combinedText.contains("ear") || combinedText.contains("throat") || combinedText.contains("swallow") ||
            selectedBodyRegionName.equals("Head", ignoreCase = true) || selectedBodyRegionName.equals("Neck", ignoreCase = true) -> "HEAD"

            combinedText.contains("chest") || combinedText.contains("breath") || combinedText.contains("cough") ||
            combinedText.contains("heart") || combinedText.contains("wheez") || combinedText.contains("lung") ||
            combinedText.contains("palpitation") || selectedBodyRegionName.equals("Chest", ignoreCase = true) -> "CHEST"

            combinedText.contains("stomach") || combinedText.contains("abdomen") || combinedText.contains("acid") ||
            combinedText.contains("reflux") || combinedText.contains("nausea") || combinedText.contains("vomit") ||
            combinedText.contains("cramp") || combinedText.contains("bloat") || combinedText.contains("diarrhea") ||
            combinedText.contains("digest") || selectedBodyRegionName.equals("Abdomen", ignoreCase = true) -> "ABDOMEN"

            combinedText.contains("leg") || combinedText.contains("knee") || combinedText.contains("ankle") ||
            combinedText.contains("foot") || combinedText.contains("calf") || combinedText.contains("toe") ||
            combinedText.contains("sciatica") || selectedBodyRegionName.equals("Legs", ignoreCase = true) -> "LEGS"

            else -> "ARM_SKIN" // Default matches Arm / Skin / Allergy (Screenshot 3)
        }

        if (category == lastLoadedQ5Category && q5Symptoms.isNotEmpty()) {
            q5SymptomsAdapter.notifyDataSetChanged()
            return
        }

        lastLoadedQ5Category = category
        selectedQ5SymptomIds.clear()
        q5Symptoms.clear()

        val rawList = when (category) {
            "HEAD" -> listOf(
                "Sensitivity to light (Photophobia)",
                "Nausea or vomiting",
                "Throbbing or pulsing pain",
                "Dizziness or lightheadedness",
                "Neck stiffness or pain",
                "Difficulty concentrating (Brain fog)"
            )
            "CHEST" -> listOf(
                "Shortness of breath (Dyspnea)",
                "Chest tightness or pressure",
                "Wheezing when breathing",
                "Rapid or fluttering heartbeat",
                "Sharp chest pain when inhaling",
                "Persistent coughing or mucus"
            )
            "ABDOMEN" -> listOf(
                "Stomach cramps or burning sensation",
                "Nausea or vomiting",
                "Abdominal bloating or gas",
                "Acid reflux or heartburn",
                "Loss of appetite",
                "Diarrhea or indigestion"
            )
            "LEGS" -> listOf(
                "Swelling in legs or ankles",
                "Sharp pain when walking or climbing stairs",
                "Joint stiffness in the morning",
                "Calf muscle cramps or tightness",
                "Numbness or tingling sensation",
                "Shooting pain down the leg (Sciatica)"
            )
            else -> listOf(
                "Swelling",
                "Itching",
                "Skin pain",
                "Pain when pressing upper limb",
                "Skin wound",
                "Contact with allergen"
            )
        }

        rawList.forEachIndexed { index, name ->
            val id = "q5_${category}_$index"
            q5Symptoms.add(SymptomItem(id, name))
        }

        // For default ARM_SKIN category, pre-select "Itching" (item index 1) to match Screenshot 3
        if (category == "ARM_SKIN") {
            selectedQ5SymptomIds.add("q5_ARM_SKIN_1")
        }

        q5SymptomsAdapter.submitList(q5Symptoms.toList()) {
            binding.rvQ5Symptoms.scrollToPosition(0)
        }
    }

    // =========================================================================
    // QUESTION 6: INSECT BITE SELECTION (Yes, No, Don't know)
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
                3 -> {
                    if (selectedBodyRegionName.isEmpty()) {
                        selectedBodyRegionName = "Arm"
                    }
                    goToQuestion(4, animate = true)
                }
                4 -> goToQuestion(5, animate = true)
                5 -> goToQuestion(6, animate = true)
                6 -> {
                    val label = when (selectedInsectBiteOption) {
                        InsectBiteOption.YES -> getString(R.string.option_yes)
                        InsectBiteOption.NO -> getString(R.string.option_no)
                        InsectBiteOption.DONT_KNOW -> getString(R.string.option_dont_know)
                    }
                    Toast.makeText(
                        this,
                        "Assessment complete! Results are ready.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun goToQuestion(index: Int, animate: Boolean) {
        val prevIndex = currentQuestionIndex
        currentQuestionIndex = index.coerceIn(1, TOTAL_QUESTIONS)

        // Update progress counter (e.g. "1/6", "2/6", etc.)
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
                binding.layoutBodyPartQuestion.visibility = View.GONE
                binding.layoutSymptomsQuestion.visibility = View.GONE
                binding.layoutQuestion5.visibility = View.GONE
                binding.layoutInsectBiteQuestion.visibility = View.GONE
            }
            2 -> {
                binding.layoutAgeQuestion.visibility = View.GONE
                binding.layoutGenderQuestion.visibility = View.VISIBLE
                binding.layoutBodyPartQuestion.visibility = View.GONE
                binding.layoutSymptomsQuestion.visibility = View.GONE
                binding.layoutQuestion5.visibility = View.GONE
                binding.layoutInsectBiteQuestion.visibility = View.GONE
            }
            3 -> {
                binding.layoutAgeQuestion.visibility = View.GONE
                binding.layoutGenderQuestion.visibility = View.GONE
                binding.layoutBodyPartQuestion.visibility = View.VISIBLE
                binding.layoutSymptomsQuestion.visibility = View.GONE
                binding.layoutQuestion5.visibility = View.GONE
                binding.layoutInsectBiteQuestion.visibility = View.GONE
            }
            4 -> {
                binding.layoutAgeQuestion.visibility = View.GONE
                binding.layoutGenderQuestion.visibility = View.GONE
                binding.layoutBodyPartQuestion.visibility = View.GONE
                binding.layoutSymptomsQuestion.visibility = View.VISIBLE
                binding.layoutQuestion5.visibility = View.GONE
                binding.layoutInsectBiteQuestion.visibility = View.GONE
                prepareQuestion4View()
            }
            5 -> {
                binding.layoutAgeQuestion.visibility = View.GONE
                binding.layoutGenderQuestion.visibility = View.GONE
                binding.layoutBodyPartQuestion.visibility = View.GONE
                binding.layoutSymptomsQuestion.visibility = View.GONE
                binding.layoutQuestion5.visibility = View.VISIBLE
                binding.layoutInsectBiteQuestion.visibility = View.GONE
                loadRelatedSymptomsForQuestion5()
            }
            6 -> {
                binding.layoutAgeQuestion.visibility = View.GONE
                binding.layoutGenderQuestion.visibility = View.GONE
                binding.layoutBodyPartQuestion.visibility = View.GONE
                binding.layoutSymptomsQuestion.visibility = View.GONE
                binding.layoutQuestion5.visibility = View.GONE
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
