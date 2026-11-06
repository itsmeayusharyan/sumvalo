package com.acuranza.app.ui.about

import android.content.ActivityNotFoundException
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acuranza.app.BuildConfig
import com.acuranza.app.designsystem.components.SumvaloLogo
import com.acuranza.app.designsystem.components.LogoShape
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.nav.Routes

internal fun openUrl(context: android.content.Context, url: String): Boolean = try {
    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: Exception) {
    false
}

/** Device and build facts shown on the About screen; injectable so tests render deterministically. */
data class AboutDeviceInfo(
    val androidRelease: String,
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val appVersion: String,
) {
    companion object {
        fun fromSystem(versionName: String = BuildConfig.VERSION_NAME) =
            AboutDeviceInfo(
                androidRelease = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                model = Build.MODEL,
                appVersion = versionName,
            )
    }
}

/** The About Sumvalo screen: branding, version and release info, device info, legal pages. Reached from Settings. */
@Composable
fun AboutScreen(nav: androidx.navigation.NavController) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    AboutContent(
        onBack = { nav.popBackStack() },
        onPrivacy = { nav.navigate(Routes.ABOUT_PRIVACY) },
        onTerms = { nav.navigate(Routes.ABOUT_TERMS) },
        onLicence = { nav.navigate(Routes.ABOUT_GPL) },
        snackbar = snackbar,
    )
}

/**
 * Stateless body of the About screen, so tests can render it without BuildConfig: branding with
 * version pill and tagline, device/build facts as label-value rows, and the legal pages as
 * icon-tile rows — the reference's card treatment.
 */
@Composable
fun AboutContent(
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit = {},
    onLicence: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    device: AboutDeviceInfo = AboutDeviceInfo.fromSystem(),
) {
    val c = SumvaloTheme.colors
    // "0.27-full" → "v0.27 · full"; a plain "0.27" → "v0.27".
    val versionLabel = device.appVersion
        .split("-", limit = 2)
        .joinToString(" · ") { it }
        .let { "v$it" }

    SumvaloScreen(
        title = "About",
        subtitle = null,
        onBack = onBack,
        snackbar = snackbar,
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            // Brand block: glowing logo tile, name, tagline, version pill.
            Column(
                Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .padding(vertical = 26.dp, horizontal = 34.dp)
                        .drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                                    center = center,
                                    radius = size.minDimension * 0.72f,
                                ),
                            )
                        },
                ) {
                    SumvaloLogo(size = 96.dp, shape = LogoShape.RoundedSquare)
                }
                Spacer(Modifier.height(14.dp))
                Text("Sumvalo", style = MaterialTheme.typography.titleLarge, color = c.ink)
                Spacer(Modifier.height(2.dp))
                Text("Your money, on your phone", style = MaterialTheme.typography.bodyMedium, color = c.subtleText)
                Spacer(Modifier.height(10.dp))
                Text(
                    versionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.subtleText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.fill)
                        .border(1.dp, c.hairline, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }

            AboutSectionLabel("Device and build")
            Spacer(Modifier.height(8.dp))
            SumvaloCard(contentPadding = PaddingValues(0.dp)) {
                AboutInfoRow("Android version", "Android ${device.androidRelease} (API ${device.apiLevel})")
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                AboutInfoRow("Device", "${device.manufacturer} ${device.model}")
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                AboutInfoRow("App version", device.appVersion)
            }

            Spacer(Modifier.height(20.dp))
            AboutSectionLabel("Legal")
            Spacer(Modifier.height(8.dp))
            SumvaloCard(contentPadding = PaddingValues(0.dp)) {
                AboutActionRow(Icons.Outlined.Shield, "Privacy policy", "What Sumvalo stores, and what it never sends", onClick = onPrivacy)
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                AboutActionRow(Icons.Outlined.Link, "Terms of service", "The plain terms of using the app", onClick = onTerms)
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                AboutActionRow(Icons.Outlined.Info, "Licence", "GPL-3.0 · free and open source", onClick = onLicence)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Sumvalo · Your data stays on this device",
                style = MaterialTheme.typography.bodySmall,
                color = c.tertiaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AboutSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = SumvaloText.sectionLabel,
        color = SumvaloTheme.colors.tertiaryText,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/** A build fact: label left, value right, 52dp row. */
@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = SumvaloTheme.colors.ink,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = SumvaloTheme.colors.subtleText,
            textAlign = TextAlign.Right,
        )
    }
}

/** A legal destination: 32dp icon tile, title + subtitle, chevron, 60dp row. */
@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = SumvaloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.subtleText, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.tertiaryText,
            modifier = Modifier.size(16.dp),
        )
    }
}
