@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.example.physiotrack.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.physiotrack.network.ApiConfig
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.PremiumCardClickable

@Composable
fun ApiSettingsScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    var baseUrl by remember { mutableStateOf(AppNetwork.config.baseUrl) }
    var deviceId by remember { mutableStateOf(AppNetwork.config.deviceId) }
    var patientId by remember { mutableStateOf(AppNetwork.config.patientId) }

    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("API Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Because your tunnel URL can change, set Base URL here before testing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("http://192.168.1.40:8000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = deviceId,
                        onValueChange = { deviceId = it },
                        label = { Text("Device ID") },
                        placeholder = { Text("dev-001") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = patientId,
                        onValueChange = { patientId = it },
                        label = { Text("Patient UUID (optional for read-only)") },
                        placeholder = { Text("e.g. 3fa85f64-5717-4562-b3fc-2c963f66afa6") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }

            PremiumCardClickable(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val newCfg = ApiConfig(
                        baseUrl = baseUrl.trim(),
                        deviceId = deviceId.trim(),
                        patientId = patientId.trim(),
                    )
                    AppNetwork.setConfig(newCfg)
                    onSaved()
                }
            ) {
                Text("Save", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.SemiBold)
            }

            PremiumCardClickable(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    testing = true
                    testResult = null
                }
            ) {
                Text(if (testing) "Testing..." else "Test /health", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.SemiBold)
            }

            if (testing) {
                LaunchedEffect(Unit) {
                    val ok = runCatching {
                        withContext(Dispatchers.Default) { AppNetwork.api.health() }
                    }.getOrElse { false }

                    testResult = if (ok) "✅ Connected" else "❌ Failed"
                    testing = false
                }
            }

            testResult?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
