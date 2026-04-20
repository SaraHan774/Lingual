package com.august.spiritscribe.ui.theme

import androidx.compose.ui.graphics.Color
import com.august.spiritscribe.domain.model.AppLanguage

fun languageBadgeColor(lang: AppLanguage): Color = when (lang) {
    AppLanguage.KOREAN -> Color(0xFF1565C0)
    AppLanguage.ENGLISH -> Color(0xFF2E7D32)
    AppLanguage.JAPANESE -> Color(0xFFC62828)
    AppLanguage.CHINESE -> Color(0xFFE65100)
}
