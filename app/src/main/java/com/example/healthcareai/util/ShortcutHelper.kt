package com.example.healthcareai.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.healthcareai.R
import com.example.healthcareai.ui.splash.SplashActivity

object ShortcutHelper {

    const val ACTION_UNINSTALL_SHORTCUT = "com.example.healthcareai.ACTION_UNINSTALL"

    fun getShortcutTitle(langCode: String): String {
        return when (langCode.lowercase()) {
            "vi" -> "Gỡ cài đặt"
            "es" -> "Desinstalar"
            "fr" -> "Désinstaller"
            "de" -> "Deinstallieren"
            "pt", "pt_br", "pt_pt" -> "Desinstalar"
            "ru" -> "Удалить"
            "ja" -> "アンインストール"
            "ko" -> "삭제하기"
            "zh_cn", "zh" -> "卸载"
            "zh_tw", "zh_hk" -> "解除安裝"
            "ar" -> "إلغاء التثبيت"
            "hi" -> "अनइंस्टॉल करें"
            "bn" -> "আনইনস্টল"
            "id", "in" -> "Copot Pemasangan"
            "tr" -> "Kaldır"
            else -> "Uninstall"
        }
    }

    /**
     * Registers or refreshes the dynamic shortcut with the user's localized language.
     * Call this in Application onCreate, SplashActivity, and whenever language changes.
     */
    fun setupUninstallShortcut(context: Context, langCode: String? = null) {
        try {
            val code = langCode ?: LocaleHelper.getSavedLanguageCode(context)
            val title = getShortcutTitle(code)

            val intent = Intent(context, SplashActivity::class.java).apply {
                action = ACTION_UNINSTALL_SHORTCUT
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra("is_for_uninstall", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val shortcut = ShortcutInfoCompat.Builder(context, "action_uninstall")
                .setShortLabel(title)
                .setLongLabel(title)
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_delete_uninstall))
                .setIntent(intent)
                .build()

            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            ShortcutManagerCompat.setDynamicShortcuts(context, listOf(shortcut))
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        } catch (e: Throwable) {
            Log.e("ShortcutHelper", "Failed to setup uninstall shortcut", e)
        }
    }
}
