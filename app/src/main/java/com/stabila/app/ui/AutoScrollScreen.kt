package com.stabila.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.stabila.app.R
import com.stabila.app.ui.components.AppPickerDialog
import com.stabila.core.ui.LocalAdaptiveParams

@Composable
fun AutoScrollScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val adaptive = LocalAdaptiveParams.current
    val context = LocalContext.current
    val isMasterEnabled by viewModel.isAutoScrollMasterEnabled.collectAsState(initial = true)
    val scrollSpeed by viewModel.autoScrollSpeed.collectAsState(initial = 3f)
    val enabledApps by viewModel.enabledScrollApps.collectAsState(initial = emptySet())
    val isGlobalEnabled = enabledApps.contains("all")
    var showAppPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = adaptive.spacingUnit * 2, top = adaptive.spacingUnit)
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.padding(end = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.generic_back))
            }
            Text(
                text = stringResource(R.string.autoscroll_setup_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = (24 * adaptive.fontScale).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        // Feature Master Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isMasterEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .clickable { viewModel.setAutoScrollMasterEnabled(!isMasterEnabled) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = if (isMasterEnabled) stringResource(R.string.autoscroll_is_on) else stringResource(R.string.autoscroll_is_off),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = (16 * adaptive.fontScale).sp),
                    color = if (isMasterEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.autoscroll_master_desc),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * adaptive.fontScale).sp),
                    color = if (isMasterEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isMasterEnabled,
                onCheckedChange = { viewModel.setAutoScrollMasterEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        AnimatedVisibility(visible = isMasterEnabled) {
            Column {
                // Accessibility Permission
                Text(
                    text = stringResource(R.string.autoscroll_permissions_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (16 * adaptive.fontScale).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = adaptive.spacingUnit)) {
                Text(stringResource(R.string.autoscroll_enable_service_title), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = (16 * adaptive.fontScale).sp)
                Text(stringResource(R.string.autoscroll_enable_service_desc), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * adaptive.fontScale).sp))
                Text(stringResource(R.string.autoscroll_restricted_desc), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * adaptive.fontScale).sp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))
                    context.startActivity(intent)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = adaptive.spacingUnit)) {
                Text(stringResource(R.string.autoscroll_overlay_title), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = (16 * adaptive.fontScale).sp)
                Text(stringResource(R.string.autoscroll_overlay_desc), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * adaptive.fontScale).sp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Speed settings
        Text(
            text = stringResource(R.string.autoscroll_speed_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (16 * adaptive.fontScale).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.autoscroll_speed_desc),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = (14 * adaptive.fontScale).sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = adaptive.spacingUnit)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..5).forEach { speed ->
                val isSelected = scrollSpeed == speed.toFloat()
                Button(
                    onClick = { viewModel.setAutoScrollSpeed(speed.toFloat()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(adaptive.buttonHeight)
                ) {
                    Text(speed.toString(), fontWeight = FontWeight.Bold, fontSize = (14 * adaptive.fontScale).sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Target Apps Selection
        Text(
            text = stringResource(R.string.autoscroll_target_apps_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (16 * adaptive.fontScale).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = adaptive.spacingUnit)) {
                Text(
                    text = stringResource(R.string.autoscroll_enable_globally_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = (16 * adaptive.fontScale).sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.autoscroll_enable_globally_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * adaptive.fontScale).sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isGlobalEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        viewModel.setEnabledScrollApps(setOf("all"))
                    } else {
                        viewModel.setEnabledScrollApps(emptySet())
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        AnimatedVisibility(visible = !isGlobalEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showAppPicker = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(start = adaptive.spacingUnit)) {
                    val count = if (enabledApps.isEmpty()) stringResource(R.string.autoscroll_none) else stringResource(R.string.autoscroll_selected_count, enabledApps.size)
                    Text(stringResource(R.string.autoscroll_select_specific_apps), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = (16 * adaptive.fontScale).sp)
                    Text(count, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * adaptive.fontScale).sp))
                }
            }
        }

        if (showAppPicker) {
            AppPickerDialog(
                initialSelectedApps = enabledApps,
                onDismissRequest = { showAppPicker = false },
                onAppsSelected = { apps ->
                    viewModel.setEnabledScrollApps(apps)
                    showAppPicker = false
                }
            )
        }
            }
        }
    }
}
