package com.acuranza.app.ui

import com.acuranza.app.ui.settings.ProfileName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The profile display-name rules: trimming, whitespace collapse, emptiness, length cap. */
class ProfileNameTest {

    @Test fun trimsAndCollapsesWhitespace() {
        assertEquals("Ayush Aryan", ProfileName.clean("  Ayush   Aryan\t"))
        assertEquals("Ayush Aryan", ProfileName.clean("Ayush\nAryan"))
    }

    @Test fun emptyAfterTrimIsRejected() {
        assertNotNull(ProfileName.validate("   "))
        assertNotNull(ProfileName.validate(""))
        assertEquals("Enter a name", ProfileName.validate("   "))
    }

    @Test fun overTheCapIsRejected() {
        val long = "a".repeat(ProfileName.MAX + 1)
        assertEquals("Keep it under ${ProfileName.MAX} characters", ProfileName.validate(long))
    }

    @Test fun atTheCapIsAccepted() {
        assertNull(ProfileName.validate("a".repeat(ProfileName.MAX)))
    }

    @Test fun aNormalNamePasses() {
        assertNull(ProfileName.validate("  Ayush   Aryan  "))
        assertNull(ProfileName.validate("Ayush Aryan"))
    }
}
