package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "apk_builds")
data class ApkBuildEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val category: String,
    val prompt: String,
    val summary: String,
    val accentColorHex: String,
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val permissionsCsv: String,
    val featuresCsv: String,
    val interactiveHtmlCode: String,
    val kotlinMainActivityCode: String,
    val androidManifestXml: String,
    val buildGradleKts: String,
    val apkFilePath: String,
    val projectZipFilePath: String,
    val apkFileSizeBytes: Long,
    val sha256Signature: String,
    val aiModelUsed: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val permissionsList: List<String>
        get() = permissionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val featuresList: List<String>
        get() = featuresCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() }
}
