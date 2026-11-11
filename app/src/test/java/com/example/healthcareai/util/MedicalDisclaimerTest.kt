package com.example.healthcareai.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MedicalDisclaimerTest {

    @Test
    fun testMedicalDisclaimerStringsExistInAllValuesFolders() {
        val resDir = File("src/main/res")
        val valuesDirs = resDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("values")
        } ?: emptyArray()

        assertTrue("Expected multiple values directories", valuesDirs.isNotEmpty())

        val requiredKeys = listOf(
            "medical_disclaimer_dialog_title",
            "dialog_medical_disclaimer_desc",
            "dialog_medical_disclaimer_btn"
        )

        for (dir in valuesDirs) {
            val stringsFile = File(dir, "strings.xml")
            if (stringsFile.exists()) {
                val content = stringsFile.readText()
                for (key in requiredKeys) {
                    assertTrue(
                        "File ${stringsFile.path} must contain key $key",
                        content.contains("name=\"$key\"")
                    )
                }
            }
        }
    }
}

