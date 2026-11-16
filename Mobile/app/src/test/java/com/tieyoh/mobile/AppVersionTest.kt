package com.tieyoh.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * REQ-002. These tests deliberately avoid typing the current version, so
 * build.gradle.kts stays the only place it is written (AC-3).
 */
class AppVersionTest {

    @Test
    fun formatLabel_isWordVersionSpaceNumber() {
        assertEquals("Version 1.2", AppVersion.formatLabel("1.2"))
        assertEquals("Version 2.0", AppVersion.formatLabel("2.0"))
        assertEquals("Version 1.10", AppVersion.formatLabel("1.10"))
    }

    @Test
    fun displayLabel_comesFromBuildConfig() {
        assertEquals("Version " + BuildConfig.VERSION_NAME, AppVersion.displayLabel)
    }

    @Test
    fun versionName_isMajorDotMinor_withNoSuffix() {
        assertTrue(
            "versionName '${BuildConfig.VERSION_NAME}' must be MAJOR.MINOR",
            Regex("""^\d+\.\d+$""").matches(BuildConfig.VERSION_NAME)
        )
    }

    @Test
    fun versionCode_isAboveTheOriginalTemplateValue() {
        assertTrue(BuildConfig.VERSION_CODE > 1)
    }
}
