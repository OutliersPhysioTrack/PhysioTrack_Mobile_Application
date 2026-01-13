package org.example.physiotrack.model

data class SurveyOption(
    val id: String,
    val label: String,
    val score: Int,
)

data class SurveyQuestion(
    val id: String,
    val text: String,
    val options: List<SurveyOption>
)

object PainSurvey {
    val questions: List<SurveyQuestion> = listOf(
        SurveyQuestion(
            id = "q1",
            text = "Apakah ada nyeri saat melakukan gerakan?",
            options = listOf(
                SurveyOption("q1_a0", "Tidak ada", 0),
                SurveyOption("q1_a1", "Sedikit", 1),
                SurveyOption("q1_a2", "Sedang", 2),
                SurveyOption("q1_a3", "Berat", 3),
            )
        ),
        SurveyQuestion(
            id = "q2",
            text = "Nyeri muncul kapan?",
            options = listOf(
                SurveyOption("q2_a0", "Tidak muncul", 0),
                SurveyOption("q2_a1", "Di akhir gerakan", 1),
                SurveyOption("q2_a2", "Di tengah gerakan", 2),
                SurveyOption("q2_a3", "Sejak awal gerakan", 3),
            )
        ),
        SurveyQuestion(
            id = "q3",
            text = "Nyeri terasa seperti apa?",
            options = listOf(
                SurveyOption("q3_a0", "Tidak ada", 0),
                SurveyOption("q3_a1", "Ngilu ringan", 1),
                SurveyOption("q3_a2", "Menusuk", 2),
                SurveyOption("q3_a3", "Sangat menusuk", 3),
            )
        ),
        SurveyQuestion(
            id = "q4",
            text = "Apakah nyeri mengganggu kelanjutan latihan?",
            options = listOf(
                SurveyOption("q4_a0", "Tidak", 0),
                SurveyOption("q4_a1", "Sedikit", 1),
                SurveyOption("q4_a2", "Ya, agak mengganggu", 2),
                SurveyOption("q4_a3", "Ya, sangat mengganggu", 3),
            )
        ),
        SurveyQuestion(
            id = "q5",
            text = "Apakah ada rasa panas / bengkak?",
            options = listOf(
                SurveyOption("q5_a0", "Tidak", 0),
                SurveyOption("q5_a1", "Sedikit", 1),
                SurveyOption("q5_a2", "Sedang", 2),
                SurveyOption("q5_a3", "Jelas terasa", 3),
            )
        ),
        SurveyQuestion(
            id = "q6",
            text = "Nyeri menetap setelah latihan?",
            options = listOf(
                SurveyOption("q6_a0", "Tidak", 0),
                SurveyOption("q6_a1", "< 10 menit", 1),
                SurveyOption("q6_a2", "10–30 menit", 2),
                SurveyOption("q6_a3", "> 30 menit", 3),
            )
        ),
        SurveyQuestion(
            id = "q7",
            text = "Apakah nyeri mempengaruhi aktivitas setelah latihan?",
            options = listOf(
                SurveyOption("q7_a0", "Tidak", 0),
                SurveyOption("q7_a1", "Sedikit", 1),
                SurveyOption("q7_a2", "Sedang", 2),
                SurveyOption("q7_a3", "Sangat", 3),
            )
        ),
        SurveyQuestion(
            id = "q8",
            text = "Seberapa sering nyeri terasa selama set?",
            options = listOf(
                SurveyOption("q8_a0", "Tidak pernah", 0),
                SurveyOption("q8_a1", "Kadang-kadang", 1),
                SurveyOption("q8_a2", "Sering", 2),
                SurveyOption("q8_a3", "Hampir selalu", 3),
            )
        ),
        SurveyQuestion(
            id = "q9",
            text = "Nyeri meningkat dibanding set sebelumnya?",
            options = listOf(
                SurveyOption("q9_a0", "Tidak", 0),
                SurveyOption("q9_a1", "Sedikit meningkat", 1),
                SurveyOption("q9_a2", "Jelas meningkat", 2),
                SurveyOption("q9_a3", "Meningkat drastis", 3),
            )
        ),
        SurveyQuestion(
            id = "q10",
            text = "Kamu butuh berhenti karena nyeri?",
            options = listOf(
                SurveyOption("q10_a0", "Tidak", 0),
                SurveyOption("q10_a1", "Ingin berhenti tapi bisa lanjut", 1),
                SurveyOption("q10_a2", "Sempat berhenti sebentar", 2),
                SurveyOption("q10_a3", "Harus berhenti", 3),
            )
        ),
    )

    fun computeRawScore(answers: Map<String, Int>): Int =
        questions.sumOf { q -> answers[q.id] ?: 0 }

    fun computePainScore0to10(rawScore: Int): Int {
        val maxScore = questions.sumOf { it.options.maxOf { op -> op.score } }
        if (maxScore <= 0) return 0
        val normalized = (rawScore.toFloat() / maxScore.toFloat()) * 10f
        return normalized.toInt().coerceIn(0, 10)
    }
}
