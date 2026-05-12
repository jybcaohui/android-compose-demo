package com.demo.creditlimit.ui.kyc

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun KycProgressIndicator(step: Int, totalSteps: Int) {
    LinearProgressIndicator(
        progress = { step.toFloat() / totalSteps.toFloat() },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary
    )
}
