@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.physiotrack.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.physiotrack.model.Exercise
import org.example.physiotrack.ui.components.PremiumCardClickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.navigationBarsPadding

@Composable
fun ExerciseLibraryScreen(
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    exercises: List<Exercise>,
) {
    var q by remember { mutableStateOf("") }
    val items = exercises.filter { it.title.contains(q, ignoreCase = true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Exercise Library", fontWeight = FontWeight.SemiBold) },
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
        ) {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search exercises...") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))

            Text("All Exercises", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { ex ->
                    ExerciseRow(exercise = ex, onClick = { onOpenExercise(ex.id) })
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    PremiumCardClickable(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(exercise.title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${exercise.durationMin} min • ${exercise.difficulty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
