package org.example.physiotrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.physiotrack.app.MainTab
import org.example.physiotrack.model.Exercise
import org.example.physiotrack.model.LocalTrainingExercises
import org.example.physiotrack.resources.Res
import org.example.physiotrack.resources.digital_wellbeing
import org.jetbrains.compose.resources.painterResource

@Composable
fun MainScaffold(
    tab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onStartTraining: (String) -> Unit,
    onOpenTherapistFeedback: () -> Unit,

    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onOpenGripTest: () -> Unit,


    exercises: List<Exercise>,
) {
    val trainingExercises = LocalTrainingExercises.items

    Scaffold(
        bottomBar = {
            BottomNavBar(
                current = tab,
                onSelect = onTabSelected,
            )
        },
    ) { padding ->
        val layoutDir = LocalLayoutDirection.current

        val contentPadding = if (tab == MainTab.Profile) {
            PaddingValues(
                start = padding.calculateStartPadding(layoutDir),
                top = 0.dp,
                end = padding.calculateEndPadding(layoutDir),
                bottom = padding.calculateBottomPadding()
            )
        } else {
            padding
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when (tab) {
                MainTab.Home -> HomeScreen(
                    onOpenExerciseLibrary = onOpenExerciseLibrary,
                    exercises = exercises,
                )

                MainTab.Program -> ProgramScreen(
                    onStartTraining = onStartTraining,
                    exercises = exercises,
                )

                MainTab.Train -> TrainScreen(
                    onOpenExerciseLibrary = onOpenExerciseLibrary,
                    onOpenExerciseDetail = onOpenExerciseDetail,
                    onOpenGripTest = onOpenGripTest,
                    onStartTraining = onStartTraining,
                    exercises = trainingExercises,
                )

                MainTab.Progress -> ProgressScreen(
                    onOpenTherapistFeedback = onOpenTherapistFeedback
                )

                MainTab.Profile -> ProfileScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onOpenHelpCenter = onOpenHelpCenter,
                    onOpenSettings = onOpenSettings,
                    onLogout = onLogout,
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    current: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    val barShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

    val barHeight = 72.dp
    val fabSize = 62.dp
    val notchSize = 86.dp
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val barColor = if (isDark) Color(0xFF0F2B5B) else Color(0xFFD6E9FF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .offset(y = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(barShape)
                .background(barColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    tab = MainTab.Home, current = current,
                    icon = Icons.Filled.Home, label = "Home",
                    onSelect = onSelect, modifier = Modifier.weight(1f)
                )
                NavItem(
                    tab = MainTab.Program, current = current,
                    icon = Icons.Filled.Today, label = "Program",
                    onSelect = onSelect, modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(notchSize))

                NavItem(
                    tab = MainTab.Progress, current = current,
                    icon = Icons.Filled.BarChart, label = "Progress",
                    onSelect = onSelect, modifier = Modifier.weight(1f)
                )
                NavItem(
                    tab = MainTab.Profile, current = current,
                    icon = Icons.Filled.Person, label = "Profile",
                    onSelect = onSelect, modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(notchSize)
                .align(Alignment.TopCenter)
                .offset(y = (-notchSize / 2))
                .background(MaterialTheme.colorScheme.background, CircleShape)
        )

        FloatingActionButton(
            onClick = { onSelect(MainTab.Train) },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            ),
            modifier = Modifier
                .size(fabSize)
                .align(Alignment.TopCenter)
                .offset(y = (-fabSize / 2))
        ) {
            Icon(
                painter = painterResource(Res.drawable.digital_wellbeing),
                contentDescription = "Train",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NavItem(
    tab: MainTab,
    current: MainTab,
    icon: ImageVector,
    label: String,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = tab == current
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelect(tab) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = fg,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
