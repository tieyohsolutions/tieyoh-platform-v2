package com.tieyoh.mobile

/**
 * The app version as shown to users (REQ-002).
 *
 * The version itself lives in exactly one place: `versionName` /
 * `versionCode` in app/build.gradle.kts. This object only reads the
 * generated [BuildConfig] value, so nothing here needs editing on a bump
 * and the label never depends on the network.
 */
object AppVersion {

    /** e.g. "Version 1.1" -- shown at the bottom of the Profile screen. */
    val displayLabel: String
        get() = formatLabel(BuildConfig.VERSION_NAME)

    /** "Version" + one space + the MAJOR.MINOR value (Decision D-1). */
    fun formatLabel(versionName: String): String = "Version $versionName"
}
