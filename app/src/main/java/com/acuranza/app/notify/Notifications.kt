package com.acuranza.app.notify

import com.acuranza.app.designsystem.theme.BrandPalette

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.acuranza.app.MainActivity
import com.acuranza.app.R

/** Notification channels and helpers. Taps open [MainActivity] with [EXTRA_ROUTE] set. */
class Notifications(private val context: Context) {

    fun createChannels() {
        // NotificationChannel is API-26+; minSdk is 26, so no guard is needed.
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_BILLS, "Card bills", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Credit card bills and due-date reminders" },
                NotificationChannel(CH_INSIGHTS, "Insights", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Budget and goal alerts" },
                NotificationChannel(CH_DETECTED, "Detected transactions", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_REMINDERS, "Transaction reminders", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Bank transactions Acuranza detected from notifications, waiting for your review" },
            ),
        )
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun bill(key: Int, title: String, text: String) = post(CH_BILLS, NOTIF_BILL_BASE + key, title, text, ROUTE_BILLS)

    fun insight(key: Int, title: String, text: String) = post(CH_INSIGHTS, NOTIF_INSIGHT_BASE + key, title, text, ROUTE_INSIGHTS)

    fun reminder(key: Int, title: String, text: String) = post(CH_INSIGHTS, NOTIF_REMINDER_BASE + key, title, text, ROUTE_RECURRING)

    /** The daily transaction-reminder nudge; tapping opens the expense form. */
    fun txnReminder(title: String, text: String) = post(CH_REMINDERS, NOTIF_TXN_REMINDER, title, text, ROUTE_ADD_EXPENSE)

    /**
     * A bank transaction Acuranza detected from an incoming notification, parked for review.
     * Called ONLY from the notification-listener path — manual entries, CSV imports and SMS
     * scans never post this. [key] is the pending-import id: stable, so a reposted alert
     * replaces itself instead of stacking.
     */
    fun detectedTransaction(key: Int, title: String, text: String) =
        post(CH_DETECTED, NOTIF_DETECTED_BASE + key, title, text, ROUTE_SMS_TRACKING)

    private fun post(channel: String, id: Int, title: String, text: String, route: String) {
        if (!canNotify()) return
        val intent = Intent(context, MainActivity::class.java)
            // SINGLE_TOP delivers the tap to the running activity (onNewIntent) instead of recreating it.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_ROUTE, route)
        val pending = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // On the lock screen only a generic line shows; amounts and merchants stay hidden until unlocked.
        val publicVersion = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Sumvalo")
            .setContentText(PUBLIC_TEXT[channel] ?: "New activity")
            .setColor(BrandPalette.Blue.toArgb())
            .build()
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setColor(BrandPalette.Blue.toArgb())
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    companion object {
        const val CH_BILLS = "bills"
        const val CH_DETECTED = "detected_transactions"
        const val CH_REMINDERS = "transaction_reminders"
        const val NOTIF_TXN_REMINDER = 7_900
        const val ROUTE_ADD_EXPENSE = "add-expense"
        const val CH_INSIGHTS = "insights"

        /** Intent extra naming the screen to open: one of the ROUTE_ constants. */
        const val EXTRA_ROUTE = "acuranza.route"
        const val ROUTE_REVIEW = "review"
        const val ROUTE_BILLS = "bills"
        const val ROUTE_SMS_TRACKING = "sms-tracking"
        const val ROUTE_INSIGHTS = "insights"
        const val ROUTE_RECURRING = "recurring"

        /** Lock-screen text per channel, with no amounts or merchants. */
        private val PUBLIC_TEXT = mapOf(
            CH_BILLS to "Card bill reminder",
            CH_INSIGHTS to "New insight",
        )

        private const val NOTIF_BILL_BASE = 300
        const val NOTIF_DETECTED_BASE = 6100_000
        private const val NOTIF_INSIGHT_BASE = 400_000
        private const val NOTIF_MATURITY_BASE = 500_000
        private const val NOTIF_REMINDER_BASE = 700_000
    }
}
