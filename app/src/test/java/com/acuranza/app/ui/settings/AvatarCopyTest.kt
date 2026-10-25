package com.acuranza.app.ui.settings

import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure decisions behind the photo pipeline: failure wording and EXIF orientation mapping. */
class AvatarCopyTest {

    @Test fun failureMessagesAreSpecific() {
        assertTrue(AvatarCopyResult.message(AvatarCopyFailure.ACCESS_DENIED).contains("access"))
        assertTrue(AvatarCopyResult.message(AvatarCopyFailure.NOT_FOUND).contains("no longer available"))
        assertTrue(AvatarCopyResult.message(AvatarCopyFailure.UNREADABLE).contains("corrupted or in a format"))
        assertTrue(AvatarCopyResult.message(AvatarCopyFailure.TOO_LARGE).contains("too large"))
        assertTrue(AvatarCopyResult.message(AvatarCopyFailure.SAVE_FAILED).contains("free space"))
    }

    @Test fun exifRotations() {
        assertEquals(90 to false, exifRotation(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180 to false, exifRotation(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270 to false, exifRotation(ExifInterface.ORIENTATION_ROTATE_270))
        assertEquals(0 to false, exifRotation(ExifInterface.ORIENTATION_NORMAL))
        assertEquals(0 to false, exifRotation(99)) // unknown values fall back to no transform
    }

    @Test fun exifMirrors() {
        assertEquals(0 to true, exifRotation(ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
        assertEquals(180 to true, exifRotation(ExifInterface.ORIENTATION_FLIP_VERTICAL))
        assertEquals(90 to true, exifRotation(ExifInterface.ORIENTATION_TRANSPOSE))
        assertEquals(270 to true, exifRotation(ExifInterface.ORIENTATION_TRANSVERSE))
        assertFalse(exifRotation(ExifInterface.ORIENTATION_NORMAL).second)
    }
}
