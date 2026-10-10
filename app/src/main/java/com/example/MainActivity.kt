package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ApkBuildEntity
import com.example.data.ApkRepository
import com.example.data.AppDatabase
import com.example.ui.ApkForgeViewModel
import com.example.ui.components.InteractiveApkRuntimeView
import com.example.ui.screens.ApkVaultTab
import com.example.ui.screens.ForgeStudioTab
import com.example.ui.screens.LivePreviewAndCodeTab
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = remember {
                ApkRepository(AppDatabase.getInstance(context).apkBuildDao())
            }
            val forgeViewModel: ApkForgeViewModel = viewModel(
                factory = ApkForgeViewModel.Factory(context, repository)
            )
            val uiState by forgeViewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
                ApkForgeApp(forgeViewModel = forgeViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkForgeApp(forgeViewModel: ApkForgeViewModel) {
    val uiState by forgeViewModel.uiState.collectAsStateWithLifecycle()
    val builds by forgeViewModel.allBuilds.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) } // 0 = Forge Studio, 1 = Live App & Code, 2 = APK Vault
    var pendingSaveBuild by remember { mutableStateOf<ApkBuildEntity?>(null) }

    val saveApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        val targetBuild = pendingSaveBuild
        if (uri != null && targetBuild != null) {
            forgeViewModel.exportFileToUserUri(targetBuild, isApk = true, uri = uri)
        }
        pendingSaveBuild = null
    }

    val activeBuild = remember(builds, uiState.activeBuildId) {
        builds.find { it.id == uiState.activeBuildId } ?: builds.firstOrNull()
    }

    // Full-Screen Launched App Overlay
    val fullScreenBuild = uiState.fullScreenLaunchBuild
    if (fullScreenBuild != null) {
        BackHandler {
            forgeViewModel.closeFullScreenApp()
        }
        FullScreenAppRunner(
            build = fullScreenBuild,
            onClose = { forgeViewModel.closeFullScreenApp() },
            onDownloadApk = { forgeViewModel.downloadApkToDevice(fullScreenBuild) }
        )
        return
    }

    // BackHandler for secondary tabs to return to Home tab (Forge Studio)
    if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Android,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Prompt → APK Compiler & Runner",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "${builds.size} APKs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    IconButton(
                        onClick = { forgeViewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle studio theme"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = stringResource(R.string.tab_forge)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_forge)) },
                    modifier = Modifier.testTag("nav_tab_forge")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.PhoneAndroid else Icons.Outlined.PhoneAndroid,
                            contentDescription = stringResource(R.string.tab_preview)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_preview)) },
                    modifier = Modifier.testTag("nav_tab_preview")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = stringResource(R.string.tab_vault)
                        )
                    },
                    label = { Text("${stringResource(R.string.tab_vault)} (${builds.size})") },
                    modifier = Modifier.testTag("nav_tab_vault")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Banner for Download & Build Confirmation
            AnimatedVisibility(
                visible = uiState.statusBannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val msg = uiState.statusBannerMessage ?: ""
                Surface(
                    color = if (uiState.isStatusSuccess) {
                        Color(0xFF064E3B)
                    } else {
                        Color(0xFF4C0519)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(
                            1.dp,
                            if (uiState.isStatusSuccess) Color(0xFF10B981) else Color(0xFFF43F5E),
                            RoundedCornerShape(14.dp)
                        )
                        .testTag("status_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (uiState.isStatusSuccess) Color(0xFF34D399) else Color(0xFFFB7185),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = { forgeViewModel.dismissStatusBanner() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss notification",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    0 -> ForgeStudioTab(
                        uiState = uiState,
                        presets = forgeViewModel.presets,
                        latestBuild = activeBuild,
                        onPromptChange = forgeViewModel::updatePrompt,
                        onCustomAppNameChange = forgeViewModel::updateCustomAppName,
                        onCustomPackageChange = forgeViewModel::updateCustomPackageName,
                        onVersionChange = forgeViewModel::updateVersionName,
                        onAccentSelect = forgeViewModel::selectAccentColor,
                        onToggleAdvanced = forgeViewModel::toggleAdvancedConfig,
                        onSelectPreset = forgeViewModel::applyPreset,
                        onGenerateClick = {
                            forgeViewModel.generateApk(
                                onCompleteNavigateToPreview = {
                                    currentTab = 1
                                }
                            )
                        },
                        onDownloadApkClick = forgeViewModel::downloadApkToDevice,
                        onSaveApkAsClick = { build ->
                            pendingSaveBuild = build
                            val cleanSlug = build.appName.lowercase()
                                .replace(Regex("[^a-z0-9]+"), "-")
                                .trim('-')
                            saveApkLauncher.launch("${cleanSlug}-v${build.versionName}.apk")
                        },
                        onLaunchFullScreen = forgeViewModel::openFullScreenApp,
                        onInspectBuildClick = { build ->
                            forgeViewModel.selectActiveBuild(build.id)
                            currentTab = 1
                        },
                        onShareApkClick = { build ->
                            forgeViewModel.shareApkOrZip(build, isApk = true)
                        }
                    )

                    1 -> LivePreviewAndCodeTab(
                        builds = builds,
                        activeBuild = activeBuild,
                        onSelectBuild = forgeViewModel::selectActiveBuild,
                        onDownloadApk = forgeViewModel::downloadApkToDevice,
                        onSaveApkAs = { build ->
                            pendingSaveBuild = build
                            val cleanSlug = build.appName.lowercase()
                                .replace(Regex("[^a-z0-9]+"), "-")
                                .trim('-')
                            saveApkLauncher.launch("${cleanSlug}-v${build.versionName}.apk")
                        },
                        onDownloadProjectZip = forgeViewModel::downloadProjectZipToDevice,
                        onShareApk = { build ->
                            forgeViewModel.shareApkOrZip(build, isApk = true)
                        },
                        onLaunchFullScreen = forgeViewModel::openFullScreenApp
                    )

                    2 -> ApkVaultTab(
                        builds = builds,
                        onDownloadApk = forgeViewModel::downloadApkToDevice,
                        onDownloadZip = forgeViewModel::downloadProjectZipToDevice,
                        onLaunchFullScreen = forgeViewModel::openFullScreenApp,
                        onInspectBuild = { build ->
                            forgeViewModel.selectActiveBuild(build.id)
                            currentTab = 1
                        },
                        onShareApk = { build ->
                            forgeViewModel.shareApkOrZip(build, isApk = true)
                        },
                        onDeleteBuild = forgeViewModel::deleteBuild
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenAppRunner(
    build: ApkBuildEntity,
    onClose: () -> Unit,
    onDownloadApk: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = Color(0xFF070B14),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("close_fullscreen_app_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Studio",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = build.appName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${build.packageName} • Running Live",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onDownloadApk,
                        modifier = Modifier.testTag("fullscreen_download_apk_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download .APK")
                    }
                }
            }
        }
    ) { innerPadding ->
        InteractiveApkRuntimeView(
            htmlContent = build.interactiveHtmlCode,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
