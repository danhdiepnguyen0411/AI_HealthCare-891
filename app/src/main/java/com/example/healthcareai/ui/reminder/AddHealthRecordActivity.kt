package com.example.healthcareai.ui.reminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.healthcareai.ui.base.BaseActivity

class AddHealthRecordActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Medication"

        val targetIntent = getIntent(this, category)
        targetIntent.putExtras(intent)
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        startActivity(targetIntent)
        finish()
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_RECORD_ID = "extra_record_id"
        const val EXTRA_RECORD_NAME = "extra_record_name"

        fun getIntent(context: Context, category: String): Intent {
            return when (category.trim().lowercase()) {
                "allergies", "allergy" -> Intent(context, AddAllergyActivity::class.java)
                "conditions", "condition" -> Intent(context, AddConditionActivity::class.java)
                "vaccination", "vaccinations" -> Intent(context, AddVaccinationActivity::class.java)
                else -> Intent(context, AddMedicationActivity::class.java)
            }
        }
    }
}
