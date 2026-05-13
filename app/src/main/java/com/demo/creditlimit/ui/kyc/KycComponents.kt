package com.demo.creditlimit.ui.kyc

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.creditlimit.R
import com.demo.creditlimit.network.model.request2.ConfigResp

// ── Shared colors ───────────────────────────────────────────────────────────

internal val KycBlue = Color(0xFF1B7FE8)
internal val KycBg = Color(0xFFF5F5F5)
internal val KycCard = Color(0xFFFFFFFF)
internal val KycLabel = Color(0xFF9E9E9E)
internal val KycText = Color(0xFF212121)
internal val KycDivider = Color(0xFFEEEEEE)
internal val KycDisabled = Color(0xFFB0BEC5)

// ── Top bar ─────────────────────────────────────────────────────────────────

@Composable
internal fun KycTopBar(title: String, onBack: () -> Unit, showService: Boolean = true) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(KycCard)
            .padding(top = 44.dp, bottom = 12.dp, start = 4.dp, end = 16.dp)
            .height(44.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.back),
                contentDescription = "Back",
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = KycText)
        )
        if (showService) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0))
                    .clickable {
                        com.demo.creditlimit.ui.webview.CommonWebActivity.start(
                            context,
                            "Contact Us",
                            com.demo.creditlimit.network.manager.EnvironmentManager.getContactUsUrl()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.service),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// ── Picker row (click to open sheet) ───────────────────────────────────────

@Composable
internal fun KycPickerRow(
    label: String,
    value: String?,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, style = TextStyle(fontSize = 12.sp, color = KycLabel))
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value ?: "Please Select",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = if (value != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (value != null) KycText else KycLabel
                ),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = KycLabel,
                modifier = Modifier.size(14.dp)
            )
        }
    }
    if (!isLast) HorizontalDivider(color = KycDivider)
}

// ── Input row (inline BasicTextField) ──────────────────────────────────────

@Composable
internal fun KycInputRow(
    label: String,
    value: String,
    placeholder: String = "Please Enter",
    isLast: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = TextStyle(fontSize = 12.sp, color = KycLabel))
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KycText),
                cursorBrush = SolidColor(KycBlue),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, style = TextStyle(fontSize = 14.sp, color = KycLabel))
                    }
                    inner()
                }
            )
            if (trailingContent != null) {
                Spacer(Modifier.width(8.dp))
                trailingContent()
            }
        }
    }
    if (!isLast) HorizontalDivider(color = KycDivider)
}

// ── Readonly display row ────────────────────────────────────────────────────

@Composable
internal fun KycReadonlyRow(
    label: String,
    value: String?,
    isLast: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = TextStyle(fontSize = 12.sp, color = KycLabel))
        Spacer(Modifier.height(4.dp))
        Text(
            text = value ?: "—",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = if (value != null) FontWeight.Bold else FontWeight.Normal,
                color = if (value != null) KycText else KycLabel
            )
        )
    }
    if (!isLast) HorizontalDivider(color = KycDivider)
}

// ── Sheet header ────────────────────────────────────────────────────────────

@Composable
internal fun KycSheetHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = KycText))
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = KycText)
        }
    }
}

// ── Single-select sheet ─────────────────────────────────────────────────────

@Composable
internal fun KycSingleSelectSheet(
    items: List<ConfigResp>,
    selectedEnum: Long?,
    onSelect: (ConfigResp) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        KycSheetHeader(title = "Please select", onClose = onClose)
        HorizontalDivider(color = KycDivider)
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.height(360.dp)
        ) {
            items(items) { cfg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(cfg) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = cfg.eneum == selectedEnum,
                        onClick = { onSelect(cfg) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = KycBlue,
                            unselectedColor = KycLabel
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = cfg.item, style = TextStyle(fontSize = 15.sp, color = KycText))
                }
                HorizontalDivider(color = KycDivider, thickness = 0.5.dp)
            }
        }
    }
}

// ── Next step button ────────────────────────────────────────────────────────

@Composable
internal fun KycNextButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(if (enabled) KycBlue else KycDisabled)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Next Step",
                color = Color.White,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

// ── Legacy progress indicator (kept for existing usage) ────────────────────

@Composable
fun KycProgressIndicator(step: Int, totalSteps: Int) {
    LinearProgressIndicator(
        progress = { step.toFloat() / totalSteps.toFloat() },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary
    )
}
