package org.example.physiotrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.PremiumCardClickable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Refresh

private enum class ProfilePage {
    MAIN, SETTINGS, NOTIFICATIONS, PRIVACY, EXPORT
}

@Composable
fun ProfileScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onOpenHelpCenter: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onExportData: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val cfg = AppNetwork.config

    var patientName by remember { mutableStateOf("—") }
    var condition by remember { mutableStateOf("—") }
    var startedAt by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf<String?>(null) }

    var page by rememberSaveable { mutableStateOf(ProfilePage.MAIN) }

    // settings state (UI-only, tidak mengubah backend)
    var unitWeight by rememberSaveable { mutableStateOf("kg") }         // kg / lb
    var unitTemp by rememberSaveable { mutableStateOf("°C") }           // °C / °F
    var dateFormat by rememberSaveable { mutableStateOf("YYYY-MM-DD") } // YYYY-MM-DD / DD MMM YYYY
    var countdownSec by rememberSaveable { mutableStateOf(5) }          // 3/5/10
    var hapticsEnabled by rememberSaveable { mutableStateOf(true) }
    var refreshIntervalSec by rememberSaveable { mutableStateOf(2) }    // 2/5
    var textSize by rememberSaveable { mutableStateOf("Default") }      // Small/Default/Large
    var reduceMotion by rememberSaveable { mutableStateOf(false) }

    // notifications state
    var notifPlanReminder by rememberSaveable { mutableStateOf(true) }
    var notifDeviceDisconnected by rememberSaveable { mutableStateOf(true) }
    var notifSafetyAlerts by rememberSaveable { mutableStateOf(true) }
    var notifSessionHaptics by rememberSaveable { mutableStateOf(true) }
    var quietHours by rememberSaveable { mutableStateOf("22:00–07:00") } // Off / preset

    // privacy state
    var shareWithTherapist by rememberSaveable { mutableStateOf(true) }
    var retention by rememberSaveable { mutableStateOf("30 days") } // 7/30/90
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    // export state
    var exportType by rememberSaveable { mutableStateOf("PDF summary") } // PDF summary / CSV sensors / CSV sessions
    var exportRange by rememberSaveable { mutableStateOf("Last 7 days") } // Today / Last 7 / Last 30 / Custom (placeholder)
    var exportIncludeRaw by rememberSaveable { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cfg.baseUrl, cfg.patientId) {
        val pid = cfg.patientId.trim()
        if (pid.isBlank()) return@LaunchedEffect
        val p = runCatching { AppNetwork.api.getPatient(pid) }.getOrNull()
        if (p != null) {
            patientName = p.name
            condition = p.primary_condition ?: "—"
            startedAt = p.created_at
            email = p.email
            phone = p.phone
        }
    }

    // --------- page router ----------
    when (page) {
        ProfilePage.MAIN -> ProfileMain(
            patientName = patientName,
            condition = condition,
            startedAt = startedAt,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onOpenHelpCenter = onOpenHelpCenter,
            onOpenSettings = {
                page = ProfilePage.SETTINGS
            },
            onOpenNotifications = { page = ProfilePage.NOTIFICATIONS },
            onOpenPrivacy = { page = ProfilePage.PRIVACY },
            onExportData = { page = ProfilePage.EXPORT },
            onLogout = onLogout
        )

        ProfilePage.SETTINGS -> SettingsPage(
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            unitWeight = unitWeight,
            onUnitWeight = { unitWeight = it },
            unitTemp = unitTemp,
            onUnitTemp = { unitTemp = it },
            dateFormat = dateFormat,
            onDateFormat = { dateFormat = it },
            countdownSec = countdownSec,
            onCountdownSec = { countdownSec = it },
            hapticsEnabled = hapticsEnabled,
            onHapticsEnabled = { hapticsEnabled = it },
            refreshIntervalSec = refreshIntervalSec,
            onRefreshIntervalSec = { refreshIntervalSec = it },
            textSize = textSize,
            onTextSize = { textSize = it },
            reduceMotion = reduceMotion,
            onReduceMotion = { reduceMotion = it },
            onBack = { page = ProfilePage.MAIN }
        )

        ProfilePage.NOTIFICATIONS -> NotificationsPage(
            notifPlanReminder = notifPlanReminder,
            onNotifPlanReminder = { notifPlanReminder = it },
            notifDeviceDisconnected = notifDeviceDisconnected,
            onNotifDeviceDisconnected = { notifDeviceDisconnected = it },
            notifSafetyAlerts = notifSafetyAlerts,
            onNotifSafetyAlerts = { notifSafetyAlerts = it },
            notifSessionHaptics = notifSessionHaptics,
            onNotifSessionHaptics = { notifSessionHaptics = it },
            quietHours = quietHours,
            onQuietHours = { quietHours = it },
            onBack = { page = ProfilePage.MAIN }
        )

        ProfilePage.PRIVACY -> PrivacyPage(
            email = email,
            phone = phone,
            shareWithTherapist = shareWithTherapist,
            onShareWithTherapist = { shareWithTherapist = it },
            retention = retention,
            onRetention = { retention = it },
            onClearCache = { showClearCacheConfirm = true },
            onRequestDeletion = { showDeleteConfirm = true },
            onBack = { page = ProfilePage.MAIN }
        )

        ProfilePage.EXPORT -> ExportPage(
            exportType = exportType,
            onExportType = { exportType = it },
            exportRange = exportRange,
            onExportRange = { exportRange = it },
            exportIncludeRaw = exportIncludeRaw,
            onExportIncludeRaw = { exportIncludeRaw = it },
            exportStatus = exportStatus,
            onExport = {
                // UI-only: kamu bisa sambungkan ke API/Share sheet nanti.
                exportStatus = "Export prepared: $exportType • $exportRange" +
                        (if (exportIncludeRaw) " • raw included" else "")
                runCatching { onExportData() }
            },
            onBack = { page = ProfilePage.MAIN }
        )
    }

    // --------- confirmations ----------
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear local cache?") },
            text = { Text("This will clear temporary app data on this device (safe).") },
            confirmButton = {
                Text(
                    "Clear",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable {
                            exportStatus = null
                            showClearCacheConfirm = false
                        },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            dismissButton = {
                Text(
                    "Cancel",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { showClearCacheConfirm = false },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Request data deletion?") },
            text = {
                Text(
                    "This will request deletion of your account and related health data. " +
                            "You may lose access to your history."
                )
            },
            confirmButton = {
                Text(
                    "Request",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable {
                            exportStatus = "Deletion request created (pending)."
                            showDeleteConfirm = false
                        },
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            },
            dismissButton = {
                Text(
                    "Cancel",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { showDeleteConfirm = false },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

/* ---------------- MAIN ---------------- */

@Composable
private fun ProfileMain(
    patientName: String,
    condition: String,
    startedAt: String?,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onExportData: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 28.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(patientName, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        condition,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Started",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(formatIsoDateOnly(startedAt), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text("Settings & Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        MenuRowCard(icon = Icons.Filled.Settings, title = "Settings", onClick = onOpenSettings)
        Spacer(Modifier.height(12.dp))
        MenuRowCard(icon = Icons.Filled.NotificationsNone, title = "Notifications", onClick = onOpenNotifications)
        Spacer(Modifier.height(12.dp))
        MenuRowCard(icon = Icons.Filled.Lock, title = "Privacy & Data", onClick = onOpenPrivacy)
        Spacer(Modifier.height(12.dp))
        MenuRowCard(icon = Icons.Filled.UploadFile, title = "Export Data", onClick = onExportData)

        Spacer(Modifier.height(18.dp))

        Text("Help & Support", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Need assistance?", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Contact your therapist or access our help documentation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f))
                        .clickable { onOpenHelpCenter() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("View Help Center", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        PremiumCardClickable(
            modifier = Modifier.fillMaxWidth(),
            onClick = onLogout
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Log out", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
    }
}

/* ---------------- SETTINGS PAGE ---------------- */

@Composable
private fun SettingsPage(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    unitWeight: String,
    onUnitWeight: (String) -> Unit,
    unitTemp: String,
    onUnitTemp: (String) -> Unit,
    dateFormat: String,
    onDateFormat: (String) -> Unit,
    countdownSec: Int,
    onCountdownSec: (Int) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsEnabled: (Boolean) -> Unit,
    refreshIntervalSec: Int,
    onRefreshIntervalSec: (Int) -> Unit,
    textSize: String,
    onTextSize: (String) -> Unit,
    reduceMotion: Boolean,
    onReduceMotion: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    ProfilePageScaffold(
        title = "Settings",
        onBack = onBack
    ) {
        SectionTitle("Units & Display")
        Spacer(Modifier.height(10.dp))
        TwoOptionRow(
            title = "Weight unit",
            value = unitWeight,
            options = listOf("kg", "lb"),
            onSelect = onUnitWeight
        )
        Spacer(Modifier.height(10.dp))
        TwoOptionRow(
            title = "Temperature unit",
            value = unitTemp,
            options = listOf("°C", "°F"),
            onSelect = onUnitTemp
        )
        Spacer(Modifier.height(10.dp))
        TwoOptionRow(
            title = "Date format",
            value = dateFormat,
            options = listOf("YYYY-MM-DD", "DD MMM YYYY"),
            onSelect = onDateFormat
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("Workout Preferences")
        Spacer(Modifier.height(10.dp))
        DropdownRow(
            title = "Start countdown",
            value = "${countdownSec}s",
            options = listOf("3s", "5s", "10s"),
            onSelect = { sel -> onCountdownSec(sel.removeSuffix("s").toIntOrNull() ?: 5) }
        )
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            title = "Haptics",
            subtitle = "Vibration cues during sessions",
            checked = hapticsEnabled,
            onChecked = onHapticsEnabled
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("Sensor & Device")
        Spacer(Modifier.height(10.dp))
        DropdownRow(
            title = "Sensor refresh interval",
            value = "${refreshIntervalSec}s",
            options = listOf("2s", "5s"),
            onSelect = { sel -> onRefreshIntervalSec(sel.removeSuffix("s").toIntOrNull() ?: 2) }
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("Accessibility")
        Spacer(Modifier.height(10.dp))
        DropdownRow(
            title = "Text size",
            value = textSize,
            options = listOf("Small", "Default", "Large"),
            onSelect = onTextSize
        )
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            title = "Reduce motion",
            subtitle = "Use fewer animations",
            checked = reduceMotion,
            onChecked = onReduceMotion
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("Appearance")
        Spacer(Modifier.height(10.dp))
        PremiumCardClickable(modifier = Modifier.fillMaxWidth(), onClick = onToggleTheme) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text("Theme", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(if (isDarkTheme) "Dark" else "Light", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/* ---------------- NOTIFICATIONS PAGE ---------------- */

@Composable
private fun NotificationsPage(
    notifPlanReminder: Boolean,
    onNotifPlanReminder: (Boolean) -> Unit,
    notifDeviceDisconnected: Boolean,
    onNotifDeviceDisconnected: (Boolean) -> Unit,
    notifSafetyAlerts: Boolean,
    onNotifSafetyAlerts: (Boolean) -> Unit,
    notifSessionHaptics: Boolean,
    onNotifSessionHaptics: (Boolean) -> Unit,
    quietHours: String,
    onQuietHours: (String) -> Unit,
    onBack: () -> Unit,
) {
    ProfilePageScaffold(title = "Notifications", onBack = onBack) {
        SectionTitle("Reminders")
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            title = "Today's plan reminder",
            subtitle = "Get a reminder to complete your assigned exercises",
            checked = notifPlanReminder,
            onChecked = onNotifPlanReminder
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("Device")
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            title = "Device disconnected",
            subtitle = "Notify when PhysioTrack device is not connected",
            checked = notifDeviceDisconnected,
            onChecked = onNotifDeviceDisconnected
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("Safety")
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            title = "Safety alerts",
            subtitle = "Notify for abnormal vitals during monitoring (when available)",
            checked = notifSafetyAlerts,
            onChecked = onNotifSafetyAlerts
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("During session")
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            title = "Session haptics",
            subtitle = "Vibration cues when sets end / long pauses",
            checked = notifSessionHaptics,
            onChecked = onNotifSessionHaptics
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("Quiet hours")
        Spacer(Modifier.height(10.dp))
        DropdownRow(
            title = "Do not disturb",
            value = quietHours,
            options = listOf("Off", "22:00–07:00", "23:00–06:00"),
            onSelect = onQuietHours
        )
    }
}

/* ---------------- PRIVACY PAGE ---------------- */

@Composable
private fun PrivacyPage(
    email: String?,
    phone: String?,
    shareWithTherapist: Boolean,
    onShareWithTherapist: (Boolean) -> Unit,
    retention: String,
    onRetention: (String) -> Unit,
    onClearCache: () -> Unit,
    onRequestDeletion: () -> Unit,
    onBack: () -> Unit,
) {
    ProfilePageScaffold(title = "Privacy & Data", onBack = onBack) {
        SectionTitle("Account info")
        Spacer(Modifier.height(10.dp))
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Email", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(email ?: "—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text("Phone", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                val phoneDisplay = phone?.let { formatPhoneDisplayId(it) } ?: "—"
                Text(phoneDisplay, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("Data collected")
        Spacer(Modifier.height(10.dp))
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("• Sensor vitals: HR, SpO₂, ECG, temperature, GSR, loadcell", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("• Therapy plan: assignments, sessions, notes (when available)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("• Device status: connectivity and last seen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("Sharing & retention")
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            title = "Share data with therapist",
            subtitle = "Allow dashboard/therapist to view your progress",
            checked = shareWithTherapist,
            onChecked = onShareWithTherapist
        )
        Spacer(Modifier.height(10.dp))
        DropdownRow(
            title = "Keep sensor history",
            value = retention,
            options = listOf("7 days", "30 days", "90 days"),
            onSelect = onRetention
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("Manage data")
        Spacer(Modifier.height(10.dp))
        PremiumCardClickable(modifier = Modifier.fillMaxWidth(), onClick = onClearCache) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text("Clear local cache", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        PremiumCardClickable(modifier = Modifier.fillMaxWidth(), onClick = onRequestDeletion) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text("Request data deletion", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/* ---------------- EXPORT PAGE ---------------- */

@Composable
private fun ExportPage(
    exportType: String,
    onExportType: (String) -> Unit,
    exportRange: String,
    onExportRange: (String) -> Unit,
    exportIncludeRaw: Boolean,
    onExportIncludeRaw: (Boolean) -> Unit,
    exportStatus: String?,
    onExport: () -> Unit,
    onBack: () -> Unit,
) {
    ProfilePageScaffold(title = "Export Data", onBack = onBack) {
        SectionTitle("Export options")
        Spacer(Modifier.height(10.dp))
        DropdownRow(
            title = "Format",
            value = exportType,
            options = listOf("PDF summary", "CSV sensors", "CSV sessions"),
            onSelect = onExportType
        )
        Spacer(Modifier.height(10.dp))
        DropdownRow(
            title = "Range",
            value = exportRange,
            options = listOf("Today", "Last 7 days", "Last 30 days"),
            onSelect = onExportRange
        )
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            title = "Include raw sensor data",
            subtitle = "May produce large files",
            checked = exportIncludeRaw,
            onChecked = onExportIncludeRaw
        )

        Spacer(Modifier.height(14.dp))
        PremiumCardClickable(modifier = Modifier.fillMaxWidth(), onClick = onExport) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (exportType.startsWith("PDF")) Icons.Filled.Description else Icons.Filled.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text("Export now", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (exportStatus != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    exportStatus,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("Privacy note")
        Spacer(Modifier.height(10.dp))
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "Exported files may contain sensitive health data. " +
                            "Only share with trusted recipients.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/* ---------------- shared page scaffold ---------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 28.dp)
        ) {
            content()
        }
    }
}


@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun DropdownRow(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    PremiumCardClickable(modifier = Modifier.fillMaxWidth(), onClick = { expanded = true }) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = {
                            onSelect(opt)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TwoOptionRow(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    DropdownRow(title = title, value = value, options = options, onSelect = onSelect)
}

/* ---------------- components ---------------- */

@Composable
private fun MenuRowCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    PremiumCardClickable(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* ---------------- helpers ---------------- */

private fun formatIsoDateOnly(raw: String?): String {
    val s = raw?.trim().orEmpty()
    if (s.isBlank()) return "—"

    val parsed = runCatching {
        Instant.parse(s).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }.getOrNull()

    if (parsed != null) return parsed

    val tIdx = s.indexOf('T')
    if (tIdx > 0) return s.substring(0, tIdx)

    return s
}

private fun formatPhoneDisplayId(raw: String): String {
    val s = raw.trim()
    if (s.isBlank()) return "—"
    if (s.startsWith("+")) return s

    val digits = s.filter(Char::isDigit)
    if (digits.isBlank()) return raw

    return when {
        digits.startsWith("62") -> "+$digits"
        digits.startsWith("0") -> "+62" + digits.drop(1)
        else -> digits
    }
}

@Composable
private fun BigProgressStatCard(
    modifier: Modifier,
    value: String,
    label: String,
) {
    PremiumCard(modifier = modifier.height(118.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 14.sp
            )
        }
    }
}
