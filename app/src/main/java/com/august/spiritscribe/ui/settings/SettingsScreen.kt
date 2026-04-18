package com.august.spiritscribe.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("설정") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("번역 엔진")
            Text(
                text = "현재 엔진: Google ML Kit (온디바이스, 오프라인)",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "번역 모델은 최초 사용 시 자동으로 다운로드됩니다 (언어당 약 30MB).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            SectionTitle("TTS")
            Text(
                text = "Android 시스템의 TextToSpeech를 사용합니다. 지원 언어는 기기 설정에 따릅니다.",
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()

            SectionTitle("앱 정보")
            Text(text = "Lingual · v0.1 MVP", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "한국어, English, 日本語, 中文 간 일기 번역 및 학습 앱",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
