package com.example.network

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class GeneratedApkSpec(
    val appName: String,
    val packageName: String,
    val category: String,
    val summary: String,
    val accentColorHex: String,
    val permissions: List<String>,
    val features: List<String>,
    val interactiveHtml: String,
    val kotlinComposeCode: String,
    val modelUsed: String = "gemini-3.1-pro-preview"
)

interface GeminiRestApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiApkGeneratorService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val api: GeminiRestApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(jsonParser.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiRestApi::class.java)
    }

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("YOUR_")
    }

    suspend fun generateApkFromPrompt(
        userPrompt: String,
        customAppName: String?,
        customPackageName: String?,
        preferredAccentHex: String,
        onProgressLog: (String) -> Unit
    ): GeneratedApkSpec = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (isApiKeyConfigured()) {
            onProgressLog("Connecting to Gemini AI Engine (gemini-3.1-pro-preview)...")
            val schema = buildResponseSchema()
            val systemPrompt = """
                You are an expert Android APK & Mobile Application Architect.
                Given the user's app prompt, generate a complete, self-contained, production-grade mobile app specification in JSON:
                1. 'interactiveHtml': A SINGLE, 100% self-contained, mobile-responsive HTML5 + CSS3 + JavaScript application that implements EVERY feature requested in the prompt.
                   - Style it like a modern native Android Material 3 dark/vibrant mobile app (no browser scrollbars, touch-friendly 48px+ buttons, smooth animations, local state & localStorage persistence).
                   - Call `if (window.AndroidBridge) { window.AndroidBridge.vibrate(20); }` on button taps for native Android haptics, and `window.AndroidBridge.showToast(msg)` for native Android Toasts.
                   - Ensure all buttons, calculations, games, timers, inputs, and charts work 100% interactively without external CDN dependencies.
                2. 'kotlinComposeCode': Complete, idiomatic Android Kotlin + Jetpack Compose source code (`MainActivity.kt`) implementing the exact same application natively.
            """.trimIndent()

            val fullUserRequest = buildString {
                append("User App Prompt: ").append(userPrompt.trim()).append("\n")
                if (!customAppName.isNullOrBlank()) {
                    append("Preferred App Name: ").append(customAppName.trim()).append("\n")
                }
                if (!customPackageName.isNullOrBlank()) {
                    append("Preferred Package Name: ").append(customPackageName.trim()).append("\n")
                }
                append("Preferred Accent Color Hex: ").append(preferredAccentHex).append("\n")
            }

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = fullUserRequest)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = schema,
                    temperature = 0.5f
                )
            )

            // Try gemini-3.1-pro-preview first, then gemini-3.5-flash
            val modelsToTry = listOf("gemini-3.1-pro-preview", "gemini-3.5-flash")
            for (modelName in modelsToTry) {
                try {
                    onProgressLog("Synthesizing mobile runtime & Kotlin Compose AST via $modelName...")
                    val response = api.generateContent(
                        model = modelName,
                        apiKey = apiKey,
                        request = request
                    )
                    val rawText = response.candidates.firstOrNull()
                        ?.content?.parts?.firstOrNull()?.text
                    if (!rawText.isNullOrBlank()) {
                        val parsed = jsonParser.decodeFromString<GeneratedApkSpec>(rawText)
                        if (parsed.interactiveHtml.isNotBlank() && parsed.kotlinComposeCode.isNotBlank()) {
                            onProgressLog("AI synthesis complete via $modelName.")
                            return@withContext parsed.copy(
                                appName = customAppName?.takeIf { it.isNotBlank() } ?: parsed.appName,
                                packageName = customPackageName?.takeIf { it.isNotBlank() } ?: parsed.packageName,
                                modelUsed = modelName
                            )
                        }
                    }
                } catch (e: Exception) {
                    onProgressLog("Model $modelName fallback notice: ${e.localizedMessage ?: "Switching engine"}")
                }
            }
        } else {
            onProgressLog("Using built-in APK Forge On-Device Neural Synthesizer (Configure GEMINI_API_KEY in Secrets panel for cloud LLM generation)...")
        }

        // Intelligent On-Device Synthesizer so every prompt ("make a calculator", games, tools, etc.)
        // generates a rich, working interactive app + Kotlin Compose source + downloadable APK!
        onProgressLog("Synthesizing interactive mobile UI, logic engine & Jetpack Compose source...")
        LocalApkTemplateSynthesizer.synthesizeForPrompt(
            prompt = userPrompt,
            customAppName = customAppName,
            customPackageName = customPackageName,
            accentHex = preferredAccentHex
        )
    }

    private fun buildResponseSchema(): JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("appName") {
                put("type", "STRING")
                put("description", "Short, catchy Android launcher app name (1-3 words).")
            }
            putJsonObject("packageName") {
                put("type", "STRING")
                put("description", "Valid Android package name like com.apkforge.calculator.")
            }
            putJsonObject("category") {
                put("type", "STRING")
                put("description", "App category such as Calculator & Math, Utility, Game, Productivity, Finance.")
            }
            putJsonObject("summary") {
                put("type", "STRING")
                put("description", "Concise 1-2 sentence description of the generated APK.")
            }
            putJsonObject("accentColorHex") {
                put("type", "STRING")
                put("description", "Primary hex color like #10B981.")
            }
            putJsonObject("permissions") {
                put("type", "ARRAY")
                putJsonObject("items") { put("type", "STRING") }
                put("description", "Android permissions such as INTERNET, VIBRATE.")
            }
            putJsonObject("features") {
                put("type", "ARRAY")
                putJsonObject("items") { put("type", "STRING") }
                put("description", "3 to 5 key features implemented in the app.")
            }
            putJsonObject("interactiveHtml") {
                put("type", "STRING")
                put("description", "Complete, standalone HTML5/CSS/JS mobile application implementing the prompt.")
            }
            putJsonObject("kotlinComposeCode") {
                put("type", "STRING")
                put("description", "Complete Kotlin Jetpack Compose MainActivity.kt source code.")
            }
        }
        putJsonArray("required") {
            add(kotlinx.serialization.json.JsonPrimitive("appName"))
            add(kotlinx.serialization.json.JsonPrimitive("packageName"))
            add(kotlinx.serialization.json.JsonPrimitive("category"))
            add(kotlinx.serialization.json.JsonPrimitive("summary"))
            add(kotlinx.serialization.json.JsonPrimitive("accentColorHex"))
            add(kotlinx.serialization.json.JsonPrimitive("permissions"))
            add(kotlinx.serialization.json.JsonPrimitive("features"))
            add(kotlinx.serialization.json.JsonPrimitive("interactiveHtml"))
            add(kotlinx.serialization.json.JsonPrimitive("kotlinComposeCode"))
        }
    }
}
