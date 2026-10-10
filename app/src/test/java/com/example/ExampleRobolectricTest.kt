package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.packager.ApkPackagerEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and verify APK package generation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("APK Forge", appName)

        val artifacts = ApkPackagerEngine.packageApkAndProject(
            context = context,
            appName = "Quantum Calculator",
            packageName = "com.apkforge.calculator",
            versionName = "1.0.0",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 36,
            permissions = listOf("VIBRATE"),
            interactiveHtmlCode = "<html><body>Calculator</body></html>",
            kotlinMainActivityCode = "package com.apkforge.calculator\nclass MainActivity"
        )

        assertTrue("APK file should exist", artifacts.apkFile.exists())
        assertTrue("APK file name should end with .apk", artifacts.apkFile.name.endsWith(".apk"))
        assertTrue("APK size should be non-zero", artifacts.apkSizeBytes > 0)

        ZipFile(artifacts.apkFile).use { zip ->
            assertTrue(zip.getEntry("AndroidManifest.xml") != null)
            assertTrue(zip.getEntry("classes.dex") != null)
            assertTrue(zip.getEntry("resources.arsc") != null)
            assertTrue(zip.getEntry("META-INF/MANIFEST.MF") != null)
            assertTrue(zip.getEntry("assets/www/index.html") != null)
        }
    }
}
