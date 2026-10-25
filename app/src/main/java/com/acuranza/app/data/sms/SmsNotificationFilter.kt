package com.acuranza.app.data.sms

/**
 * Which notifications the automatic SMS capture may even look at, and what text it may read.
 * Pure logic, JVM-tested; the listener service is a thin Android wrapper around it.
 *
 * Privacy rules, in order:
 *  1. Only the phone's SMS/MMS apps' notifications are considered — nothing else (no chat apps,
 *     no banking-app notifications, no mail). Other notifications are never touched.
 *  2. The expanded notification body is read; when a ROM posts only the collapsed line, that is
 *     used instead.
 *  3. From there the same SharedSmsPipeline applies: authentication messages, promotions,
 *     personal conversations and ambiguous text are rejected before anything is stored, and
 *     the message text itself is never kept — only the extracted transaction fields, parked
 *     for review exactly like a shared message.
 */
object SmsNotificationFilter {

    /** The default SMS apps Sumvalo watches: stock AOSP, Google Messages, Samsung, OnePlus/Oppo, Xiaomi, Huawei. */
    val SMS_PACKAGES: Set<String> = setOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.android.sms",
        "com.samsung.android.messaging",
        "com.oneplus.mms",
        "com.oppo.mms",
        "com.xiaomi.mms",
        "com.huawei.android.MmsApp",
    )

    /** True when [pkg] is an SMS app whose notifications may be examined at all. */
    fun isSmsPackage(pkg: String): Boolean = pkg in SMS_PACKAGES

    /** The notification text to parse, or null when there is nothing readable. */
    fun bodyOf(bigText: CharSequence?, text: CharSequence?): String? {
        val body = (bigText ?: text)?.toString()?.trim()
        return body?.takeIf { it.isNotEmpty() }
    }
}
