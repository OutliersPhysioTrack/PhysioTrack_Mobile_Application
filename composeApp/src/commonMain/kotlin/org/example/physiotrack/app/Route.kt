package org.example.physiotrack.app

sealed interface Route {
    data object Welcome : Route
    data object AuthChoice : Route
    data object Login : Route
    data object OnboardingProfile : Route
    data class OnboardingCredentials(val draft: org.example.physiotrack.model.RegistrationDraft) : Route
    data object OnboardingBaseline : Route

    data class Main(val tab: MainTab) : Route
    data object ExerciseLibrary : Route
    data class ExerciseDetail(val exerciseId: String) : Route
    data class TrainingSession(val exerciseId: String) : Route

    data object GripTest : Route

    data class PostSessionSurvey(
        val sessionId: String?,
        val exerciseTitle: String,
        val exerciseId: String,
        val setIndex: Int,
        val repsDone: Int,
    ) : Route

    data object TherapistFeedback : Route
    data object HelpCenter : Route

    data object ApiSettings : Route
}
