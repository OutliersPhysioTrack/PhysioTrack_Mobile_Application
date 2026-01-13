package org.example.physiotrack.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.physiotrack.model.PainSurvey
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.network.SessionPatchDto
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.PremiumCardClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostSessionSurveyScreen(
    sessionId: String?,
    exerciseTitle: String,
    exerciseId: String,
    setIndex: Int,
    repsDone: Int,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val answers = remember { mutableStateMapOf<String, Int>() }
    var notes by remember { mutableStateOf("") }

    var posting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val allAnswered = answers.size == PainSurvey.questions.size
    val rawScore = PainSurvey.computeRawScore(answers)
    val painScore = PainSurvey.computePainScore0to10(rawScore)

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Post-Session Survey", fontWeight = FontWeight.SemiBold) },
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
                .padding(16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text(exerciseTitle, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "• Set: ${setIndex + 1} • Reps done: $repsDone",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PainSurvey.questions.forEachIndexed { index, q ->
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("${index + 1}. ${q.text}", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    val selectedScore = answers[q.id]
                    q.options.forEach { opt ->
                        OptionRow(
                            label = opt.label,
                            selected = (selectedScore == opt.score),
                            onSelect = { answers[q.id] = opt.score }
                        )
                    }
                }
            }

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("Notes (optional)", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("Any discomfort / comments") }
                )
            }

            PremiumCardClickable(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (posting) return@PremiumCardClickable

                    if (!allAnswered) {
                        status = "⚠️ Please answer all questions"
                        return@PremiumCardClickable
                    }

                    posting = true
                    status = null

                    scope.launch {
                        val sid = sessionId?.trim().orEmpty()

                        if (sid.isBlank()) {
                            status = "ℹ️ SessionId kosong, survey tidak dikirim ke server"
                            posting = false
                            onDone() // tetap lanjut (karena memang gak bisa simpan)
                            return@launch
                        }

                        val ok = runCatching {
                            AppNetwork.api.patchSession(
                                sessionId = sid,
                                payload = SessionPatchDto(
                                    pain_score = painScore,
                                    pain_raw_score = rawScore,
                                    pain_notes = notes.trim().ifBlank { null },
                                )
                            )
                        }.getOrNull() == true

                        posting = false

                        if (ok) {
                            status = "✅ Survey saved"
                            onDone()
                        } else {
                            status = "⚠️ Survey not saved (API error). Please try again."

                        }
                    }
                }
            ) {
                Text(
                    text = when {
                        posting -> "Submitting..."
                        allAnswered -> "Submit"
                        else -> "Answer all questions"
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = if (allAnswered) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = label,
            modifier = Modifier.padding(top = 12.dp),
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
