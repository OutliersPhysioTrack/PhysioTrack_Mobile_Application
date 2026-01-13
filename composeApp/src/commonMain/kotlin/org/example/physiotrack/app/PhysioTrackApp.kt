package org.example.physiotrack.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.physiotrack.model.LocalTrainingExercises
import org.example.physiotrack.model.toUiExercise
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.prefs.AuthSession
import org.example.physiotrack.platform.PlatformBackHandler
import org.example.physiotrack.screens.ApiSettingsScreen
import org.example.physiotrack.screens.AuthChoiceScreen
import org.example.physiotrack.screens.BaselineAssessmentScreen
import org.example.physiotrack.screens.ExerciseDetailScreen
import org.example.physiotrack.screens.ExerciseLibraryScreen
import org.example.physiotrack.screens.GripTestScreen
import org.example.physiotrack.screens.HelpCenterScreen
import org.example.physiotrack.screens.LoginScreen
import org.example.physiotrack.screens.MainScaffold
import org.example.physiotrack.screens.OnboardingCredentialsScreen
import org.example.physiotrack.screens.OnboardingProfileScreen
import org.example.physiotrack.screens.PostSessionSurveyScreen
import org.example.physiotrack.screens.TherapistFeedbackScreen
import org.example.physiotrack.screens.WelcomeScreen
import org.example.physiotrack.training.TrainingSessionScreen
import org.example.physiotrack.ui.PhysioTheme

@Composable
fun PhysioTrackApp() {
    val nav = rememberAppNavigator()

    PlatformBackHandler(
        enabled = nav.canPop() || ((nav.current as? Route.Main)?.tab != MainTab.Home)
    ) {
        val r = nav.current
        if (r is Route.Main && r.tab != MainTab.Home) {
            nav.replace(Route.Main(MainTab.Home))
        } else {
            nav.pop()
        }
    }

    var isDarkTheme by rememberSaveable { mutableStateOf(false) }

    var reloadToken by rememberSaveable { mutableStateOf(0) }
    var exercises by androidx.compose.runtime.remember {
        mutableStateOf(emptyList<org.example.physiotrack.model.Exercise>())
    }

    val trainingExercises = androidx.compose.runtime.remember { LocalTrainingExercises.items }

    LaunchedEffect(Unit) {
        val savedPatientId = AuthSession.getPatientId()
        if (!savedPatientId.isNullOrBlank()) {
            AppNetwork.setConfig(AppNetwork.config.copy(patientId = savedPatientId))
            nav.replaceAll(Route.Main(MainTab.Home))
        }
    }

    LaunchedEffect(reloadToken, AppNetwork.config.baseUrl) {
        val fetched = runCatching {
            withContext(Dispatchers.Default) { AppNetwork.api.listExercises().map { it.toUiExercise() } }
        }.getOrNull().orEmpty()
        exercises = fetched
    }

    PhysioTheme(darkTheme = isDarkTheme) {
        when (val route = nav.current) {

            Route.Welcome -> WelcomeScreen(
                onGetStarted = { nav.push(Route.AuthChoice) }
            )

            Route.AuthChoice -> AuthChoiceScreen(
                onLogin = { nav.push(Route.Login) },
                onCreateAccount = { nav.push(Route.OnboardingProfile) },
            )

            Route.Login -> LoginScreen(
                onBack = { nav.pop() },
                onLoggedIn = { patientId ->
                    AuthSession.setPatientId(patientId)
                    AppNetwork.setConfig(AppNetwork.config.copy(patientId = patientId))
                    nav.replaceAll(Route.Main(MainTab.Home))
                }
            )

            Route.OnboardingProfile -> OnboardingProfileScreen(
                onBack = { nav.pop() },
                onContinue = { draft -> nav.push(Route.OnboardingCredentials(draft)) },
            )

            is Route.OnboardingCredentials -> OnboardingCredentialsScreen(
                draft = route.draft,
                onBack = { nav.pop() },
                onRegistered = { patientId ->
                    AuthSession.setPatientId(patientId)
                    AppNetwork.setConfig(AppNetwork.config.copy(patientId = patientId))
                    nav.replaceAll(Route.Main(MainTab.Home))
                }
            )

            Route.OnboardingBaseline -> BaselineAssessmentScreen(
                onBack = { nav.pop() },
                onStartAssessment = { nav.replaceAll(Route.Main(MainTab.Home)) },
                onSkip = { nav.replaceAll(Route.Main(MainTab.Home)) }
            )

            Route.HelpCenter -> HelpCenterScreen(
                onBack = { nav.pop() },
                onContactEmail = { /* TODO */ },
                onContactCall = { /* TODO */ },
                onContactChat = { /* TODO */ },
            )

            Route.TherapistFeedback -> TherapistFeedbackScreen(
                onBack = { nav.pop() }
            )

            is Route.Main -> MainScaffold(
                tab = route.tab,
                onTabSelected = { nav.replace(Route.Main(it)) },
                onOpenExerciseLibrary = { nav.push(Route.ExerciseLibrary) },
                onOpenExerciseDetail = { id -> nav.push(Route.ExerciseDetail(id)) },
                onStartTraining = { id -> nav.push(Route.TrainingSession(id)) },

                onOpenGripTest = { nav.push(Route.GripTest) },

                onOpenTherapistFeedback = { nav.push(Route.TherapistFeedback) },

                isDarkTheme = isDarkTheme,
                onToggleTheme = { isDarkTheme = !isDarkTheme },
                onOpenHelpCenter = { nav.push(Route.HelpCenter) },
                onOpenSettings = { nav.push(Route.ApiSettings) },
                onLogout = {
                    AuthSession.clear()
                    AppNetwork.setConfig(AppNetwork.config.copy(patientId = ""))
                    nav.replaceAll(Route.AuthChoice)
                },

                exercises = exercises,
            )

            Route.ExerciseLibrary -> ExerciseLibraryScreen(
                onBack = { nav.pop() },
                onOpenExercise = { id -> nav.push(Route.ExerciseDetail(id)) },
                exercises = trainingExercises,
            )

            is Route.ExerciseDetail -> {
                val all = exercises + trainingExercises
                val exercise = all.firstOrNull { it.id == route.exerciseId } ?: return@PhysioTheme
                ExerciseDetailScreen(
                    exercise = exercise,
                    onBack = { nav.pop() },
                    onStartSession = { nav.push(Route.TrainingSession(route.exerciseId)) },
                )
            }

            is Route.TrainingSession -> {
                val all = exercises + trainingExercises
                val exercise = all.firstOrNull { it.id == route.exerciseId } ?: return@PhysioTheme
                TrainingSessionScreen(
                    exercise = exercise,
                    onBack = { nav.pop() },
                    onFinish = { sid, setIndex, repsDone ->
                        nav.replace(
                            Route.PostSessionSurvey(
                                sessionId = sid,
                                exerciseTitle = exercise.title,
                                exerciseId = exercise.id,
                                setIndex = setIndex,
                                repsDone = repsDone
                            )
                        )
                    },
                )
            }

            Route.GripTest -> GripTestScreen(
                onBack = { nav.pop() },
                onFinish = { nav.pop() },
            )

            is Route.PostSessionSurvey -> PostSessionSurveyScreen(
                sessionId = route.sessionId,
                exerciseTitle = route.exerciseTitle,
                exerciseId = route.exerciseId,
                setIndex = route.setIndex,
                repsDone = route.repsDone,
                onBack = { nav.pop() },
                onDone = { nav.replaceAll(Route.Main(MainTab.Train)) },
            )

            Route.ApiSettings -> ApiSettingsScreen(
                onBack = { nav.pop() },
                onSaved = {
                    reloadToken += 1
                    nav.pop()
                }
            )
        }
    }
}
