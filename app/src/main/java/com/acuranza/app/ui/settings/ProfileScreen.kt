package com.acuranza.app.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.AcuranzaCard
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.SecondaryButton
import com.acuranza.app.designsystem.theme.AcuranzaText
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.text.style.TextOverflow

/** Display-name rules for the profile: trimmed, single-spaced, non-empty, at most [MAX] characters. */
object ProfileName {
    const val MAX = 40

    /** Collapses runs of whitespace and trims the ends — the canonical form a save persists. */
    fun clean(raw: String): String = raw.replace(Regex("\\s+"), " ").trim()

    /** null = acceptable; otherwise the message shown under the field. Newlines collapse to spaces in [clean]. */
    fun validate(raw: String): String? = when {
        clean(raw).isEmpty() -> "Enter a name"
        clean(raw).length > MAX -> "Keep it under $MAX characters"
        else -> null
    }
}

/**
 * Circular profile photo with graceful fallbacks: a saved file that no longer exists, a cancelled
 * pick or a decode failure all fall through to the name's initial (or a person glyph when the
 * profile is untouched). Decoding happens off the main thread.
 */
@Composable
fun ProfileAvatar(path: String?, name: String?, size: Dp, modifier: Modifier = Modifier) {
    val file = remember(path) { path?.let(::File)?.takeIf { it.exists() } }
    var bitmap by remember(file?.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file?.absolutePath) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { file?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() } }.getOrNull()
        }
    }
    val initial = name?.trim()?.take(1)?.uppercase()?.takeIf { it.isNotEmpty() }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(AcuranzaTheme.colors.accentTint),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else if (initial != null) {
            Text(initial, style = MaterialTheme.typography.titleMedium, color = AcuranzaTheme.colors.accent)
        } else {
            Icon(
                Icons.Outlined.Person,
                contentDescription = "No profile photo",
                tint = AcuranzaTheme.colors.accent,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

/** The Settings hub's tappable profile card: photo/initials, name (or the default prompt), edit affordance. */
@Composable
fun ProfileCard(name: String?, avatarPath: String?, onClick: () -> Unit) {
    AcuranzaCard(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(path = avatarPath, name = name, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name?.takeIf { it.isNotBlank() } ?: "Your profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = AcuranzaTheme.colors.ink,
                )
                Text(
                    if (name.isNullOrBlank()) "Add your name and photo" else "Your profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcuranzaTheme.colors.subtleText,
                )
            }
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Edit profile",
                tint = AcuranzaTheme.colors.tertiaryText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Profile editing: preview (photo + name) updates live, the system photo picker supplies a new
 * image, and only Save persists — Cancel discards both the staged name and any staged file.
 * The picker grants no persistent access, so a picked image is copied into app-private storage
 * immediately (which also validates it can be decoded at all).
 */
@Composable
fun ProfileScreen(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val haptic = rememberHaptic()

    var draftName by remember(s.profileName) { mutableStateOf(s.profileName ?: "") }
    var draftAvatar by remember(s.profileAvatar) { mutableStateOf(s.profileAvatar) }
    /** File copied for this edit but not yet saved; deleted if the user backs out. */
    var stagedFile by remember { mutableStateOf<String?>(null) }

    /** Deletes a pick this session that was never saved, so failed/abandoned picks don't leak files. */
    fun discardStaged(keeping: String?) {
        if (stagedFile != null && stagedFile != s.profileAvatar && stagedFile != keeping) {
            runCatching { File(stagedFile!!).delete() }
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) { copyAvatar(context, uri) }
                val copied = result.path
                if (copied != null) {
                    discardStaged(keeping = copied)
                    stagedFile = copied
                    draftAvatar = copied
                } else {
                    // The previous photo (saved or staged) is untouched on every failure path.
                    snackbar.showSnackbar(AvatarCopyResult.message(result.failure ?: AvatarCopyFailure.UNREADABLE))
                }
            }
        } // null = the user cancelled the picker; nothing changes
    }

    AcuranzaScreen(
        title = "Profile",
        subtitle = "How Acuranza greets you",
        onBack = {
            discardStaged(keeping = null)
            nav.popBackSafely()
        },
        snackbar = snackbar,
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            val error = ProfileName.validate(draftName)
            val nameChanged = ProfileName.clean(draftName) != (s.profileName ?: "")
            val avatarChanged = draftAvatar != s.profileAvatar
            ProfileContent(
                savedName = s.profileName,
                savedAvatar = s.profileAvatar,
                name = draftName,
                stagedAvatar = draftAvatar,
                nameError = if (draftName.isBlank()) null else error,
                onNameChange = { draftName = it },
                onPickImage = {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemoveImage = {
                    discardStaged(keeping = null)
                    stagedFile = null
                    draftAvatar = null
                },
                onSave = {
                    val cleaned = ProfileName.clean(draftName)
                    if (ProfileName.validate(cleaned) == null) {
                        scope.launch {
                            haptic.confirm()
                            c.settings.setProfileName(cleaned.takeIf { it.isNotEmpty() })
                            if (avatarChanged) {
                                c.settings.setProfileAvatar(draftAvatar)
                                // The replaced file is private to Acuranza and no longer referenced.
                                if (s.profileAvatar != null && s.profileAvatar != draftAvatar) {
                                    runCatching { File(s.profileAvatar!!).delete() }
                                }
                            }
                            stagedFile = null
                            android.widget.Toast.makeText(context, "Profile saved", android.widget.Toast.LENGTH_SHORT).show()
                            nav.popBackSafely()
                        }
                    }
                },
                onCancel = {
                    discardStaged(keeping = null)
                    nav.popBackSafely()
                },
                saveEnabled = (nameChanged || avatarChanged) && (error == null),
            )
        }
    }
}

/** Stateless profile-editor body (screenshot fixtures call this directly). */
@Composable
fun ProfileContent(
    savedName: String?,
    savedAvatar: String?,
    name: String,
    stagedAvatar: String?,
    nameError: String?,
    onNameChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    saveEnabled: Boolean,
) {
    AcuranzaCard(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Preview: the staged values, falling back to the saved ones.
            ProfileAvatar(path = stagedAvatar, name = name.takeIf { it.isNotBlank() } ?: savedName, size = 96.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                name.takeIf { it.isNotBlank() } ?: savedName ?: "Your profile",
                style = AcuranzaText.screenTitle,
                color = AcuranzaTheme.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Shown on your home greeting and in Settings",
                style = MaterialTheme.typography.bodySmall,
                color = AcuranzaTheme.colors.subtleText,
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    AcuranzaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            SettingsRow("Profile photo", "Choose a picture from your device", Icons.Outlined.Image) { onPickImage() }
            if (stagedAvatar != null || savedAvatar != null) {
                SettingsDivider()
                SettingsRow("Remove photo", "Restore the default initial avatar", Icons.Outlined.Person) { onRemoveImage() }
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Display name", style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = { Text("Your name", style = MaterialTheme.typography.bodyMedium) },
                    isError = nameError != null,
                    supportingText = if (nameError != null) {
                        { Text(nameError, color = AcuranzaTheme.colors.alert) }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = AcuranzaTheme.colors.cardSurface,
                        focusedContainerColor = AcuranzaTheme.colors.cardSurface,
                        unfocusedBorderColor = AcuranzaTheme.colors.hairline,
                        focusedBorderColor = AcuranzaTheme.colors.accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton("Cancel", onCancel, modifier = Modifier.weight(1f))
        PrimaryButton("Save", onSave, modifier = Modifier.weight(1f), enabled = saveEnabled)
    }
}

/** Why a picked image couldn't become the profile photo. */
enum class AvatarCopyFailure { ACCESS_DENIED, NOT_FOUND, UNREADABLE, TOO_LARGE, SAVE_FAILED }

/** A finished copy: either the app-private path to show, or a failure with a specific message. */
data class AvatarCopyResult(val path: String?, val failure: AvatarCopyFailure? = null) {
    companion object {
        /** User-facing wording per failure; JVM-tested so the messages can't drift from the mapping. */
        fun message(failure: AvatarCopyFailure): String = when (failure) {
            AvatarCopyFailure.ACCESS_DENIED -> "Sumvalo can't access that image — try picking it again"
            AvatarCopyFailure.NOT_FOUND -> "That image is no longer available — try picking it again"
            AvatarCopyFailure.UNREADABLE -> "That image is corrupted or in a format this device can't read"
            AvatarCopyFailure.TOO_LARGE -> "That image is too large for the profile photo"
            AvatarCopyFailure.SAVE_FAILED -> "Couldn't save the photo — check free space"
        }
    }
}

/** EXIF orientation → (rotation degrees, mirror). Pure so the mapping is unit-tested. */
internal fun exifRotation(orientation: Int): Pair<Int, Boolean> = when (orientation) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90 to false
    ExifInterface.ORIENTATION_ROTATE_180 -> 180 to false
    ExifInterface.ORIENTATION_ROTATE_270 -> 270 to false
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> 0 to true
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180 to true
    ExifInterface.ORIENTATION_TRANSPOSE -> 90 to true
    ExifInterface.ORIENTATION_TRANSVERSE -> 270 to true
    else -> 0 to false
}

/**
 * Copies a picked image into app-private storage as a 512px centre-cropped square, replacing any
 * previous avatar atomically (temp file + rename). Formats the platform decodes through
 * BitmapFactory work here — JPEG/JPG, PNG, WebP, GIF (first frame), BMP, and HEIF/HEIC on the
 * API levels whose decoders support them (29+; 26–27 cannot) — detected by content, never by
 * extension. EXIF orientation is applied, transparency is kept by saving PNG instead of JPEG,
 * and the previous photo survives every failure path.
 *
 * Root-cause note: an earlier version checked the STREAM by testing the decode result —
 * `openInputStream(uri)?.use { decodeStream(it, null, bounds) } ?: return null` — but
 * bounds-only decoding always returns null, so every selection aborted. The stream null-check
 * and the decode-result check are now separate.
 */
internal fun copyAvatar(context: Context, uri: Uri): AvatarCopyResult {
    return try {
    val resolver = context.contentResolver

    // Bounds pass: openInputStream's null means the URI is gone; decodeStream returning null
    // under inJustDecodeBounds is the API's documented behaviour, NOT an error.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(uri) ?: return AvatarCopyResult(null, AvatarCopyFailure.NOT_FOUND)
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return AvatarCopyResult(null, AvatarCopyFailure.UNREADABLE)

    // EXIF orientation read from the content stream (phone photos are usually stored rotated).
    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    // Downscale decode: never hold more than ~4x the 512px target in memory.
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 1024 && bounds.outHeight / (sample * 2) >= 1024) sample *= 2
    val src = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        ?: return AvatarCopyResult(null, AvatarCopyFailure.UNREADABLE)

    try {
        // Centre-crop a square, applying the EXIF transform in the same pass.
        val side = minOf(src.width, src.height)
        val (degrees, mirror) = exifRotation(orientation)
        val matrix = Matrix().apply {
            if (mirror) preScale(-1f, 1f)
            postRotate(degrees.toFloat())
        }
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side, if (matrix.isIdentity) null else matrix, true)

        // Keep transparency: PNG when the crop has alpha, JPEG otherwise.
        val asPng = square.hasAlpha()
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val out = File(dir, if (asPng) "profile.png" else "profile.jpg")
        val tmp = File(dir, out.name + ".tmp")
        val compressed = tmp.outputStream().use {
            square.compress(if (asPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, if (asPng) 100 else 90, it)
        }
        if (!compressed) {
            tmp.delete()
            return AvatarCopyResult(null, AvatarCopyFailure.SAVE_FAILED)
        }
        if (out.exists()) out.delete()
        if (!tmp.renameTo(out)) {
            tmp.copyTo(out, overwrite = true)
            tmp.delete()
        }
        AvatarCopyResult(out.absolutePath)
    } finally {
        src.recycle()
    }
} catch (e: SecurityException) {
    AvatarCopyResult(null, AvatarCopyFailure.ACCESS_DENIED)
} catch (e: java.io.FileNotFoundException) {
    AvatarCopyResult(null, AvatarCopyFailure.NOT_FOUND)
} catch (e: OutOfMemoryError) {
    AvatarCopyResult(null, AvatarCopyFailure.TOO_LARGE)
} catch (e: Exception) {
    AvatarCopyResult(null, AvatarCopyFailure.UNREADABLE)
    }
}
