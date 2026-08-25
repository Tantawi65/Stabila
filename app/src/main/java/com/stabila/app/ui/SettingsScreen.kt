package com.stabila.app.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Calendar

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.stabila.app.ui.components.AppPickerDialog
import com.stabila.core.ui.LocalAdaptiveParams
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val reminderTime by viewModel.reminderTime.collectAsState()
    val themePreference by viewModel.themePreference.collectAsState(initial = "SYSTEM")
    val context = LocalContext.current
    val adaptive = LocalAdaptiveParams.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                viewModel.setNotifications(true)
            } else {
                viewModel.setNotifications(false)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = (32 * adaptive.fontScale).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = adaptive.spacingUnit * 2, top = adaptive.spacingUnit)
        )

        // Theme Selection
        Text(
            text = "App Theme",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            val options = listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark")
            options.forEach { (value, label) ->
                val isSelected = themePreference == value
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.setThemePreference(value) },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }


        SettingsToggle(
            title = "Daily Reminders",
            description = "Receive notifications to take your daily tremor test.",
            checked = notifications,
            onCheckedChange = { checked ->
                if (checked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setNotifications(true)
                        }
                    } else {
                        viewModel.setNotifications(true)
                    }
                } else {
                    viewModel.setNotifications(false)
                }
            }
        )

        AnimatedVisibility(visible = notifications) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val amPm = if (hourOfDay < 12) "AM" else "PM"
                                val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                                val min = String.format("%02d", minute)
                                viewModel.setReminderTime("$hour:$min $amPm")
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Reminder Time", color = MaterialTheme.colorScheme.onBackground, fontSize = (16 * adaptive.fontScale).sp)
                Text(reminderTime, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = (16 * adaptive.fontScale).sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Accessibility & Auto-Scroll
        Text(
            text = "Accessibility",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = (22 * adaptive.fontScale).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = adaptive.spacingUnit)
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
            Icon(Icons.Default.Keyboard, contentDescription = "Accessibility", tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = adaptive.spacingUnit)) {
                Text("Enable Auto-Scroll Service", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = (16 * adaptive.fontScale).sp)
                Text("Android 13+: First go to App Info -> 'Allow Restricted Settings'", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * adaptive.fontScale).sp))
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
            Icon(Icons.Default.Keyboard, contentDescription = "Overlay", tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = adaptive.spacingUnit)) {
                Text("Allow Display Over Other Apps", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = (16 * adaptive.fontScale).sp)
                Text("Required for the 'Emergency Brake' shield", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * adaptive.fontScale).sp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val scrollSpeed by viewModel.autoScrollSpeed.collectAsState(initial = 3f)
        
        Text(
            text = "Auto-Scroll Speed",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (16 * adaptive.fontScale).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Predefined speeds designed for users with tremors.",
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
        val enabledApps by viewModel.enabledScrollApps.collectAsState(initial = emptySet())
        val isGlobalEnabled = enabledApps.contains("all")
        var showAppPicker by remember { mutableStateOf(false) }

        Text(
            text = "Target Apps",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (16 * adaptive.fontScale).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        SettingsToggle(
            title = "Enable Globally",
            description = "Auto-Scroll activates in all apps.",
            checked = isGlobalEnabled,
            onCheckedChange = { checked ->
                if (checked) {
                    viewModel.setEnabledScrollApps(setOf("all"))
                } else {
                    viewModel.setEnabledScrollApps(emptySet())
                }
            }
        )

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
                Icon(Icons.Default.Keyboard, contentDescription = "Apps", tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(start = adaptive.spacingUnit)) {
                    val count = if (enabledApps.isEmpty()) "None" else "${enabledApps.size} selected"
                    Text("Select Specific Apps", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = (16 * adaptive.fontScale).sp)
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

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val adaptive = LocalAdaptiveParams.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = adaptive.spacingUnit)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (16 * adaptive.fontScale).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (14 * adaptive.fontScale).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
