package com.example.ai

import com.example.BuildConfig
import com.example.data.SessionWithSets
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class GeminiRequest(
    @field:Json(name = "contents") val contents: List<GeminiContent>,
    @field:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    @field:Json(name = "parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @field:Json(name = "text") val text: String
)

data class GeminiResponse(
    @field:Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

data class GeminiCandidate(
    @field:Json(name = "content") val content: GeminiContent? = null
)

interface GeminiRestService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiScienceAnalyzer {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service: GeminiRestService = retrofit.create(GeminiRestService::class.java)

    suspend fun generateMonthlyAnalysis(
        monthYear: String,
        sessions: List<SessionWithSets>
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Pair(
                generateFallbackAnalysis(sessions),
                "• Dr. Brad Schoenfeld (2021) - Principles and Practice of Resistance Training.\n" +
                "• Dr. Mike Israetel (Renaissance Periodization) - Maximum Recoverable Volume (MRV) & Landmark Volumes.\n" +
                "• NSCA (National Strength and Conditioning Association) - Guidelines for Hypertrophy and Progressive Overload."
            )
        }

        val totalSessions = sessions.size
        val totalSetsCompleted = sessions.sumOf { s -> s.setsWithDetails.count { it.set.completed } }
        val totalVolumeKg = sessions.sumOf { s ->
            s.setsWithDetails.filter { it.set.completed }.sumOf { (it.set.weightKg * it.set.reps).toDouble() }
        }.toInt()

        val exerciseSummary = sessions.flatMap { it.setsWithDetails }
            .filter { it.set.completed }
            .groupBy { it.exercise.name }
            .mapValues { entry ->
                val maxWeight = entry.value.maxOfOrNull { it.set.weightKg } ?: 0f
                val totalSets = entry.value.size
                "$totalSets séries (Max: ${maxWeight}kg)"
            }.entries.take(8).joinToString("; ") { "${it.key}: ${it.value}" }

        val prompt = """
            Agis en tant qu'expert en sciences du sport, physiologie de la musculation et médecine sportive.
            Analyse les données du mois ($monthYear) de l'utilisateur :
            - Nombre de séances : $totalSessions
            - Séries totales validées : $totalSetsCompleted
            - Tonnage/Volume total : $totalVolumeKg kg
            - Exercices principaux : $exerciseSummary

            Structure ta réponse en deux sections séparées par le délimiteur '===REFERENCES===' :
            SECTION 1: Analyse scientifique détaillée et personnalisée en français.
            - Analyse de la Surcharge Progressive (Volume effectif vs Récupération).
            - Recommandations pour le mois prochain (RPE suggéré, gestion du volume MRV/MEV, deload éventuel).
            - Conseils physiologiques (sommeil, apport protéique, récupération articulaire).

            ===REFERENCES===
            SECTION 2: Liste de 3 à 5 études scientifiques précises ou méta-analyses médicales de référence (ex: Dr. Brad Schoenfeld, Dr. Mike Israetel, NSCA, Journal of Sports Sciences).
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(
                        text = "Tu es un scientifique du sport chevronné, encourageant, pédagogique et rigoureux. Rédige en français sous forme synthétique et claire avec des puces."
                    )
                )
            )
        )

        try {
            val response = service.generateContent(apiKey, request)
            val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!fullText.isNullOrBlank()) {
                val parts = fullText.split("===REFERENCES===")
                val analysisText = parts.getOrNull(0)?.trim() ?: fullText
                val refsText = parts.getOrNull(1)?.trim() ?: "• Principes NSCA et Études de Brad Schoenfeld sur l'hypertrophie."
                Pair(analysisText, refsText)
            } else {
                Pair(generateFallbackAnalysis(sessions), getDefaultReferences())
            }
        } catch (e: Exception) {
            Pair(generateFallbackAnalysis(sessions), getDefaultReferences())
        }
    }

    private fun generateFallbackAnalysis(sessions: List<SessionWithSets>): String {
        val totalSessions = sessions.size
        val totalSets = sessions.sumOf { s -> s.setsWithDetails.count { it.set.completed } }
        val totalVolumeKg = sessions.sumOf { s ->
            s.setsWithDetails.filter { it.set.completed }.sumOf { (it.set.weightKg * it.set.reps).toDouble() }
        }.toInt()

        return """
            ### 📈 Bilan de Progression Mensuelle
            
            • **Volume Global :** $totalVolumeKg kg soulevés à travers $totalSets séries validées sur $totalSessions séances.
            • **Principe de Surcharge Progressive :** Vos séances montrent un engagement constant. Pour continuer à progresser selon les recherches en science du sport, augmentez la charge de 2.5% ou ajoutez 1 répétition par série la semaine prochaine.
            • **Récupération & Volume Effectif (MEV / MRV) :** Veillez à maintenir entre 10 et 20 séries effectives par groupe musculaire par semaine pour optimiser l'hypertrophie sans risquer le surentraînement.
            • **Conseil Médical :** Accordez une attention particulière à la qualité du sommeil (7-9h/nuit) et à l'hydratation pendant vos entraînements intenses.
        """.trimIndent()
    }

    private fun getDefaultReferences(): String {
        return "• Schoenfeld, B. J. et al. (2021). Loading Recommendations for Muscle Strength, Hypertrophy, and Local Muscular Endurance. Sports (Basel).\n" +
               "• Israetel, M. (2020). Scientific Principles of Hypertrophy Training. Renaissance Periodization.\n" +
               "• NSCA (National Strength & Conditioning Association) - Essentials of Strength Training and Conditioning."
    }
}
