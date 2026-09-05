package com.stabila.feature.history.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.stabila.core.ui.LocalAdaptiveParams
import com.stabila.core.domain.TestType
import com.stabila.core.domain.TremorReading
import com.stabila.core.ui.Amber400
import com.stabila.core.ui.Emerald500
import com.stabila.core.ui.Red500
import com.stabila.feature.history.HistoryViewModel
import com.stabila.feature.history.export.PdfExporter
import com.stabila.core.ui.components.StabilaPrimaryButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.res.stringResource
import com.stabila.core.R

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val filterType by viewModel.filterType.collectAsState()
    val readings by viewModel.readings.collectAsState()
    val adaptive = LocalAdaptiveParams.current
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfExporter = remember { PdfExporter(context) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = (28 * adaptive.fontScale).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(Modifier.height(16.dp))

        // Filters
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.width(16.dp)) }
            item {
                FilterChip(
                    text = stringResource(R.string.history_filter_all),
                    isSelected = filterType == null,
                    onClick = { viewModel.setFilter(null) }
                )
            }

            item {
                FilterChip(
                    text = stringResource(R.string.history_filter_action),
                    isSelected = filterType == TestType.ACTION,
                    onClick = { viewModel.setFilter(TestType.ACTION) }
                )
            }
            item {
                FilterChip(
                    text = stringResource(R.string.history_filter_spiral),
                    isSelected = filterType == TestType.SPIRAL,
                    onClick = { viewModel.setFilter(TestType.SPIRAL) }
                )
            }
            item { Spacer(Modifier.width(16.dp)) }
        }

        Spacer(Modifier.height(24.dp))

        if (readings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (16 * adaptive.fontScale).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                HistoryLineChart(readings = readings)
            }

            Spacer(Modifier.height(24.dp))

            // PDF Export Button
            StabilaPrimaryButton(
                text = if (isExporting) stringResource(R.string.history_generating_pdf) else stringResource(R.string.history_download_pdf),
                isLoading = isExporting,
                modifier = Modifier.padding(horizontal = 24.dp),
                onClick = {
                    if (isExporting || readings.isEmpty()) return@StabilaPrimaryButton
                    isExporting = true
                    scope.launch {
                        val file = pdfExporter.exportReadingsToPdf(readings)
                        isExporting = false
                        if (file != null) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.startActivity(Intent.createChooser(intent, "Open PDF"))
                        }
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // List
            Text(
                text = stringResource(R.string.history_recent_tests),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (16 * adaptive.fontScale).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(readings) { reading ->
                    HistoryItemRow(reading = reading)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val adaptive = LocalAdaptiveParams.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = adaptive.spacingUnit, vertical = adaptive.spacingUnit / 2)
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = (14 * adaptive.fontScale).sp
            )
        )
    }
}

@Composable
private fun HistoryItemRow(reading: TremorReading) {
    val adaptive = LocalAdaptiveParams.current
    val df = remember { SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()) }
    val testTypeName = if (reading.testType == TestType.ACTION) {
        stringResource(R.string.test_type_action)
    } else {
        stringResource(R.string.test_type_spiral)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(adaptive.spacingUnit),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = df.format(Date(reading.timestampEpochMs)),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = (12 * adaptive.fontScale).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.history_test_suffix, testTypeName),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (16 * adaptive.fontScale).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                val medicationTag = reading.medicationTag
                if (medicationTag != null) {
                    val medLabel = when (medicationTag) {
                        "pre-dose" -> stringResource(R.string.test_medication_pre)
                        "post-dose" -> stringResource(R.string.test_medication_post)
                        else -> medicationTag
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = medLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = (11 * adaptive.fontScale).sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Score circle
        val scoreColor = when {
            reading.score < 30f -> Emerald500
            reading.score < 65f -> Amber400
            else                -> Red500
        }
        
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size((40 * adaptive.fontScale).dp)
                .clip(CircleShape)
                .background(scoreColor.copy(alpha = 0.15f))
        ) {
            Text(
                text = reading.score.toInt().toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (16 * adaptive.fontScale).sp
                ),
                color = scoreColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
