package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ApkBuildEntity
import com.example.packager.ApkPackagerEngine
import com.example.ui.components.InteractiveApkRuntimeView

@Composable
fun LivePreviewAndCodeTab(
    builds: List<ApkBuildEntity>,
    activeBuild: ApkBuildEntity?,
    onSelectBuild: (Long) -> Unit,
    onDownloadApk: (ApkBuildEntity) -> Unit,
    onSaveApkAs: (ApkBuildEntity) -> Unit,
    onDownloadProjectZip: (ApkBuildEntity) -> Unit,
    onShareApk: (ApkBuildEntity) -> Unit,
    onLaunchFullScreen: (ApkBuildEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Live Device, 1 = Kotlin Code, 2 = Manifest & APK Inspector

    if (activeBuild == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Generate an APK in Forge Studio to preview and download it here.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Build Switcher Pills if user has multiple generated APKs
        if (builds.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(builds, key = { it.id }) { item ->
                    val isSelected = item.id == activeBuild.id
                    Surface(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectBuild(item.id) }
                            .testTag("preview_select_build_${item.id}")
                    ) {
                        Text(
                            text = "${item.appName} v${item.versionName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Top APK Header & Instant Download Bar
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeBuild.appName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${activeBuild.packageName} • v${activeBuild.versionName} • ${ApkPackagerEngine.formatBytes(activeBuild.apkFileSizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { onLaunchFullScreen(activeBuild) },
                        modifier = Modifier.testTag("launch_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInFull,
                            contentDescription = "Launch Full Screen",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Full Screen")
                    }
                }

                // Action Row: Download .APK | Save .APK As | Project .ZIP | Share
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onDownloadApk(activeBuild) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("download_apk_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download .APK", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onSaveApkAs(activeBuild) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_apk_as_button")
                    ) {
                        Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save .APK As…")
                    }

                    OutlinedButton(
                        onClick = { onDownloadProjectZip(activeBuild) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("download_zip_button")
                    ) {
                        Icon(Icons.Filled.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Project .ZIP")
                    }

                    IconButton(
                        onClick = { onShareApk(activeBuild) },
                        modifier = Modifier.testTag("share_apk_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Package")
                    }
                }
            }
        }

        // Sub-Tab Switcher (Interactive Emulator | Kotlin Compose | APK Manifest & Digests)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                Triple(0, "Live Emulator", Icons.Filled.PhoneAndroid),
                Triple(1, "MainActivity.kt", Icons.Filled.Code),
                Triple(2, "APK & Manifest", Icons.Filled.Description)
            )
            tabs.forEach { (index, label, icon) ->
                val active = selectedSubTab == index
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedSubTab = index }
                        .testTag("subtab_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedSubTab) {
                0 -> {
                    // Realistic Android Phone Frame hosting the live generated app
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp))
                            .border(
                                width = 3.dp,
                                color = Color(0xFF28354E),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .background(Color(0xFF070B14))
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Simulated Phone Status Bar + Camera Punch Hole
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0B1120))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "9:41",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                )
                                Text(
                                    text = "5G • 100%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981)
                                )
                            }

                            InteractiveApkRuntimeView(
                                htmlContent = activeBuild.interactiveHtmlCode,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )

                            // Gesture Pill Footer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0B1120))
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF475569))
                                )
                            }
                        }
                    }
                }

                1 -> {
                    CodeViewerCard(
                        title = "src/main/java/${activeBuild.packageName.replace('.', '/')}/MainActivity.kt",
                        code = activeBuild.kotlinMainActivityCode,
                        onCopy = {
                            copyToClipboard(context, "MainActivity.kt", activeBuild.kotlinMainActivityCode)
                        }
                    )
                }

                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // APK Binary Signature & Structure Summary Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Signed APK Archive Contents",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = "SHA-256: ${activeBuild.sha256Signature}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Model: ${activeBuild.aiModelUsed} • Target SDK ${activeBuild.targetSdk} (Min SDK ${activeBuild.minSdk})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val archiveEntries = listOf(
                                    "AndroidManifest.xml (Package & Permissions)",
                                    "classes.dex (Dalvik Executable Header & Class Table)",
                                    "resources.arsc (Compiled Resource Table)",
                                    "assets/www/index.html (Interactive Hybrid App Bundle)",
                                    "src/main/java/.../MainActivity.kt (Jetpack Compose Source)",
                                    "META-INF/MANIFEST.MF & APKFORGE.SF (SHA-256 Digests)"
                                )
                                archiveEntries.forEach { entry ->
                                    Text(
                                        text = "├─ $entry",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        CodeViewerCard(
                            title = "AndroidManifest.xml",
                            code = activeBuild.androidManifestXml,
                            onCopy = {
                                copyToClipboard(context, "AndroidManifest.xml", activeBuild.androidManifestXml)
                            },
                            modifier = Modifier.height(260.dp)
                        )

                        CodeViewerCard(
                            title = "app/build.gradle.kts",
                            code = activeBuild.buildGradleKts,
                            onCopy = {
                                copyToClipboard(context, "build.gradle.kts", activeBuild.buildGradleKts)
                            },
                            modifier = Modifier.height(280.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeViewerCard(
    title: String,
    code: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Surface(
        color = Color(0xFF070B14),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF38BDF8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}
