package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ApkBuildEntity
import com.example.data.ApkRepository
import com.example.network.GeminiApkGeneratorService
import com.example.network.LocalApkTemplateSynthesizer
import com.example.packager.ApkPackagerEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PromptPreset(
    val id: String,
    val title: String,
    val badge: String,
    val prompt: String,
    val defaultAppName: String,
    val defaultPackageName: String,
    val accentHex: String
)

data class ForgeUiState(
    val promptText: String = "Make a scientific calculator with trigonometry, unit converter (km/miles, kg/lbs), calculation history tape, and neon dark theme",
    val customAppName: String = "",
    val customPackageName: String = "",
    val versionName: String = "1.0.0",
    val selectedAccentHex: String = "#10B981",
    val showAdvancedConfig: Boolean = false,
    val isGenerating: Boolean = false,
    val buildProgress: Float = 0f,
    val currentBuildStage: String = "",
    val buildConsoleLogs: List<String> = emptyList(),
    val activeBuildId: Long? = null,
    val fullScreenLaunchBuild: ApkBuildEntity? = null,
    val statusBannerMessage: String? = null,
    val isStatusSuccess: Boolean = true,
    val isDarkTheme: Boolean = true
)

class ApkForgeViewModel(
    private val appContext: Context,
    private val repository: ApkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgeUiState())
    val uiState: StateFlow<ForgeUiState> = _uiState.asStateFlow()

    val allBuilds: StateFlow<List<ApkBuildEntity>> = repository.allBuilds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val presets: List<PromptPreset> = listOf(
        PromptPreset(
            id = "calc",
            title = "Scientific Calculator Pro",
            badge = "Calculator",
            prompt = "Make a scientific calculator with trigonometry, unit converter (km/miles, kg/lbs), calculation history tape, and neon dark theme",
            defaultAppName = "Quantum Calc Pro",
            defaultPackageName = "com.apkforge.quantumcalc",
            accentHex = "#10B981"
        ),
        PromptPreset(
            id = "snake",
            title = "Neon Cyber Snake Game",
            badge = "Arcade Game",
            prompt = "Create an arcade Neon Cyber Snake game with touch D-Pad controls, score tracker, high score persistence, and haptic feedback",
            defaultAppName = "Neon Cyber Snake",
            defaultPackageName = "com.apkforge.cybersnake",
            accentHex = "#06B6D4"
        ),
        PromptPreset(
            id = "timer",
            title = "Chrono Focus Pomodoro",
            badge = "Productivity",
            prompt = "Build a Pomodoro focus timer app with 25m focus sessions, 5m short breaks, circular countdown ring, and haptic alerts",
            defaultAppName = "Chrono Focus Pro",
            defaultPackageName = "com.apkforge.chronofocus",
            accentHex = "#F59E0B"
        ),
        PromptPreset(
            id = "tasks",
            title = "TaskFlow & Notes Manager",
            badge = "Organizer",
            prompt = "Build a fast task checklist and quick notes organizer app with persistent storage, instant completion toggles, and clean cards",
            defaultAppName = "TaskFlow Studio",
            defaultPackageName = "com.apkforge.taskflow",
            accentHex = "#10B981"
        )
    )

    init {
        seedInitialShowcaseBuildsIfNeeded()
    }

    private fun seedInitialShowcaseBuildsIfNeeded() {
        viewModelScope.launch {
            if (repository.getBuildCount() == 0) {
                val calcSpec = LocalApkTemplateSynthesizer.buildCalculatorApkSpec(
                    prompt = presets[0].prompt,
                    customAppName = presets[0].defaultAppName,
                    customPackageName = presets[0].defaultPackageName,
                    accentHex = presets[0].accentHex
                )
                val calcPackaged = ApkPackagerEngine.packageApkAndProject(
                    context = appContext,
                    appName = calcSpec.appName,
                    packageName = calcSpec.packageName,
                    versionName = "1.0.0",
                    versionCode = 1,
                    minSdk = 24,
                    targetSdk = 36,
                    permissions = calcSpec.permissions,
                    interactiveHtmlCode = calcSpec.interactiveHtml,
                    kotlinMainActivityCode = calcSpec.kotlinComposeCode
                )
                val firstId = repository.insertBuild(
                    ApkBuildEntity(
                        appName = calcSpec.appName,
                        packageName = calcSpec.packageName,
                        versionName = "1.0.0",
                        versionCode = 1,
                        category = calcSpec.category,
                        prompt = presets[0].prompt,
                        summary = calcSpec.summary,
                        accentColorHex = calcSpec.accentColorHex,
                        permissionsCsv = calcSpec.permissions.joinToString(","),
                        featuresCsv = calcSpec.features.joinToString("|"),
                        interactiveHtmlCode = calcSpec.interactiveHtml,
                        kotlinMainActivityCode = calcSpec.kotlinComposeCode,
                        androidManifestXml = calcPackaged.androidManifestXml,
                        buildGradleKts = calcPackaged.buildGradleKts,
                        apkFilePath = calcPackaged.apkFile.absolutePath,
                        projectZipFilePath = calcPackaged.projectZipFile.absolutePath,
                        apkFileSizeBytes = calcPackaged.apkSizeBytes,
                        sha256Signature = calcPackaged.sha256Hex,
                        aiModelUsed = calcSpec.modelUsed
                    )
                )

                val gameSpec = LocalApkTemplateSynthesizer.buildArcadeGameApkSpec(
                    prompt = presets[1].prompt,
                    customAppName = presets[1].defaultAppName,
                    customPackageName = presets[1].defaultPackageName,
                    accentHex = presets[1].accentHex
                )
                val gamePackaged = ApkPackagerEngine.packageApkAndProject(
                    context = appContext,
                    appName = gameSpec.appName,
                    packageName = gameSpec.packageName,
                    versionName = "1.2.0",
                    versionCode = 2,
                    minSdk = 24,
                    targetSdk = 36,
                    permissions = gameSpec.permissions,
                    interactiveHtmlCode = gameSpec.interactiveHtml,
                    kotlinMainActivityCode = gameSpec.kotlinComposeCode
                )
                repository.insertBuild(
                    ApkBuildEntity(
                        appName = gameSpec.appName,
                        packageName = gameSpec.packageName,
                        versionName = "1.2.0",
                        versionCode = 2,
                        category = gameSpec.category,
                        prompt = presets[1].prompt,
                        summary = gameSpec.summary,
                        accentColorHex = gameSpec.accentColorHex,
                        permissionsCsv = gameSpec.permissions.joinToString(","),
                        featuresCsv = gameSpec.features.joinToString("|"),
                        interactiveHtmlCode = gameSpec.interactiveHtml,
                        kotlinMainActivityCode = gameSpec.kotlinComposeCode,
                        androidManifestXml = gamePackaged.androidManifestXml,
                        buildGradleKts = gamePackaged.buildGradleKts,
                        apkFilePath = gamePackaged.apkFile.absolutePath,
                        projectZipFilePath = gamePackaged.projectZipFile.absolutePath,
                        apkFileSizeBytes = gamePackaged.apkSizeBytes,
                        sha256Signature = gamePackaged.sha256Hex,
                        aiModelUsed = gameSpec.modelUsed,
                        createdAt = System.currentTimeMillis() - 60_000L
                    )
                )

                _uiState.update { it.copy(activeBuildId = firstId) }
            }
        }
    }

    fun updatePrompt(text: String) {
        _uiState.update { it.copy(promptText = text) }
    }

    fun updateCustomAppName(name: String) {
        _uiState.update { it.copy(customAppName = name) }
    }

    fun updateCustomPackageName(pkg: String) {
        _uiState.update { it.copy(customPackageName = pkg) }
    }

    fun updateVersionName(ver: String) {
        _uiState.update { it.copy(versionName = ver) }
    }

    fun selectAccentColor(hex: String) {
        _uiState.update { it.copy(selectedAccentHex = hex) }
    }

    fun toggleAdvancedConfig() {
        _uiState.update { it.copy(showAdvancedConfig = !it.showAdvancedConfig) }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun applyPreset(preset: PromptPreset) {
        _uiState.update {
            it.copy(
                promptText = preset.prompt,
                customAppName = preset.defaultAppName,
                customPackageName = preset.defaultPackageName,
                selectedAccentHex = preset.accentHex
            )
        }
    }

    fun selectActiveBuild(buildId: Long) {
        _uiState.update { it.copy(activeBuildId = buildId) }
    }

    fun openFullScreenApp(build: ApkBuildEntity) {
        _uiState.update { it.copy(fullScreenLaunchBuild = build) }
    }

    fun closeFullScreenApp() {
        _uiState.update { it.copy(fullScreenLaunchBuild = null) }
    }

    fun dismissStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun generateApk(onCompleteNavigateToPreview: () -> Unit) {
        val currentState = _uiState.value
        val prompt = currentState.promptText.trim()
        if (prompt.isEmpty()) {
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Please describe the app you want to generate first.",
                    isStatusSuccess = false
                )
            }
            return
        }

        viewModelScope.launch {
            val logs = mutableListOf<String>()
            fun pushLog(msg: String) {
                logs.add("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}] $msg")
                _uiState.update { it.copy(buildConsoleLogs = logs.toList()) }
            }

            _uiState.update {
                it.copy(
                    isGenerating = true,
                    buildProgress = 0.12f,
                    currentBuildStage = "1/4 • Parsing Prompt & Architecture Spec...",
                    buildConsoleLogs = emptyList(),
                    statusBannerMessage = null
                )
            }
            pushLog("Initializing APK Forge build pipeline...")
            pushLog("Target Prompt: \"${prompt.take(80)}${if (prompt.length > 80) "..." else ""}\"")
            delay(250)

            _uiState.update {
                it.copy(
                    buildProgress = 0.38f,
                    currentBuildStage = "2/4 • Synthesizing Mobile App Runtime & Kotlin Compose..."
                )
            }

            val spec = GeminiApkGeneratorService.generateApkFromPrompt(
                userPrompt = prompt,
                customAppName = currentState.customAppName.takeIf { it.isNotBlank() },
                customPackageName = currentState.customPackageName.takeIf { it.isNotBlank() },
                preferredAccentHex = currentState.selectedAccentHex,
                onProgressLog = { pushLog(it) }
            )

            _uiState.update {
                it.copy(
                    buildProgress = 0.72f,
                    currentBuildStage = "3/4 • Compiling DEX Stub, Manifest & Signing .APK..."
                )
            }
            pushLog("Packaging AndroidManifest.xml (${spec.packageName})...")
            pushLog("Computing SHA-256 digests & signing META-INF/APKFORGE.SF...")
            delay(300)

            val versionName = currentState.versionName.ifBlank { "1.0.0" }
            val nextVersionCode = repository.getBuildCount() + 1

            val packaged = ApkPackagerEngine.packageApkAndProject(
                context = appContext,
                appName = spec.appName,
                packageName = spec.packageName,
                versionName = versionName,
                versionCode = nextVersionCode,
                minSdk = 24,
                targetSdk = 36,
                permissions = spec.permissions,
                interactiveHtmlCode = spec.interactiveHtml,
                kotlinMainActivityCode = spec.kotlinComposeCode
            )

            pushLog("APK built: ${packaged.apkFile.name} (${ApkPackagerEngine.formatBytes(packaged.apkSizeBytes)})")
            pushLog("SHA-256: ${packaged.sha256Hex.take(32)}...")

            val newEntity = ApkBuildEntity(
                appName = spec.appName,
                packageName = spec.packageName,
                versionName = versionName,
                versionCode = nextVersionCode,
                category = spec.category,
                prompt = prompt,
                summary = spec.summary,
                accentColorHex = spec.accentColorHex,
                permissionsCsv = spec.permissions.joinToString(","),
                featuresCsv = spec.features.joinToString("|"),
                interactiveHtmlCode = spec.interactiveHtml,
                kotlinMainActivityCode = spec.kotlinComposeCode,
                androidManifestXml = packaged.androidManifestXml,
                buildGradleKts = packaged.buildGradleKts,
                apkFilePath = packaged.apkFile.absolutePath,
                projectZipFilePath = packaged.projectZipFile.absolutePath,
                apkFileSizeBytes = packaged.apkSizeBytes,
                sha256Signature = packaged.sha256Hex,
                aiModelUsed = spec.modelUsed
            )

            val newId = repository.insertBuild(newEntity)

            _uiState.update {
                it.copy(
                    isGenerating = false,
                    buildProgress = 1.0f,
                    currentBuildStage = "4/4 • APK Forged & Ready for Download!",
                    activeBuildId = newId,
                    statusBannerMessage = "Forged ${spec.appName} (${packaged.apkFile.name})! Ready to run or download.",
                    isStatusSuccess = true
                )
            }
            onCompleteNavigateToPreview()
        }
    }

    fun downloadApkToDevice(build: ApkBuildEntity) {
        viewModelScope.launch {
            val file = ensureApkFileExists(build)
            val cleanSlug = build.appName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            val fileName = "${cleanSlug}-v${build.versionName}.apk"
            val result = ApkPackagerEngine.saveFileToDeviceDownloads(
                context = appContext,
                sourceFile = file,
                desiredFileName = fileName,
                mimeType = "application/vnd.android.package-archive"
            )
            _uiState.update {
                it.copy(
                    statusBannerMessage = result.message,
                    isStatusSuccess = result.success
                )
            }
        }
    }

    fun downloadProjectZipToDevice(build: ApkBuildEntity) {
        viewModelScope.launch {
            val file = ensureProjectZipExists(build)
            val cleanSlug = build.appName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            val fileName = "${cleanSlug}-v${build.versionName}-android-studio-project.zip"
            val result = ApkPackagerEngine.saveFileToDeviceDownloads(
                context = appContext,
                sourceFile = file,
                desiredFileName = fileName,
                mimeType = "application/zip"
            )
            _uiState.update {
                it.copy(
                    statusBannerMessage = result.message,
                    isStatusSuccess = result.success
                )
            }
        }
    }

    fun exportFileToUserUri(build: ApkBuildEntity, isApk: Boolean, uri: Uri) {
        viewModelScope.launch {
            val source = if (isApk) ensureApkFileExists(build) else ensureProjectZipExists(build)
            val result = ApkPackagerEngine.copyFileToUserUri(appContext, source, uri)
            _uiState.update {
                it.copy(
                    statusBannerMessage = result.message,
                    isStatusSuccess = result.success
                )
            }
        }
    }

    fun shareApkOrZip(build: ApkBuildEntity, isApk: Boolean) {
        viewModelScope.launch {
            val file = if (isApk) ensureApkFileExists(build) else ensureProjectZipExists(build)
            val mime = if (isApk) "application/vnd.android.package-archive" else "application/zip"
            ApkPackagerEngine.sharePackageFile(
                context = appContext,
                file = file,
                mimeType = mime,
                title = "Export ${build.appName} (${file.name})"
            )
        }
    }

    fun deleteBuild(id: Long) {
        viewModelScope.launch {
            repository.deleteBuildById(id)
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Removed APK build from vault.",
                    isStatusSuccess = true
                )
            }
        }
    }

    private suspend fun ensureApkFileExists(build: ApkBuildEntity): File {
        val existing = File(build.apkFilePath)
        if (existing.exists()) return existing
        val regenerated = ApkPackagerEngine.packageApkAndProject(
            context = appContext,
            appName = build.appName,
            packageName = build.packageName,
            versionName = build.versionName,
            versionCode = build.versionCode,
            minSdk = build.minSdk,
            targetSdk = build.targetSdk,
            permissions = build.permissionsList,
            interactiveHtmlCode = build.interactiveHtmlCode,
            kotlinMainActivityCode = build.kotlinMainActivityCode
        )
        return regenerated.apkFile
    }

    private suspend fun ensureProjectZipExists(build: ApkBuildEntity): File {
        val existing = File(build.projectZipFilePath)
        if (existing.exists()) return existing
        val regenerated = ApkPackagerEngine.packageApkAndProject(
            context = appContext,
            appName = build.appName,
            packageName = build.packageName,
            versionName = build.versionName,
            versionCode = build.versionCode,
            minSdk = build.minSdk,
            targetSdk = build.targetSdk,
            permissions = build.permissionsList,
            interactiveHtmlCode = build.interactiveHtmlCode,
            kotlinMainActivityCode = build.kotlinMainActivityCode
        )
        return regenerated.projectZipFile
    }

    class Factory(
        private val context: Context,
        private val repository: ApkRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApkForgeViewModel(context.applicationContext, repository) as T
        }
    }
}
