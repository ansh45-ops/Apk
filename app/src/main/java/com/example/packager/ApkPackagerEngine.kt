package com.example.packager

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.Adler32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class PackagedArtifacts(
    val apkFile: File,
    val projectZipFile: File,
    val apkSizeBytes: Long,
    val sha256Hex: String,
    val androidManifestXml: String,
    val buildGradleKts: String
)

data class DownloadResult(
    val success: Boolean,
    val displayLocation: String,
    val message: String,
    val savedUri: Uri? = null
)

object ApkPackagerEngine {

    suspend fun packageApkAndProject(
        context: Context,
        appName: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        minSdk: Int,
        targetSdk: Int,
        permissions: List<String>,
        interactiveHtmlCode: String,
        kotlinMainActivityCode: String
    ): PackagedArtifacts = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "apks").apply {
            if (!exists()) mkdirs()
        }

        val slug = appName.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "generated-app" }
        val timestampSuffix = (System.currentTimeMillis() % 100000).toString()

        val apkFile = File(outputDir, "${slug}-v${versionName}-${timestampSuffix}.apk")
        val projectZipFile = File(outputDir, "${slug}-v${versionName}-android-studio.zip")

        val manifestXml = generateAndroidManifestXml(
            appName = appName,
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            permissions = permissions
        )

        val buildGradleKts = generateBuildGradleKts(
            packageName = packageName,
            minSdk = minSdk,
            targetSdk = targetSdk,
            versionCode = versionCode,
            versionName = versionName
        )

        val githubWorkflowYml = generateGithubActionsYml(appName, slug)

        val stringsXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name">${escapeXml(appName)}</string>
            </resources>
        """.trimIndent()

        val dexBytes = buildMinimalDexStub(packageName, appName)
        val arscBytes = buildMinimalResourcesArsc(packageName, appName)

        // Collect all APK entries to compute real SHA-256 digests for META-INF/MANIFEST.MF
        val apkEntries = linkedMapOf<String, ByteArray>()
        apkEntries["AndroidManifest.xml"] = manifestXml.toByteArray(Charsets.UTF_8)
        apkEntries["classes.dex"] = dexBytes
        apkEntries["resources.arsc"] = arscBytes
        apkEntries["assets/www/index.html"] = interactiveHtmlCode.toByteArray(Charsets.UTF_8)
        apkEntries["res/values/strings.xml"] = stringsXml.toByteArray(Charsets.UTF_8)
        val packagePath = packageName.replace('.', '/')
        apkEntries["src/main/java/$packagePath/MainActivity.kt"] =
            kotlinMainActivityCode.toByteArray(Charsets.UTF_8)

        val manifestMf = buildJarManifest(apkEntries, appName, packageName, versionName)
        val manifestMfBytes = manifestMf.toByteArray(Charsets.UTF_8)
        val signatureSfBytes = buildSignatureFile(manifestMfBytes, apkEntries)
        val certBlockBytes = buildCertificateStub(packageName)

        ZipOutputStream(FileOutputStream(apkFile)).use { zos ->
            for ((path, bytes) in apkEntries) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(bytes)
                zos.closeEntry()
            }
            zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            zos.write(manifestMfBytes)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("META-INF/APKFORGE.SF"))
            zos.write(signatureSfBytes)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("META-INF/APKFORGE.RSA"))
            zos.write(certBlockBytes)
            zos.closeEntry()
        }

        // Also build the complete Android Studio Gradle Project ZIP (with .github/workflows/build-apk.yml)
        ZipOutputStream(FileOutputStream(projectZipFile)).use { zos ->
            fun addTextFile(path: String, content: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            addTextFile(".github/workflows/build-apk.yml", githubWorkflowYml)

            addTextFile(
                "settings.gradle.kts",
                """
                pluginManagement {
                    repositories {
                        google()
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories {
                        google()
                        mavenCentral()
                    }
                }
                rootProject.name = "${appName.replace("\"", "")}"
                include(":app")
                """.trimIndent()
            )

            addTextFile(
                "build.gradle.kts",
                """
                plugins {
                    id("com.android.application") version "8.7.2" apply false
                    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
                    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
                }
                """.trimIndent()
            )

            addTextFile("app/build.gradle.kts", buildGradleKts)
            addTextFile("app/src/main/AndroidManifest.xml", manifestXml)
            addTextFile("app/src/main/res/values/strings.xml", stringsXml)
            addTextFile("app/src/main/assets/index.html", interactiveHtmlCode)
            addTextFile("app/src/main/java/$packagePath/MainActivity.kt", kotlinMainActivityCode)
            addTextFile(
                "README.md",
                """
                # $appName (`$packageName`)
                Generated by **APK Forge Studio**.
                
                ## Included Artifacts
                1. **GitHub Actions CI/CD (`.github/workflows/build-apk.yml`)**: Push to GitHub to automatically compile and download a signed `.apk` artifact.
                2. **Interactive Web/Hybrid Runtime**: `app/src/main/assets/index.html`
                3. **Native Jetpack Compose Source**: `app/src/main/java/$packagePath/MainActivity.kt`
                4. **AndroidManifest & Gradle Scripts**: Ready to open in Android Studio and run `gradle assembleDebug`.
                """.trimIndent()
            )
        }

        val sha256Hex = computeFileSha256Hex(apkFile)

        PackagedArtifacts(
            apkFile = apkFile,
            projectZipFile = projectZipFile,
            apkSizeBytes = apkFile.length(),
            sha256Hex = sha256Hex,
            androidManifestXml = manifestXml,
            buildGradleKts = buildGradleKts
        )
    }

    suspend fun saveFileToDeviceDownloads(
        context: Context,
        sourceFile: File,
        desiredFileName: String,
        mimeType: String
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                return@withContext DownloadResult(
                    success = false,
                    displayLocation = "",
                    message = "Package file not found on disk. Please regenerate the APK."
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, desiredFileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/APKForge"
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val collection =
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, contentValues)
                    ?: return@withContext DownloadResult(
                        success = false,
                        displayLocation = "",
                        message = "Could not create entry in system Downloads folder."
                    )

                resolver.openOutputStream(itemUri)?.use { outStream ->
                    FileInputStream(sourceFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)

                DownloadResult(
                    success = true,
                    displayLocation = "Downloads/APKForge/$desiredFileName",
                    message = "Downloaded to Downloads/APKForge/$desiredFileName (${formatBytes(sourceFile.length())})",
                    savedUri = itemUri
                )
            } else {
                val downloadsDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "APKForge"
                ).apply { mkdirs() }
                val targetFile = File(downloadsDir, desiredFileName)
                sourceFile.copyTo(targetFile, overwrite = true)

                DownloadResult(
                    success = true,
                    displayLocation = targetFile.absolutePath,
                    message = "Downloaded to ${targetFile.name} (${formatBytes(targetFile.length())})",
                    savedUri = Uri.fromFile(targetFile)
                )
            }
        } catch (e: Exception) {
            DownloadResult(
                success = false,
                displayLocation = "",
                message = "Download error: ${e.localizedMessage ?: "Failed to write file"}"
            )
        }
    }

    suspend fun copyFileToUserUri(
        context: Context,
        sourceFile: File,
        destinationUri: Uri
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            }
            DownloadResult(
                success = true,
                displayLocation = destinationUri.lastPathSegment ?: "Selected Location",
                message = "Saved ${sourceFile.name} (${formatBytes(sourceFile.length())})",
                savedUri = destinationUri
            )
        } catch (e: Exception) {
            DownloadResult(
                success = false,
                displayLocation = "",
                message = "Failed to save file: ${e.localizedMessage}"
            )
        }
    }

    fun sharePackageFile(context: Context, file: File, mimeType: String, title: String) {
        if (!file.exists()) return
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open share sheet: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(java.util.Locale.US, "%.2f MB", mb)
    }

    fun generateGithubActionsYml(appName: String, slug: String = "android-app"): String {
        return """
            name: Build $appName APK

            on:
              push:
                branches: [ "main", "master" ]
              workflow_dispatch:

            jobs:
              build-apk:
                runs-on: ubuntu-latest
                steps:
                  - name: Checkout Code
                    uses: actions/checkout@v4

                  - name: Set up JDK 17
                    uses: actions/setup-java@v4
                    with:
                      java-version: '17'
                      distribution: 'temurin'

                  - name: Setup Android SDK
                    uses: android-actions/setup-android@v3

                  - name: Setup Gradle
                    uses: gradle/actions/setup-gradle@v4
                    with:
                      gradle-version: '8.11.1'

                  - name: Build Debug APK
                    run: gradle assembleDebug --no-daemon

                  - name: Upload Downloadable APK
                    uses: actions/upload-artifact@v4
                    with:
                      name: ${slug}-debug-apk
                      path: app/build/outputs/apk/debug/*.apk
        """.trimIndent()
    }

    private fun generateAndroidManifestXml(
        appName: String,
        packageName: String,
        versionCode: Int,
        versionName: String,
        permissions: List<String>
    ): String {
        val permLines = permissions.joinToString("\n") { perm ->
            val fqPerm = if (perm.startsWith("android.permission.")) perm else "android.permission.$perm"
            """    <uses-permission android:name="$fqPerm" />"""
        }
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="$packageName"
                android:versionCode="$versionCode"
                android:versionName="$versionName">

            $permLines

                <application
                    android:allowBackup="true"
                    android:label="${escapeXml(appName)}"
                    android:supportsRtl="true"
                    android:theme="@android:style/Theme.DeviceDefault.NoActionBar">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true"
                        android:configChanges="orientation|screenSize|keyboardHidden">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent()
    }

    private fun generateBuildGradleKts(
        packageName: String,
        minSdk: Int,
        targetSdk: Int,
        versionCode: Int,
        versionName: String
    ): String {
        return """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
                id("org.jetbrains.kotlin.plugin.compose")
            }

            android {
                namespace = "$packageName"
                compileSdk = $targetSdk

                defaultConfig {
                    applicationId = "$packageName"
                    minSdk = $minSdk
                    targetSdk = $targetSdk
                    versionCode = $versionCode
                    versionName = "$versionName"
                }

                buildFeatures {
                    compose = true
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            dependencies {
                implementation(platform("androidx.compose:compose-bom:2024.09.00"))
                implementation("androidx.activity:activity-compose:1.9.3")
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
            }
        """.trimIndent()
    }

    private fun buildMinimalDexStub(packageName: String, appName: String): ByteArray {
        val classDescriptor = "L${packageName.replace('.', '/')}/MainActivity;"
        val payloadText = "APKFORGE_DEX_PAYLOAD:$classDescriptor:$appName:v1"
        val payloadBytes = payloadText.toByteArray(Charsets.UTF_8)
        val totalSize = 112 + payloadBytes.size
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        // DEX magic "dex\n039\0"
        buffer.put(byteArrayOf(0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x39, 0x00))
        // Placeholder for Adler32 checksum (4 bytes) + SHA-1 signature (20 bytes)
        buffer.position(32)
        buffer.putInt(totalSize) // file_size
        buffer.putInt(0x70)      // header_size (112 bytes)
        buffer.putInt(0x12345678) // endian_tag
        // Remaining header zeros + payload
        buffer.position(112)
        buffer.put(payloadBytes)

        val fullArray = buffer.array()
        // Compute SHA-1 over bytes 32..end
        val sha1 = MessageDigest.getInstance("SHA-1")
        sha1.update(fullArray, 32, fullArray.size - 32)
        val sha1Bytes = sha1.digest()
        System.arraycopy(sha1Bytes, 0, fullArray, 12, 20)

        // Compute Adler32 over bytes 12..end
        val adler = Adler32()
        adler.update(fullArray, 12, fullArray.size - 12)
        val checksumBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(adler.value.toInt()).array()
        System.arraycopy(checksumBytes, 0, fullArray, 8, 4)

        return fullArray
    }

    private fun buildMinimalResourcesArsc(packageName: String, appName: String): ByteArray {
        val info = "RES_TABLE:$packageName:$appName".toByteArray(Charsets.UTF_8)
        val totalLen = 12 + info.size
        val buffer = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(0x0002) // RES_TABLE_TYPE
        buffer.putShort(0x000C) // header size
        buffer.putInt(totalLen)
        buffer.putInt(1)        // package count
        buffer.put(info)
        return buffer.array()
    }

    private fun buildJarManifest(
        entries: Map<String, ByteArray>,
        appName: String,
        packageName: String,
        versionName: String
    ): String {
        val sb = StringBuilder()
        sb.append("Manifest-Version: 1.0\r\n")
        sb.append("Created-By: APK Forge Studio 1.0 (Android AI Packaging Engine)\r\n")
        sb.append("Package-Name: $packageName\r\n")
        sb.append("App-Label: $appName\r\n")
        sb.append("App-Version: $versionName\r\n\r\n")

        for ((name, bytes) in entries) {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            val b64 = Base64.encodeToString(digest, Base64.NO_WRAP)
            sb.append("Name: $name\r\n")
            sb.append("SHA-256-Digest: $b64\r\n\r\n")
        }
        return sb.toString()
    }

    private fun buildSignatureFile(
        manifestBytes: ByteArray,
        entries: Map<String, ByteArray>
    ): ByteArray {
        val manifestDigest = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(manifestBytes),
            Base64.NO_WRAP
        )
        val sb = StringBuilder()
        sb.append("Signature-Version: 1.0\r\n")
        sb.append("Created-By: APK Forge Signer\r\n")
        sb.append("SHA-256-Digest-Manifest: $manifestDigest\r\n\r\n")

        for ((name, bytes) in entries) {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            val b64 = Base64.encodeToString(digest, Base64.NO_WRAP)
            sb.append("Name: $name\r\n")
            sb.append("SHA-256-Digest: $b64\r\n\r\n")
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun buildCertificateStub(packageName: String): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x30, 0x82.toByte(), 0x01, 0x0A))
        out.write("APKFORGE-CERT-CN=$packageName,OU=MobileStudio,O=APKForge".toByteArray(Charsets.UTF_8))
        return out.toByteArray()
    }

    private fun computeFileSha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(8192)
            var read: Int
            while (fis.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02X".format(it) }
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
