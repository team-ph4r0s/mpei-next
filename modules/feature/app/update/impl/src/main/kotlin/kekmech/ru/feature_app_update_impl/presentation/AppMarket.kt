package kekmech.ru.feature_app_update_impl.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build

/** A place to which the page of the app in a market can be opened. [packageName] limits the intent to one application. */
internal data class MarketTarget(
    val uri: String,
    val packageName: String? = null,
)

/**
 * Where to open the page of the app, the best place first. The app is published in several markets under one version,
 * so no market is named here: the page is opened in the market that has installed the app (it is the one that will
 * deliver the update), then in any application that handles the standard `market://` link, and at last at the link
 * from the settings of the app, if there is one.
 */
internal fun marketTargets(appPackage: String, installer: String?, fallbackUrl: String): List<MarketTarget> {
    val marketUri = "market://details?id=$appPackage"
    return listOfNotNull(
        installer?.takeIf { it.isNotBlank() }?.let { MarketTarget(marketUri, packageName = it) },
        MarketTarget(marketUri),
        fallbackUrl.trim().takeIf { it.isNotEmpty() }?.let { MarketTarget(it) },
    )
}

/** Opens the page of the app in the market it was installed from; false if no application could show it. */
internal fun Context.openAppInMarket(fallbackUrl: String): Boolean =
    marketTargets(packageName, installerPackage(), fallbackUrl).any { target ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target.uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .apply { target.packageName?.let { setPackage(it) } }
        try {
            startActivity(intent)
            true
        } catch (@Suppress("SwallowedException") e: ActivityNotFoundException) {
            false
        }
    }

/** The package of the market (or the installer) that put the app on the device; null for a build installed from a file. */
private fun Context.installerPackage(): String? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        packageManager.getInstallSourceInfo(packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        packageManager.getInstallerPackageName(packageName)
    }
} catch (@Suppress("SwallowedException") e: PackageManager.NameNotFoundException) {
    null
}
