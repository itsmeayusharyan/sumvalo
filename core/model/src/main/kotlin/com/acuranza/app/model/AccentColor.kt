package com.acuranza.app.model

/**
 * Accent colours a user can pick in Settings → Appearance. Blue is the brand default; the others
 * trade the brand hue while keeping Sumvalo's neutral slate surfaces and lightness scale.
 */
enum class AccentColor(val label: String) {
    BLUE("Blue"),
    TEAL("Teal"),
    VIOLET("Violet"),
    ROSE("Rose"),
}
